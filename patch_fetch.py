import re

with open('app/src/main/java/com/example/ui/ScratchEditorScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the injection Javascript to use async fetch
regex = r'// Natively fetch data from JavascriptInterface.*?function tryInject\(\) \{'

replacement = """
                window.__scratch_job_id = currentJobId;
                
                // Natively fetch data from JavascriptInterface, bypassing all IPC limits
                var rawData = window.AndroidProjectProvider.fetchProjectJson() || "";
                var base64Data = window.AndroidProjectProvider.fetchBase64Data() || "";
                window.AndroidProjectProvider.clearData(); // Clean up immediately
                
                if ((!base64Data || base64Data.length === 0) && (!rawData || rawData.length === 0)) return "Empty data";
                
                var uint8Array = null;
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
                
                function tryInject() {"""

if re.search(regex, content, re.DOTALL):
    content = re.sub(regex, replacement, content, flags=re.DOTALL)
    print("Replaced JS header logic.")
else:
    print("Regex JS header not found.")
    
# Replace the tryInject call
regex2 = r'// 5\. 挂载重试探测器.*?tryInject\(\);.*?\} catch\(e\)'

replacement2 = """// 5. 挂载重试探测器
                    if (!tryInject()) {
                        setTimeout(arguments.callee, 500);
                    }
                }
                
                if (base64Data && base64Data.length > 0) {
                    fetch("data:application/octet-stream;base64," + base64Data)
                        .then(res => res.arrayBuffer())
                        .then(buffer => {
                            uint8Array = new Uint8Array(buffer);
                            tryInject();
                        })
                        .catch(e => {
                            console.error("Base64 decode failed", e);
                        });
                } else {
                    tryInject();
                }
                
            } catch(e)"""

if re.search(regex2, content, re.DOTALL):
    content = re.sub(regex2, replacement2, content, flags=re.DOTALL)
    print("Replaced JS tail logic.")
else:
    print("Regex JS tail not found.")

with open('app/src/main/java/com/example/ui/ScratchEditorScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
