with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "r") as f:
    content = f.read()

target = """        val formattedJson = remember(detailWork.workCode) {
            try { org.json.JSONObject(detailWork.workCode).toString(2) } catch (e: Exception) { detailWork.workCode }
        }"""

replacement = """        val formattedJson = remember(detailWork.workCode) {
            com.example.data.ScratchToPythonConverter.convertJsonToPython(detailWork.workCode)
        }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "w") as f:
        f.write(content)
    print("Patched formattedJson in TeacherWorksScreens.kt")
else:
    print("Target block not found in TeacherWorksScreens.kt")
