import re

with open('app/src/main/java/com/example/ui/ScratchEditorScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

intercept_regex = r'if \(url == "https://local\.scratch\.app/project\.sb3"\) \{'
intercept_replacement = """if (url.endsWith("/___android_injected_project.sb3")) {"""

if re.search(intercept_regex, content):
    content = re.sub(intercept_regex, intercept_replacement, content)
    print("Replaced intercept URL")
else:
    print("intercept_regex not found")

js_regex = r'fetch\("https://local\.scratch\.app/project\.sb3"\)'
js_replacement = """fetch("/___android_injected_project.sb3")"""

if re.search(js_regex, content):
    content = re.sub(js_regex, js_replacement, content)
    print("Replaced JS fetch URL")
else:
    print("js_regex not found")

with open('app/src/main/java/com/example/ui/ScratchEditorScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
