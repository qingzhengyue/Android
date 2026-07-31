import os

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = """            var cStr = ""
            if (classMatch != null) {
                val c = classMatch.groupValues[1]
                cStr = numMap[c] ?: c
            } else {
                cStr = "1"
            }"""

replacement = """            var cStr = ""
            if (classMatch != null) {
                val c = classMatch.groupValues.getOrElse(1) { classMatch.value }
                cStr = numMap[c] ?: c
            } else {
                cStr = "1"
            }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched cStr")
else:
    print("Target not found")
