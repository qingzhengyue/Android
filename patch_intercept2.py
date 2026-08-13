import re

with open('app/src/main/java/com/example/ui/ScratchEditorScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

intercept_regex = r'                        override fun shouldOverrideUrlLoading\(view: WebView\?, request: WebResourceRequest\?\): Boolean \{'
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

with open('app/src/main/java/com/example/ui/ScratchEditorScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
