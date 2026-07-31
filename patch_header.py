with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "r") as f:
    content = f.read()

target = """                        "积木视图" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                            ) {
                                AndroidView(
                                    factory = { ctx ->"""

replacement = """                        "积木视图" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                            ) {
                                // 优雅的标题头部
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🐱 角色 1 (Sprite1) 积木程序",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF333333),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    val blockCount = remember(detailWork.workCode) {
                                        try {
                                            val json = org.json.JSONObject(detailWork.workCode)
                                            var count = 0
                                            if (json.has("targets")) {
                                                val targets = json.getJSONArray("targets")
                                                for (i in 0 until targets.length()) {
                                                    val target = targets.getJSONObject(i)
                                                    if (target.has("blocks")) {
                                                        count += target.getJSONObject("blocks").length()
                                                    }
                                                }
                                            } else if (json.has("blocks")) {
                                                count = json.getJSONObject("blocks").length()
                                            }
                                            count
                                        } catch (e: Exception) {
                                            0
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$blockCount 个积木",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1976D2),
                                            maxLines = 1
                                        )
                                    }
                                }
                                
                                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                                AndroidView(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    factory = { ctx ->"""

content = content.replace(target, replacement)

target2 = """                                            addJavascriptInterface(jsInterface, "AndroidProjectLoader")
                                            loadUrl("file:///android_asset/scratch_blocks_viewer.html")
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }"""

replacement2 = """                                            addJavascriptInterface(jsInterface, "AndroidProjectLoader")
                                            loadUrl("file:///android_asset/scratch_blocks_viewer.html")
                                        }
                                    }
                                )
                            }"""

content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "w") as f:
    f.write(content)
