import sys
import re

filepath = "app/src/main/java/com/example/ui/ScratchEditorScreen.kt"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Fix the bad injection in addBlockToScratch
bad_injection = """    val safeJson = projectJson.replace("\\\\", "\\\\\\\\").replace("\\\"", "\\\\\\\"").replace("\\n", "\\\\n").replace("\\r", "\\\\r").replace("$", "\\\\$")
    val js = \"\"\"
        (function() {
            try {
                if (window.addBlockFromAndroid) {"""

good_injection = """    val js = \"\"\"
        (function() {
            try {
                if (window.addBlockFromAndroid) {"""

content = content.replace(bad_injection, good_injection)

# 2. Fix the leftovers of ScratchProjectLoaderInterface class
# I will use a precise split
if "class ScratchProjectLoaderInterface" in content:
    idx = content.find("class ScratchProjectLoaderInterface")
    content = content[:idx]

# 3. Fix the declaration
content = content.replace("var projectLoaderInterface by remember { mutableStateOf<ScratchProjectLoaderInterface?>(null) }", "")
content = content.replace("var projectLoaderInterface by remember { mutableStateOf<ScratchProjectLoaderInterface?>(null) }", "")

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print("Done")
