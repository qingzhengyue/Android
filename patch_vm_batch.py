import os

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = 'val prefix = "S${gStr}${cStr}"'
replacement = 'val prefix = "${gStr}${cStr}"'

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched batch prefix")
else:
    print("target not found")
