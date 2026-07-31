with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "r") as f:
    content = f.read()

import re

# Match the AndroidView inside the "积木视图" -> { ... Column(...) { ... } } block
# Specifically, we find AndroidView after HorizontalDivider
pattern = re.compile(r'HorizontalDivider\(color = Color\(0xFFEEEEEE\), thickness = 1\.dp\)\s*\n\s*AndroidView\([\s\S]*?\}\s*\)\s*\}\s*\}\s*"代码视图"', re.MULTILINE)

replacement = """HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                                val blockOpcodes = remember(detailWork.workCode) {
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
                                    androidx.compose.foundation.lazy.items(blockOpcodes) { opcode ->
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
                                }
                            }
                        }
                        "代码视图" """

if pattern.search(content):
    content = pattern.sub(replacement, content)
    with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "w") as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Not found")

