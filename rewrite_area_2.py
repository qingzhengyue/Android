import re

with open("app/src/main/java/com/example/ui/AiComponents.kt", "r") as f:
    content = f.read()

start_marker = "// Area 2: 中间内容展示区 (Modifier.weight(1f) 占满所有剩余空间、支持完整滚动及点击历史记录展开)"
end_marker = "// Area 3: 固定底部输入区"

parts = content.split(start_marker)
if len(parts) < 2:
    print("Start marker not found")
    exit(1)

pre_content = parts[0]
rest = parts[1]

parts2 = rest.split(end_marker)
if len(parts2) < 2:
    print("End marker not found")
    exit(1)

post_content = parts2[1]

new_area_2 = """// Area 2: 中间内容展示区 (Modifier.weight(1f) 占满所有剩余空间)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section: Specific Controls depending on selected tab
                when (activeTab) {
                    "语法纠错" -> {
                        // Switch for Real-time detection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFF8BBD0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFC2185B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("开启实时代码检测", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                            }
                            Switch(
                                checked = realTimeCheckEnabled,
                                onCheckedChange = { onRealTimeCheckChange(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFC2185B),
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color.White
                                ),
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        // Interactive manually trigger button preserved
                        Button(
                            onClick = { getLiveCodeAndCall("语法纠错", "") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2185B)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text("🛑 立即手动语法检测", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    "创意引导" -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF8BBD0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("💡 创意灵感库介绍", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2185B))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "不知道怎么搭积木了？没关系！在下方输入一个你喜欢的主题（如“走迷宫”、“极速赛车”），点击发送，精灵姐姐就会利用魔法，根据你目前的进度送给你三大创意巧思哦！✨",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    "考点讲解" -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF8BBD0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("🎓 知识点锦囊 (快速点击选择)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2185B))
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("循环", "变量", "广播", "坐标").forEach { chip ->
                                        Box(
                                            modifier = Modifier
                                                .height(32.dp)
                                                .background(Color(0xFFFFEEF0), RoundedCornerShape(12.dp))
                                                .border(1.dp, Color(0xFFF8BBD0), RoundedCornerShape(12.dp))
                                                .clickable { 
                                                    kbPromptInput = TextFieldValue(chip)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "🏷️ $chip",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFC2185B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Current diagnostics AI result loader
                val currentTypeShow = when (activeTab) {
                    "语法纠错" -> "语法纠错"
                    "创意引导" -> "创意引导"
                    else -> "知识点讲解"
                }
                if (aiLoading && aiResultType == currentTypeShow) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFF8BBD0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFC2185B), strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            val loadingMsg = if (aiResultType == "创意引导") {
                                "正在分析你的代码，请稍候..."
                            } else {
                                "精灵姐姐正在全力思索中..."
                            }
                            Text(loadingMsg, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                // Area 2.5: 快捷引导芯片 (取代了原来的 Area 2.5)
                PromptChipsRow(
                    prompts = mergedPrompts,
                    onChipClick = { chipText ->
                        val cleanPrompt = chipText.replace(Regex("^[🧩💡🎓❓⚡📦]\\s*"), "")
                        when (activeTab) {
                            "语法纠错" -> customQuestionInput = TextFieldValue(cleanPrompt)
                            "创意引导" -> creativePromptInput = TextFieldValue(cleanPrompt)
                            else -> kbPromptInput = TextFieldValue(cleanPrompt)
                        }
                    }
                )

                // 聊天消息列表 (占用剩余可用空间)
                Box(modifier = Modifier.weight(1f)) {
                    ChatMessageList(messages = chatMessages)
                }
            }

            // Area 3: 固定底部输入区"""

with open("app/src/main/java/com/example/ui/AiComponents.kt", "w") as f:
    f.write(pre_content + new_area_2 + post_content)

print("Rewrote AiComponents.kt successfully")
