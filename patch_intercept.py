import re

with open('app/src/main/java/com/example/ui/ScratchEditorScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update ProjectDataProvider to hold ByteArray
provider_regex = r'object ProjectDataProvider \{.*?\n\}'
provider_replacement = """object ProjectDataProvider {
    var projectJson: String = ""
    var base64Data: String = ""
    var projectBytes: ByteArray? = null
    
    @android.webkit.JavascriptInterface
    fun fetchProjectJson(): String = projectJson

    @android.webkit.JavascriptInterface
    fun fetchBase64Data(): String = base64Data
    
    @android.webkit.JavascriptInterface
    fun clearData() {
        projectJson = ""
        base64Data = ""
        projectBytes = null
    }
}"""

if re.search(provider_regex, content, re.DOTALL):
    content = re.sub(provider_regex, provider_replacement, content, flags=re.DOTALL)
    print("Updated ProjectDataProvider")
else:
    print("Provider not found")
    
# 2. Add shouldInterceptRequest
intercept_regex = r'override fun shouldOverrideUrlLoading\(view: WebView\?, request: WebResourceRequest\?\): Boolean \{'
intercept_replacement = """                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                            val url = request?.url?.toString() ?: return null
                            if (url == "https://local.scratch.app/project.sb3") {
                                val bytes = ProjectDataProvider.projectBytes
                                if (bytes != null) {
                                    val stream = java.io.ByteArrayInputStream(bytes)
                                    val response = WebResourceResponse("application/octet-stream", "UTF-8", stream)
                                    response.responseHeaders = mapOf("Access-Control-Allow-Origin" to "*")
                                    return response
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                        
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {"""
                        
if intercept_regex in content:
    content = content.replace(intercept_regex, intercept_replacement)
    print("Added shouldInterceptRequest")
else:
    print("intercept_regex not found")

# 3. Update loadProjectIntoWebView to set projectBytes
load_regex = r'    // Store data in the interface object\s+ProjectDataProvider\.projectJson = projectJson\s+ProjectDataProvider\.base64Data = base64Data'
load_replacement = """    // Store data in the interface object
    ProjectDataProvider.projectJson = projectJson
    ProjectDataProvider.base64Data = base64Data
    ProjectDataProvider.projectBytes = try {
        if (base64Data.isNotEmpty()) android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT) else null
    } catch(e: Exception) { null }"""
    
if re.search(load_regex, content):
    content = re.sub(load_regex, load_replacement, content)
    print("Updated loadProjectIntoWebView")
else:
    print("load_regex not found")
    
# 4. Update JS to fetch from local.scratch.app
js_regex = r'                if \(base64Data && base64Data\.length > 0\) \{\s+fetch\("data:application/octet-stream;base64," \+ base64Data\)'
js_replacement = """                if (base64Data && base64Data.length > 0) {
                    fetch("https://local.scratch.app/project.sb3")"""

if re.search(js_regex, content):
    content = re.sub(js_regex, js_replacement, content)
    print("Updated JS fetch")
else:
    print("js_regex not found")

with open('app/src/main/java/com/example/ui/ScratchEditorScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
