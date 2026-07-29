import sys

filepath = "app/src/main/java/com/example/ui/ScratchEditorScreen.kt"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

idx_start = content.find("fun loadProjectIntoWebView(webView: WebView?, projectJson: String, base64Data: String) {")

new_fun = """fun loadProjectIntoWebView(webView: WebView?, projectJson: String, base64Data: String) {
    if (webView == null) return

    val jobId = System.currentTimeMillis()

    // 1. 初始化 JS 端接收容器，并设定新任务的唯一 ID（终止一切旧的死循环）
    webView.evaluateJavascript(\"\"\"
        window.__scratch_job_id = $jobId;
        window.__android_b64 = '';
        window.__android_json_b64 = '';
    \"\"\".trimIndent(), null)

    // 2. 核心黑科技：每次仅发送 150KB 切片，彻底绕开 Android WebView 的 1MB 崩溃红线
    val chunkSize = 150 * 1024 

    if (base64Data.isNotEmpty()) {
        val b64Chunks = base64Data.chunked(chunkSize)
        for (chunk in b64Chunks) {
            // Base64 只包含安全字符，无需任何转义，绝对不会报 JS 语法错误
            webView.evaluateJavascript("window.__android_b64 += \\"$chunk\\";", null)
        }
    }

    if (projectJson.isNotEmpty()) {
        // 把 JSON 也转成 Base64 再发送，彻底消灭所有 引号/换行符 引起的断链和解析异常
        val jsonBase64 = android.util.Base64.encodeToString(projectJson.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        val jsonChunks = jsonBase64.chunked(chunkSize)
        for (chunk in jsonChunks) {
            webView.evaluateJavascript("window.__android_json_b64 += \\"$chunk\\";", null)
        }
    }

    // 3. 开始执行核心注入
    val js = \"\"\"
        (function() {
            try {
                var currentJobId = window.__scratch_job_id;
                var base64Data = window.__android_b64;
                var jsonB64 = window.__android_json_b64;
                
                if ((!base64Data || base64Data.length === 0) && (!jsonB64 || jsonB64.length === 0)) return "Empty data";
                
                // 将安全运达的 JSON Base64 解码回原本的字符串
                var rawData = "";
                if (jsonB64 && jsonB64.length > 0) {
                    try {
                        rawData = decodeURIComponent(escape(window.atob(jsonB64)));
                    } catch(e) { console.error("JSON decode error", e); }
                }
                
                // 标准版 VM 仅认准 Uint8Array，进行严格二进制转换
                function base64ToUint8Array(b64) {
                    var binaryString = window.atob(b64);
                    var len = binaryString.length;
                    var bytes = new Uint8Array(len);
                    for (var i = 0; i < len; i++) {
                        bytes[i] = binaryString.charCodeAt(i);
                    }
                    return bytes;
                }

                var uint8Array = null;
                if (base64Data && base64Data.length > 0) {
                    try { uint8Array = base64ToUint8Array(base64Data); } catch(e) {}
                }

                var attempts = 0;
                var maxAttempts = 120; // 60秒最大轮询
                var readyCount = 0;

                function getVm() {
                    if (window.vm) return window.vm;
                    if (window.scratch && window.scratch.vm) return window.scratch.vm;
                    if (window.__turboWarp__ && window.__turboWarp__.vm) return window.__turboWarp__.vm;
                    var frames = document.querySelectorAll('iframe');
                    for (var i = 0; i < frames.length; i++) {
                        try { if (frames[i].contentWindow && frames[i].contentWindow.vm) return frames[i].contentWindow.vm; } catch(e) {}
                    }
                    
                    // 极限 React Fiber DOM 强扒
                    try {
                        var el = document.getElementById('scratch') || document.querySelector('[class^="gui_stage-wrapper_"]') || document.querySelector('[class*="gui_page-wrapper_"]');
                        if (el) {
                            var keys = Object.keys(el);
                            var reactKey = keys.find(function(k) { return k.startsWith('__reactInternalInstance') || k.startsWith('__reactFiber'); });
                            if (reactKey) {
                                var fiber = el[reactKey];
                                while (fiber) {
                                    if (fiber.stateNode && fiber.stateNode.props && fiber.stateNode.props.vm) return fiber.stateNode.props.vm;
                                    if (fiber.memoizedProps && fiber.memoizedProps.vm) return fiber.memoizedProps.vm;
                                    fiber = fiber.return;
                                }
                            }
                        }
                    } catch(e) {}
                    return null;
                }

                function tryInject() {
                    // 若检测到后续切片任务开启，旧探针立刻销毁，绝不干扰
                    if (window.__scratch_job_id !== currentJobId) return true; 
                    attempts++;

                    // 1. 🚀 TurboWarp 极速通道 (完美)
                    if (window.loadProject && typeof window.loadProject === 'function') {
                        var twData = uint8Array ? uint8Array.buffer : (rawData ? JSON.parse(rawData) : null);
                        if (twData) window.loadProject(twData);
                        console.log("Success: Injected via TurboWarp API.");
                        return true; 
                    }

                    // 2. 🐢 标准版底层强插通道
                    var targetVm = getVm();
                    
                    if (!targetVm || !targetVm.editingTarget || !targetVm.runtime || targetVm.runtime.targets.length === 0) {
                        readyCount = 0; return false; 
                    }
                    
                    // 利用最稳妥的 DOM 探测
                    var hasWorkspace = document.querySelector('.blocklyWorkspace') || document.querySelector('[class*="gui_blocks-wrapper"]');
                    if (!hasWorkspace) {
                        var frames = document.querySelectorAll('iframe');
                        for(var f=0; f<frames.length; f++){
                            try { if(frames[f].contentDocument.querySelector('.blocklyWorkspace')) { hasWorkspace = true; break; } }catch(e){}
                        }
                    }
                    if (!hasWorkspace) {
                        readyCount = 0; return false;
                    }

                    var loaderVisible = false;
                    var loaders = document.querySelectorAll('[class*="loader_fullscreen"], [class*="loader_background"]');
                    for (var i = 0; i < loaders.length; i++) {
                        if (window.getComputedStyle(loaders[i]).display !== 'none') {
                            loaderVisible = true; break;
                        }
                    }
                    if (loaderVisible) {
                        readyCount = 0; return false;
                    }

                    // 彻底稳定期：4周期 (约2秒)
                    readyCount++;
                    if (readyCount < 4) {
                        return false; 
                    }

                    console.log("Target VM locked. Executing safe payload.");

                    try {
                        // 兜底强刷：绕开所有 UI
                        var dataToLoad = uint8Array ? uint8Array.buffer : JSON.parse(rawData);
                        var loadPromise = targetVm.loadProject(dataToLoad);
                        
                        loadPromise.then(function() {
                            setTimeout(function() {
                                if (window.__scratch_job_id !== currentJobId) return;
                                
                                // 销毁残余旧视图
                                var bly = window.Blockly;
                                if (!bly) {
                                    var fs = document.querySelectorAll('iframe');
                                    for(var j=0; j<fs.length; j++) {
                                        try { if(fs[j].contentWindow && fs[j].contentWindow.Blockly) { bly = fs[j].contentWindow.Blockly; break; } }catch(e){}
                                    }
                                }
                                try { if (bly && bly.getMainWorkspace()) bly.getMainWorkspace().clear(); } catch(err){}
                                
                                if (targetVm.emitWorkspaceUpdate) targetVm.emitWorkspaceUpdate();
                                if (targetVm.emitTargetsUpdate) targetVm.emitTargetsUpdate();
                                
                                var targets = targetVm.runtime.targets;
                                if (targets && targets.length > 0 && targetVm.setEditingTarget) {
                                    var stage = targets.find(function(t) { return t.isStage; });
                                    var sprite = targets.find(function(t) { return !t.isStage; }) || targets[0];
                                    
                                    if (stage) targetVm.setEditingTarget(stage.id);
                                    setTimeout(function() {
                                        if (window.__scratch_job_id !== currentJobId) return;
                                        if (sprite) targetVm.setEditingTarget(sprite.id);
                                        
                                        // 强制唤醒 Redux 核心状态机
                                        try {
                                            var el = document.getElementById('scratch') || document.querySelector('[class^="gui_stage-wrapper_"]');
                                            if (el) {
                                                var keys = Object.keys(el);
                                                var reactKey = keys.find(function(k) { return k.startsWith('__reactInternalInstance') || k.startsWith('__reactFiber'); });
                                                if (reactKey) {
                                                    var fiber = el[reactKey];
                                                    var store = null;
                                                    while (fiber) {
                                                        if (fiber.stateNode && fiber.stateNode.store) { store = fiber.stateNode.store; break; }
                                                        if (fiber.memoizedProps && fiber.memoizedProps.store) { store = fiber.memoizedProps.store; break; }
                                                        fiber = fiber.return;
                                                    }
                                                    if (store) store.dispatch({ type: 'scratch-gui/project-state/SET_PROJECT_ID', projectId: 'injected_' + currentJobId });
                                                }
                                            }
                                        } catch(ex) {}
                                        
                                        window.dispatchEvent(new Event('resize'));
                                    }, 80);
                                } else {
                                    window.dispatchEvent(new Event('resize'));
                                }
                            }, 150);
                        }).catch(function(e) { console.error("VM load error:", e); });
                        
                        return true;
                    } catch(e) {
                        console.error("Injection error:", e);
                        readyCount = 0;
                        return false;
                    }
                }

                if (!tryInject()) {
                    var timer = setInterval(function() {
                        if (tryInject() || attempts >= maxAttempts || window.__scratch_job_id !== currentJobId) {
                            clearInterval(timer);
                        }
                    }, 500);
                }
                return "Polling Started for Job: " + currentJobId;
            } catch(e) {
                return "Fatal Error: " + e.message;
            }
        })();
    \"\"\".trimIndent()
    
    webView.evaluateJavascript(js, null)
}"""

if idx_start != -1:
    idx_end = content.find("class ScratchJsInterface(private val onChanged: () -> Unit) {", idx_start)
    if idx_end != -1:
        content = content[:idx_start] + new_fun + "\n\n" + content[idx_end:]
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(content)
        print("Done")
    else:
        print("Could not find class ScratchJsInterface")
else:
    print("Could not find loadProjectIntoWebView")
