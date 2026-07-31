with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "r") as f:
    content = f.read()

target = """                                            webViewClient = object : WebViewClient() {
                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    super.onPageFinished(view, url)
                                                    val safeJsonLiteral = org.json.JSONObject.quote(detailWork.workCode)
                                                    view?.evaluateJavascript("if(window.loadProject){ window.loadProject($safeJsonLiteral); }", null)
                                                }
                                            }"""

replacement = """                                            webViewClient = object : WebViewClient() {
                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    super.onPageFinished(view, url)
                                                    view?.evaluateJavascript("if(window.setViewOnly){ window.setViewOnly(true); }", null)
                                                    val safeJsonLiteral = org.json.JSONObject.quote(detailWork.workCode)
                                                    view?.evaluateJavascript("if(window.loadProject){ window.loadProject($safeJsonLiteral); }", null)
                                                }
                                            }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "w") as f:
    f.write(content)
