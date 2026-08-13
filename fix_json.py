import re

with open('app/src/main/java/com/example/ui/ScratchEditorScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = """                if (jsonB64 && jsonB64.length > 0) {
                    try {
                        rawData = decodeURIComponent(escape(window.atob(jsonB64)));
                    } catch(e) { console.error("JSON decode error", e); }
                }"""

if target in content:
    content = content.replace(target, "")
    print("Removed old jsonB64 logic.")

with open('app/src/main/java/com/example/ui/ScratchEditorScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
