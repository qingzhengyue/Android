with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "r") as f:
    content = f.read()

broken = '"# [解析代码失败]: ${e.message}\n$scratchJson"'
content = content.replace(broken, '"# [解析代码失败]: ${e.message}\\n$scratchJson"')

with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "w") as f:
    f.write(content)
