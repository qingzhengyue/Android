with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace('val newStudentNum = "S${newClassId}${(index + 1).toString().padStart(3, \'0\')}"', 'val newStudentNum = "${newClassId}${(index + 1).toString().padStart(3, \'0\')}"')
content = content.replace('val newStudentNum = "S${disabledClass.classId}${(index + 1).toString().padStart(3, \'0\')}"', 'val newStudentNum = "${disabledClass.classId}${(index + 1).toString().padStart(3, \'0\')}"')

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w", encoding="utf-8") as f:
    f.write(content)
