import os

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

target = """val localFile = java.io.File(context.filesDir, "student_${studentId}_project_${report.workId}.sb3")"""
replacement = """val timestamp = System.currentTimeMillis()
                    val localFile = java.io.File(context.filesDir, "student_${studentId}_project_${report.workId}_${timestamp}.sb3")"""

if target in content:
    content = content.replace(target, replacement)
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched MainViewModel filename")
else:
    print("Target not found in MainViewModel")
