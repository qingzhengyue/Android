import os

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = 'val randSuffix = 1'
replacement = """val existingStudents = repository.getStudentsByClass(classId)
            val randSuffix = existingStudents.size + 1"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched randSuffix")
else:
    print("target not found")
