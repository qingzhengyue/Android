import os

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = 'onResult("成功批量导入 $count 名学生！学号前缀为 $prefix，默认密码 123456")'
replacement = 'onResult("成功批量导入 $count 名学生！学号前缀为 S$prefix，默认密码 123456")'

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched msg")
