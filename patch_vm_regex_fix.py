import os

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = 'val classMatch = Regex("([一二三四五六七八九十0-9]+)\s*班").find(classEntity.className) ?: Regex("(?<=[(（])[一二三四五六七八九十0-9]+(?=[)）])").find(classEntity.className) ?: Regex("([一二三四五六七八九十0-9]+)").findAll(classEntity.className).lastOrNull()'
replacement = 'val classMatch = Regex("([一二三四五六七八九十0-9]+)\\\\s*班").find(classEntity.className) ?: Regex("(?<=[(（])[一二三四五六七八九十0-9]+(?=[)）])").find(classEntity.className) ?: Regex("([一二三四五六七八九十0-9]+)").findAll(classEntity.className).lastOrNull()'

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched regex syntax")
else:
    print("Target not found")
