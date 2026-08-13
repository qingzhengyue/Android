import re

with open('app/src/main/java/com/example/ui/ScratchEditorScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

regex2 = r'                if \(!tryInject\(\)\) \{\s*var timer = setInterval\(function\(\) \{\s*if \(tryInject\(\) \|\| attempts >= maxAttempts \|\| window\.__scratch_job_id !== currentJobId\) \{\s*clearInterval\(timer\);\s*\}\s*\}, 500\);\s*\}\s*return "Polling Started for Job: " \+ currentJobId;\s*\} catch\(e\) \{'

replacement2 = """                function startInjecting() {
                    if (!tryInject()) {
                        var timer = setInterval(function() {
                            if (tryInject() || attempts >= maxAttempts || window.__scratch_job_id !== currentJobId) {
                                clearInterval(timer);
                            }
                        }, 500);
                    }
                }
                
                if (base64Data && base64Data.length > 0) {
                    fetch("data:application/octet-stream;base64," + base64Data)
                        .then(res => res.arrayBuffer())
                        .then(buffer => {
                            uint8Array = new Uint8Array(buffer);
                            startInjecting();
                        })
                        .catch(e => {
                            console.error("Base64 decode failed", e);
                        });
                } else {
                    startInjecting();
                }
                
                return "Polling Started for Job: " + currentJobId;
            } catch(e) {"""

if re.search(regex2, content, re.DOTALL):
    content = re.sub(regex2, replacement2, content, flags=re.DOTALL)
    print("Replaced JS tail logic.")
else:
    print("Regex JS tail not found.")

with open('app/src/main/java/com/example/ui/ScratchEditorScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
