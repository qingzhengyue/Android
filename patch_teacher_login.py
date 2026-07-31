import os

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r", encoding="utf-8") as f:
    content = f.read()

target1 = """    fun teacherRegister(workId: String, name: String, pass: String, onSuccess: () -> Unit) {
        val cleanId = workId.trim().uppercase()
        val cleanName = name.trim()
        val cleanPass = pass.trim()
        viewModelScope.launch {
            _currentBtnLoading.value = true
            if (cleanNum.isEmpty()) {
                _authError.value = "学号必须包含数字！"
                _currentBtnLoading.value = false
                return@launch
            }"""

replacement1 = """    fun teacherRegister(workId: String, name: String, pass: String, onSuccess: () -> Unit) {
        val cleanId = workId.trim().uppercase()
        val cleanName = name.trim()
        val cleanPass = pass.trim()
        viewModelScope.launch {
            _currentBtnLoading.value = true"""

target2 = """    fun teacherLogin(workId: String, pass: String, onSuccess: () -> Unit) {
        val cleanId = workId.trim().uppercase()
        val cleanPass = pass.trim()
        viewModelScope.launch {
            _currentBtnLoading.value = true
            if (cleanNum.isEmpty()) {
                _authError.value = "学号必须包含数字！"
                _currentBtnLoading.value = false
                return@launch
            }"""

replacement2 = """    fun teacherLogin(workId: String, pass: String, onSuccess: () -> Unit) {
        val cleanId = workId.trim().uppercase()
        val cleanPass = pass.trim()
        viewModelScope.launch {
            _currentBtnLoading.value = true"""

target3 = """    fun studentLogin(studentNum: String, pass: String, onSuccess: () -> Unit) {
        // 健壮性处理：剔除输入中的所有字母和特殊字符，只保留纯数字
        // 这样无论学生输入 "S3101" 还是 "3101"，都能提取出 3101
        val rawNum = studentNum.trim()
        val cleanNumStr = rawNum.replace(Regex("[^0-9]"), "")
        // 转为 Long 类型，确保 4 位数字编码规则（防溢出/类型对齐）
        val cleanNumLong = cleanNumStr.toLongOrNull() ?: 0L
        
        val cleanPass = pass.trim()
        viewModelScope.launch {
            _currentBtnLoading.value = true
            if (cleanNum.isEmpty()) {
                _authError.value = "学号必须包含数字！"
                _currentBtnLoading.value = false
                return@launch
            }"""
            
replacement3 = """    fun studentLogin(studentNum: String, pass: String, onSuccess: () -> Unit) {
        // 健壮性处理：剔除输入中的所有字母和特殊字符，只保留纯数字
        // 这样无论学生输入 "S3101" 还是 "3101"，都能提取出 3101
        val rawNum = studentNum.trim()
        val cleanNumStr = rawNum.replace(Regex("[^0-9]"), "")
        // 转为 Long 类型，确保 4 位数字编码规则（防溢出/类型对齐）
        val cleanNumLong = cleanNumStr.toLongOrNull() ?: 0L
        
        val cleanPass = pass.trim()
        viewModelScope.launch {
            _currentBtnLoading.value = true
            if (cleanNumStr.isEmpty()) {
                _authError.value = "学号必须包含数字！"
                _currentBtnLoading.value = false
                return@launch
            }"""

content = content.replace(target1, replacement1)
content = content.replace(target2, replacement2)
content = content.replace(target3, replacement3)

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w", encoding="utf-8") as f:
    f.write(content)
print("done")
