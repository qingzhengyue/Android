with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r") as f:
    content = f.read()

target = """    fun submitWorkAndAiReport(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val studentId = _currentUserId.value
            val classId = _currentClassId.value
            if (studentId == -1) {
                onResult("错误：请先登录")
                return@launch
            }"""

replacement = """    fun submitWorkAndAiReport(onResult: (String) -> Unit) {
        android.util.Log.d("SupabaseDebug", "====== 🎯 提交作品按钮被成功触发了！======")
        viewModelScope.launch {
            val studentId = _currentUserId.value
            val classId = _currentClassId.value
            if (studentId == -1) {
                android.util.Log.e("SupabaseDebug", "错误：当前 studentId = -1，用户未登录！")
                onResult("错误：请先登录")
                return@launch
            }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w") as f:
        f.write(content)
    print("MainViewModel logs added.")
else:
    print("Target not found.")
