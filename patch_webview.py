import re

with open('app/src/main/java/com/example/ui/ScratchEditorScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 3. Rewrite loadProjectIntoWebView
regex = r'fun loadProjectIntoWebView\(webView: WebView\?, projectJson: String, base64Data: String\) \{.*?var rawData = "";'

replacement_load = """fun loadProjectIntoWebView(webView: WebView?, projectJson: String, base64Data: String) {
    if (webView == null) return
    val jobId = System.currentTimeMillis()
    
    // Store data in the interface object
    ProjectDataProvider.projectJson = projectJson
    ProjectDataProvider.base64Data = base64Data

    // 3. 开始执行核心注入
    val js = \"\"\"
        (function() {
            try {
                var currentJobId = $jobId;
                
                // Natively fetch data from JavascriptInterface, bypassing all IPC limits
                var rawData = window.AndroidProjectProvider.getProjectJson() || "";
                var base64Data = window.AndroidProjectProvider.getBase64Data() || "";
                window.AndroidProjectProvider.clearData(); // Clean up immediately
                
                if ((!base64Data || base64Data.length === 0) && (!rawData || rawData.length === 0)) return "Empty data";"""

if re.search(regex, content, re.DOTALL):
    content = re.sub(regex, replacement_load, content, flags=re.DOTALL)
    print("Replaced loadProjectIntoWebView logic.")
else:
    print("Regex loadProjectIntoWebView not found.")

with open('app/src/main/java/com/example/ui/ScratchEditorScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
