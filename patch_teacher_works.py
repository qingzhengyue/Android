import re

with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "r") as f:
    content = f.read()

target_json = """        val formattedJson = remember(detailWork.workCode) {
            com.example.data.ScratchToPythonConverter.convertJsonToPython(detailWork.workCode)
        }"""
        
replacement_json = """        val formattedJson = remember(detailWork.workCode) {
            if (detailWork.workCode.isBlank() || detailWork.workCode == "{}" || detailWork.workCode == "\"\"") {
                "# [此作品未包含任何积木代码，可能学生提交了空草稿]"
            } else {
                com.example.data.ScratchToPythonConverter.convertJsonToPython(detailWork.workCode)
            }
        }"""

if target_json in content:
    content = content.replace(target_json, replacement_json)
    print("Patched formattedJson logic")

target_webview = """                                        WebView(ctx).apply {
                                            settings.javaScriptEnabled = true"""
                                            
replacement_webview = """                                        WebView(ctx).apply {
                                            layoutParams = android.view.ViewGroup.LayoutParams(
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            webChromeClient = android.webkit.WebChromeClient()
                                            settings.javaScriptEnabled = true"""

if target_webview in content:
    content = content.replace(target_webview, replacement_webview)
    print("Patched WebView layoutParams")

with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "w") as f:
    f.write(content)
