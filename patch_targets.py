with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "r") as f:
    content = f.read()

import re

pattern = re.compile(r'Column\(\s*modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.weight\(1f\)\s*\.clip\(RoundedCornerShape\(8\.dp\)\)\s*\.background\(Color\.White\)\s*\)\s*\{.*?\}\s*\}\s*"代码视图"', re.DOTALL)

replacement = """Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                            ) {
                                val targetBlocksList = remember(detailWork.workCode) {
                                    val list = mutableListOf<Pair<String, List<String>>>()
                                    try {
                                        val json = org.json.JSONObject(detailWork.workCode)
                                        if (json.has("targets")) {
                                            val targets = json.getJSONArray("targets")
                                            for (i in 0 until targets.length()) {
                                                val target = targets.getJSONObject(i)
                                                val name = target.optString("name", "角色 ${i+1}")
                                                val isStage = target.optBoolean("isStage", false)
                                                val displayName = if (isStage) "🖼️ 舞台 ($name)" else "🐱 角色 ($name)"
                                                val opcodes = mutableListOf<String>()
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
                                                if (opcodes.isNotEmpty() || i == 0) {
                                                    list.add(Pair(displayName, opcodes))
                                                }
                                            }
                                        } else if (json.has("blocks")) {
                                            val opcodes = mutableListOf<String>()
                                            val blocksObj = json.getJSONObject("blocks")
                                            blocksObj.keys().forEach { key ->
                                                val block = blocksObj.optJSONObject(key)
                                                val opcode = block?.optString("opcode")
                                                if (!opcode.isNullOrEmpty()) {
                                                    opcodes.add(opcode)
                                                }
                                            }
                                            list.add(Pair("🐱 角色 1", opcodes))
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    list
                                }

                                androidx.compose.foundation.lazy.LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    contentPadding = PaddingValues(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    targetBlocksList.forEach { (targetName, opcodes) ->
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 16.dp, bottom = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = targetName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF333333),
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp))
                                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${opcodes.size} 个积木",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF1976D2),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                                        }
                                        
                                        if (opcodes.isEmpty()) {
                                            item {
                                                Text("无积木程序", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                                            }
                                        } else {
                                            items(opcodes) { opcode ->
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

