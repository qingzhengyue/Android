with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace('fallbackId: Long', 'fallbackId: Int')
content = content.replace('Regex("([一二三四五六七八九十0-9]+)\s*班")', 'Regex("([一二三四五六七八九十0-9]+)\\\\s*班")')

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w", encoding="utf-8") as f:
    f.write(content)
