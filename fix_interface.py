import re

with open('app/src/main/java/com/example/ui/ScratchEditorScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("fun getProjectJson()", "fun fetchProjectJson()")
content = content.replace("fun getBase64Data()", "fun fetchBase64Data()")

content = content.replace("window.AndroidProjectProvider.getProjectJson()", "window.AndroidProjectProvider.fetchProjectJson()")
content = content.replace("window.AndroidProjectProvider.getBase64Data()", "window.AndroidProjectProvider.fetchBase64Data()")

with open('app/src/main/java/com/example/ui/ScratchEditorScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
