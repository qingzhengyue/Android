with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "r") as f:
    content = f.read()

target = """                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. 复制代码按钮（主操作，均分宽度）
                        Button(
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Scratch Work Code", formattedJson)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "代码已复制到剪贴板！", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "复制失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("复制代码", fontSize = 15.sp, maxLines = 1)
                        }

                        // 2. 关闭按钮（次操作，均分宽度）
                        OutlinedButton(
                            onClick = { viewingWorkDetail = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF4B5563))
                        ) {
                            Text("关闭", color = Color(0xFFD1D5DB), fontSize = 15.sp, maxLines = 1)
                        }
                    }"""

replacement = """                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (viewingDetailTab == "代码视图") {
                            // 1. 复制代码按钮（主操作，均分宽度）
                            Button(
                                onClick = {
                                    try {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Scratch Work Code", formattedJson)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "代码已复制到剪贴板！", Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "复制失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("复制代码", fontSize = 15.sp, maxLines = 1)
                            }
                        }

                        // 2. 关闭按钮（次操作，均分宽度）
                        OutlinedButton(
                            onClick = { viewingWorkDetail = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF4B5563))
                        ) {
                            Text("关闭", color = Color(0xFFD1D5DB), fontSize = 15.sp, maxLines = 1)
                        }
                    }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "w") as f:
        f.write(content)
    print("Patched TeacherWorksScreens.kt")
else:
    print("Could not find target block in TeacherWorksScreens.kt")
