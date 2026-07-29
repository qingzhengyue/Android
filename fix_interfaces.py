import sys
import re

filepath = "app/src/main/java/com/example/ui/ScratchEditorScreen.kt"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Remove var projectLoaderInterface by remember ...
content = re.sub(r'var projectLoaderInterface by remember \{ mutableStateOf<ScratchProjectLoaderInterface\?>(null) \}?\n?', '', content)
content = re.sub(r'var projectLoaderInterface \?= null\n?', '', content)

# 2. Remove projectLoaderInterface?.setProjectData...
content = re.sub(r'projectLoaderInterface\?\.setProjectData\(finalJson, finalBase64\)\n?', '', content)

# 3. Remove loaderInterface instantiation and addJavascriptInterface
content = re.sub(r'val loaderInterface = ScratchProjectLoaderInterface\(\)\n?', '', content)
content = re.sub(r'projectLoaderInterface = loaderInterface\n?', '', content)
content = re.sub(r'addJavascriptInterface\(loaderInterface, "AndroidProjectLoader"\)\n?', '', content)
content = re.sub(r'addJavascriptInterface\(loaderInterface, "AndroidBlockViewer"\)\n?', '', content)

# 4. Remove ScratchProjectLoaderInterface class completely
content = re.sub(r'class ScratchProjectLoaderInterface\([\s\S]*?\}\n', '', content)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print("Removed unused interfaces.")
