import os

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r", encoding="utf-8") as f:
    content = f.read()

target1 = "fun registerStudentByTeacher(studentNumber: String, name: String, pass: String, classId: Int, onResult: (String) -> Unit) {\n        viewModelScope.launch {\n            if (studentNumber.isBlank() || name.isBlank() || pass.isBlank()) {\n                onResult(\"各项输入不能为空！\")\n                return@launch\n            }\n            val existing = repository.getStudentByNumber(studentNumber)"
replacement1 = """fun registerStudentByTeacher(studentNumber: String, name: String, pass: String, classId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            if (studentNumber.isBlank() || name.isBlank() || pass.isBlank()) {
                onResult("各项输入不能为空！")
                return@launch
            }
            val cleanNum = studentNumber.replace(Regex("[^0-9]"), "")
            if (cleanNum.isEmpty()) {
                onResult("学号必须包含数字！")
                return@launch
            }
            val existing = repository.getStudentByNumber(cleanNum)"""

if target1 in content:
    content = content.replace(target1, replacement1)
    # also change where studentNumber is passed to Student(...)
    target1_part2 = """            val student = Student(
                studentNumber = studentNumber,
                name = name,
                password = pass,
                classId = classId
            )"""
    replacement1_part2 = """            val student = Student(
                studentNumber = cleanNum,
                name = name,
                password = pass,
                classId = classId
            )"""
    content = content.replace(target1_part2, replacement1_part2)
    print("Patched registerStudentByTeacher")
else:
    print("target1 not found")

target2 = """    fun studentRegister(studentNum: String, name: String, pass: String, classId: Int, onSuccess: () -> Unit) {
        val cleanNum = studentNum.trim().uppercase()"""
replacement2 = """    fun studentRegister(studentNum: String, name: String, pass: String, classId: Int, onSuccess: () -> Unit) {
        val cleanNum = studentNum.replace(Regex("[^0-9]"), "")"""

if target2 in content:
    content = content.replace(target2, replacement2)
    
    # We also need to add a check for cleanNum being empty
    target2_part2 = """        val cleanPass = pass.trim()
        viewModelScope.launch {
            _currentBtnLoading.value = true"""
    replacement2_part2 = """        val cleanPass = pass.trim()
        viewModelScope.launch {
            _currentBtnLoading.value = true
            if (cleanNum.isEmpty()) {
                _authError.value = "学号必须包含数字！"
                _currentBtnLoading.value = false
                return@launch
            }"""
    content = content.replace(target2_part2, replacement2_part2)
    print("Patched studentRegister")
else:
    print("target2 not found")

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w", encoding="utf-8") as f:
    f.write(content)

