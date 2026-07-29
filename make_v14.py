import sys

filepath = "app/src/main/java/com/example/ui/ScratchEditorScreen.kt"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Replace non-breaking spaces
content = content.replace("\xa0", " ")

# 1. Update handleCodeInjection call
content = content.replace("loadProjectIntoWebView(webView)", "loadProjectIntoWebView(webView, finalJson, finalBase64)")

# 2. Update signature
old_sig = "fun loadProjectIntoWebView(webView: WebView?) {"
new_sig = "fun loadProjectIntoWebView(webView: WebView?, projectJson: String, base64Data: String) {"
content = content.replace(old_sig, new_sig)

# 3. Update the inner js injection for loadProjectIntoWebView
old_js_start = """    val js = \"\"\"
        (function() {
            try {
                window.__scratch_job_id = (window.__scratch_job_id || 0) + 1;
                var currentJobId = window.__scratch_job_id;

                var rawData = window.AndroidProjectLoader ? window.AndroidProjectLoader.getProjectJson() : null;
                var base64Data = window.AndroidProjectLoader ? window.AndroidProjectLoader.getProjectBase64() : null;"""

new_js_start = """    val safeJson = projectJson.replace("\\\\", "\\\\\\\\").replace("\\\"", "\\\\\\\"").replace("\\n", "\\\\n").replace("\\r", "\\\\r").replace("$", "\\\\$")
    val js = \"\"\"
        (function() {
            try {
                window.__scratch_job_id = (window.__scratch_job_id || 0) + 1;
                var currentJobId = window.__scratch_job_id;

                var rawData = "${safeJson}";
                var base64Data = "${base64Data}";"""

if old_js_start in content:
    content = content.replace(old_js_start, new_js_start)
else:
    print("WARNING: Could not find old_js_start to replace!")

# 4. Remove projectLoaderInterface initialization
content = content.replace("var projectLoaderInterface by remember { mutableStateOf<ScratchProjectLoaderInterface?>(null) }", "")
content = content.replace("projectLoaderInterface?.setProjectData(finalJson, finalBase64)", "")

old_loader_setup = """                    val loaderInterface = ScratchProjectLoaderInterface()
                    projectLoaderInterface = loaderInterface
                    addJavascriptInterface(ScratchJsInterface {
                        scratchChangeCounter++
                    }, "AndroidWorkspace")
                    addJavascriptInterface(loaderInterface, "AndroidProjectLoader")
                    addJavascriptInterface(loaderInterface, "AndroidBlockViewer")"""

new_loader_setup = """                    addJavascriptInterface(ScratchJsInterface {
                        scratchChangeCounter++
                    }, "AndroidWorkspace")"""

if old_loader_setup in content:
    content = content.replace(old_loader_setup, new_loader_setup)
else:
    print("WARNING: Could not find old_loader_setup to replace!")
    
# 5. Remove ScratchProjectLoaderInterface class at the end
idx = content.find("class ScratchProjectLoaderInterface(")
if idx != -1:
    content = content[:idx]

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print("Done")
