import os

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = """    fun studentRegister(studentNum: String, name: String, pass: String, classId: Int, onSuccess: () -> Unit) {
        val cleanNum = studentNum.replace(Regex("[^0-9]"), "")
        val cleanName = name.trim()
        val cleanPass = pass.trim()
        viewModelScope.launch {
            _currentBtnLoading.value = true
            _authError.value = null"""

replacement = """    fun studentRegister(studentNum: String, name: String, pass: String, classId: Int, onSuccess: () -> Unit) {
        val cleanNum = studentNum.replace(Regex("[^0-9]"), "")
        val cleanName = name.trim()
        val cleanPass = pass.trim()
        viewModelScope.launch {
            _currentBtnLoading.value = true
            if (cleanNum.isEmpty()) {
                _authError.value = "学号必须包含数字！"
                _currentBtnLoading.value = false
                return@launch
            }
            _authError.value = null"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w", encoding="utf-8") as f:
    f.write(content)

