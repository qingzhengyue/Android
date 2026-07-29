import sys
import re

filepath = "app/src/main/java/com/example/ui/ScratchEditorScreen.kt"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# We want to replace the body of fun loadProjectIntoWebView(webView: WebView?, projectJson: String, base64Data: String)
# We will use regex to find this function block.
# Since it's the second to last thing in the file before ScratchJsInterface, we can extract from it to the end.

idx_start = content.find("fun loadProjectIntoWebView(webView: WebView?, projectJson: String, base64Data: String)")

new_fun = """fun loadProjectIntoWebView(webView: WebView?, projectJson: String, base64Data: String) {
    if (webView == null) return

    val safeJson = projectJson.replace("\\\\", "\\\\\\\\").replace("\\\"", "\\\\\\\"").replace("\\n", "\\\\n").replace("\\r", "\\\\r").replace("$", "\\\\$")
    val js = \"\"\"
        (function() {
            try {
                window.__scratch_job_id = (window.__scratch_job_id || 0) + 1;
                var currentJobId = window.__scratch_job_id;

                var rawData = "${safeJson}";
                var base64Data = "${base64Data}";
                
                if ((!base64Data || base64Data.length === 0) && (!rawData || rawData.length === 0)) return "Empty data";
                
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
                    if (window.__scratch_job_id !== currentJobId) return true; 
                    attempts++;

                    // 1. 🚀 TurboWarp
                    if (window.loadProject && typeof window.loadProject === 'function') {
                        var twData = uint8Array ? uint8Array.buffer : JSON.parse(rawData);
                        window.loadProject(twData);
                        console.log("Success: Injected via TurboWarp API.");
                        return true; 
                    }

                    // 2. 🐢 标准版底层强插通道
                    var targetVm = getVm();
                    
                    if (!targetVm || !targetVm.editingTarget || !targetVm.runtime || targetVm.runtime.targets.length === 0) {
                        readyCount = 0; return false; 
                    }
                    
                    // ★ 终极修复：绝对不能校验 window.Blockly！官方编译版不暴露它！
                    // 改为校验 DOM 节点是否存在，证明界面已经就绪
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

                    readyCount++;
                    if (readyCount < 4) {
                        console.log("Waiting for React UI to settle... (" + readyCount + "/4)");
                        return false; 
                    }

                    console.log("Target VM locked. Executing safe payload.");

                    try {
                        var fileInputs = document.querySelectorAll('input[type="file"]');
                        var reactInjected = false;

                        if (uint8Array && fileInputs.length > 0) {
                            var dynamicFileName = "project_" + Date.now() + ".sb3";
                            var file = new File([uint8Array.buffer], dynamicFileName, { type: "application/x.scratch.sb3" });
                            var dt = new DataTransfer();
                            dt.items.add(file);
                            
                            for (var i = 0; i < fileInputs.length; i++) {
                                var input = fileInputs[i];
                                var keys = Object.keys(input);
                                var reactKey = keys.find(function(k) { return k.startsWith('__reactProps') || k.startsWith('__reactEventHandlers'); });
                                
                                if (reactKey && input[reactKey] && input[reactKey].onChange) {
                                    input.value = ''; // 必须清空，防同名文件拦截
                                    try {
                                        var nativeFileSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "files").set;
                                        if (nativeFileSetter) nativeFileSetter.call(input, dt.files);
                                    } catch(e) {
                                        input.files = dt.files;
                                    }
                                    input[reactKey].onChange({
                                        target: input,
                                        currentTarget: input,
                                        preventDefault: function() {},
                                        stopPropagation: function() {}
                                    });
                                    reactInjected = true;
                                }
                            }
                        }

                        if (reactInjected) {
                            console.log("Injected via React File Input bypass!");
                            return true;
                        }

                        // VM 兜底强刷
                        var loadPromise = uint8Array ? targetVm.loadProject(uint8Array.buffer) : targetVm.loadProject(JSON.parse(rawData));
                        loadPromise.then(function() {
                            setTimeout(function() {
                                if (window.__scratch_job_id !== currentJobId) return;
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
                                        
                                        // 强制唤醒 Redux 状态
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
}

class ScratchJsInterface(private val onChanged: () -> Unit) {
    @android.webkit.JavascriptInterface
    fun onCodeChanged() {
        onChanged()
    }
}"""

idx_end_class = content.find("class ScratchJsInterface", idx_start)

if idx_start != -1:
    content = content[:idx_start] + new_fun

    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Done applying V15 loadProjectIntoWebView")
else:
    print("Could not find loadProjectIntoWebView")

