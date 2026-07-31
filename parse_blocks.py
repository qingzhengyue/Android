with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "r") as f:
    content = f.read()

target = """                                AndroidView(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            layoutParams = android.view.ViewGroup.LayoutParams(
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            webChromeClient = android.webkit.WebChromeClient()
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.allowFileAccess = true
                                            settings.allowFileAccessFromFileURLs = true
                                            settings.allowUniversalAccessFromFileURLs = true
                                            settings.builtInZoomControls = true
                                            settings.displayZoomControls = false
                                            
                                            val jsInterface = TeacherBlockViewerInterface(detailWork.workCode)
                                            addJavascriptInterface(jsInterface, "AndroidProjectLoader")
                                            loadUrl("file:///android_asset/scratch_blocks_viewer.html")
                                        }
                                    }
                                )"""

replacement = """                                val blockOpcodes = remember(detailWork.workCode) {
                                    val opcodes = mutableListOf<String>()
                                    try {
                                        val json = org.json.JSONObject(detailWork.workCode)
                                        if (json.has("targets")) {
                                            val targets = json.getJSONArray("targets")
                                            for (i in 0 until targets.length()) {
                                                val target = targets.getJSONObject(i)
                                                if (target.has("blocks")) {
                                                    val blocksObj = target.getJSONObject("blocks")
                                                    blocksObj.keys().forEach { key ->
                                                        val block = blocksObj.optJSONObject(key)
                                                        val opcode = block?.optString("opcode")
                                                        if (!opcode.isNullOrEmpty()) {
                                                            opcodes.add(opcode)
                                                        }
                                                    }
                                                }
                                            }
                                        } else if (json.has("blocks")) {
                                            val blocksObj = json.getJSONObject("blocks")
                                            blocksObj.keys().forEach { key ->
                                                val block = blocksObj.optJSONObject(key)
                                                val opcode = block?.optString("opcode")
                                                if (!opcode.isNullOrEmpty()) {
                                                    opcodes.add(opcode)
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    opcodes
                                }

                                androidx.compose.foundation.lazy.LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    contentPadding = PaddingValues(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(blockOpcodes) { opcode ->
                                        val zhName = BlockTranslator.getChineseName(opcode)
                                        val color = BlockTranslator.getBlockColor(opcode)
                                        
                                        Box(
                                            modifier = Modifier
                                                .background(color = color, shape = RoundedCornerShape(8.dp))
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = zhName,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "w") as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Target not found, trying partial match...")
    import re
    # Fallback with regex
    pattern = re.compile(r'AndroidView\(\s*modifier = Modifier\.fillMaxWidth\(\)\.weight\(1f\),\s*factory = \{ ctx ->.*?\n\s*\)\s*\}\s*\)', re.DOTALL)
    if pattern.search(content):
        content = pattern.sub(replacement, content)
        with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "w") as f:
            f.write(content)
        print("Patched with regex successfully")
    else:
        print("Regex not found")

