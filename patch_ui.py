import os

with open("app/src/main/java/com/example/ui/TeacherClassScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

target1 = """viewModel.batchImportStudentsByTeacher(
                            namesStr = batchNamesInput,
                            classId = selClass.classId
                        )"""
replacement1 = """viewModel.batchImportStudentsByTeacher(
                            namesStr = batchNamesInput,
                            classEntity = selClass
                        )"""

content = content.replace(target1, replacement1)

target2 = 'text = "S${student.studentNumber}"'
replacement2 = 'text = student.studentNumber'

content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/ui/TeacherClassScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)
print("Done")
