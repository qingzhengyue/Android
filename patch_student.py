with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

target = """                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.loadWorkToWorkspace(work)
                                            onGoToCode?.invoke()
                                            Toast.makeText(context, "已载入《${work.workName}》！为您切换至 Scratch 工作区 ✨", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("载入作品积木 🧩", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.loadReportForWork(work.workId)
                                            showReportDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("看 AI 评测报告 👀", fontSize = 11.sp, color = Color.White)
                                    }
                                }"""

replacement = """                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.loadWorkToWorkspace(work)
                                            onGoToCode?.invoke()
                                            Toast.makeText(context, "已载入《${work.workName}》！为您切换至 Scratch 工作区 ✨", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                        contentPadding = PaddingValues(vertical = 12.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(46.dp)
                                    ) {
                                        androidx.compose.material.icons.Icons
                                        androidx.compose.material.icons.filled.Code
                                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("载入作品积木", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.loadReportForWork(work.workId)
                                            showReportDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        contentPadding = PaddingValues(vertical = 12.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(46.dp)
                                    ) {
                                        androidx.compose.material.icons.filled.Analytics
                                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("看 AI 评测报告", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
        f.write(content)
    print("Patched StudentScreens.kt")
else:
    print("Target block not found in StudentScreens.kt")
