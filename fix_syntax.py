with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "r") as f:
    content = f.read()

content = content.replace('detailWork.workCode == "\\"\\"" {', 'detailWork.workCode == "\\"\\\"") {')

with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "w") as f:
    f.write(content)
