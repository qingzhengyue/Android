with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

target = """                                // 操作按钮在右侧
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.loadReportForWork(work.workId)
                                            showReportDialog = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                                    ) {
                                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF1E88E5))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("看评价", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.loadWorkToWorkspace(work)
                                            onGoToCode?.invoke()
                                            Toast.makeText(context, "已载入《${work.workName}》！为您切换至 Scratch 工作区 ✨", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("载入作品 🧩", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }"""

replacement = """                                // 操作按钮在右侧，使用 MD3 现代设计语言
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            viewModel.loadReportForWork(work.workId)
                                            showReportDialog = true
                                        },
                                        modifier = Modifier.height(40.dp).padding(end = 12.dp),
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Rounded.Analytics,
                                            contentDescription = "看评价",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "看评价",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.loadWorkToWorkspace(work)
                                            onGoToCode?.invoke()
                                            Toast.makeText(context, "已载入《${work.workName}》！为您切换至 Scratch 工作区 ✨", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.height(40.dp),
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Rounded.Extension,
                                            contentDescription = "载入作品",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "载入作品",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
        f.write(content)
    print("Replaced button row successfully.")
else:
    print("Target not found.")

