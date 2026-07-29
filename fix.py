import sys

filepath = "app/src/main/java/com/example/ui/ScratchEditorScreen.kt"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace("\xa0", " ")

content = content.replace("loadProjectIntoWebView(webView)", "loadProjectIntoWebView(webView, finalJson, finalBase64)")

old_sig = "fun loadProjectIntoWebView(webView: WebView?) {"
new_sig = "fun loadProjectIntoWebView(webView: WebView?, projectJson: String, base64Data: String) {"
content = content.replace(old_sig, new_sig)

old_vars = """var rawData = window.AndroidProjectLoader ? window.AndroidProjectLoader.getProjectJson() : null;
                var base64Data = window.AndroidProjectLoader ? window.AndroidProjectLoader.getProjectBase64() : null;"""

new_vars = """var rawData = "${safeJson}";
                var base64Data = "${base64Data}";"""

if old_vars in content:
    content = content.replace(old_vars, new_vars)
else:
    print("WARNING: Could not find old_vars to replace!")
    print(repr(old_vars))

# Add the safeJson variable right before val js = \"\"\"
old_val_js = "val js = \"\"\""
new_val_js = """val safeJson = projectJson.replace("\\\\", "\\\\\\\\").replace("\\\"", "\\\\\\\"").replace("\\n", "\\\\n").replace("\\r", "\\\\r").replace("$", "\\\\$")
    val js = \"\"\""""

content = content.replace(old_val_js, new_val_js)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print("Done")
