with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r") as f:
    content = f.read()

import re
pattern = r'fun loadReportForWork\(workId: Int\) \{\s*viewModelScope\.launch \{\s*_isReportLoading\.value = true\s*_activeReport\.value = repository\.getReportForWork\(workId\)\s*_isReportLoading\.value = false\s*\}\s*\}'
replacement = """fun loadReportForWork(workId: Int) {
        viewModelScope.launch {
            _isReportLoading.value = true
            try {
                _activeReport.value = repository.getReportForWork(workId)
            } catch (e: Exception) {
                e.printStackTrace()
                _activeReport.value = null
            } finally {
                _isReportLoading.value = false
            }
        }
    }"""
if re.search(pattern, content):
    content = re.sub(pattern, replacement, content)
    with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w") as f:
        f.write(content)
    print("Fixed")
else:
    print("Not found")
