import re

with open('app/src/main/java/com/example/data/AppRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

if "import kotlinx.coroutines.launch" not in content:
    content = content.replace("import kotlinx.coroutines.withContext", "import kotlinx.coroutines.withContext\nimport kotlinx.coroutines.launch\nimport kotlinx.coroutines.GlobalScope")

with open('app/src/main/java/com/example/data/AppRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
