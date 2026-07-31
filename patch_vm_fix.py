import os

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = """        viewModelScope.launch {
            _currentBtnLoading.value = true
            if (cleanNum.isEmpty()) {
                _authError.value = "学号必须包含数字！"
                _currentBtnLoading.value = false
                return@launch
            }"""
replacement = """        viewModelScope.launch {
            _currentBtnLoading.value = true"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w", encoding="utf-8") as f:
    f.write(content)

