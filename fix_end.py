import sys

filepath = "app/src/main/java/com/example/ui/ScratchEditorScreen.kt"
with open(filepath, "r", encoding="utf-8") as f:
    lines = f.readlines()

# Find the end of the class ScratchJsInterface
# The line "    }}" is likely the end of ScratchJsInterface.
# Let's find the last occurrence of "    }}" or just cut from "@android.webkit.JavascriptInterface"

new_lines = []
for line in lines:
    if "@android.webkit.JavascriptInterface" in line and "fun getProjectData" not in "".join(new_lines[-5:]):
        # We can just break
        break
    new_lines.append(line)

with open(filepath, "w", encoding="utf-8") as f:
    f.writelines(new_lines)

print("Done")
