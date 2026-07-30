import re

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

target = """                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = work.workName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333)
                                )

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                                ) {
                                    Text(
                                        text = "提报 ${work.submitCount} 次",
                                        fontSize = 10.sp,
                                        color = Color(0xFF1E88E5),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // --- 教师审查评语及评分显示区 ---
                            if (work.reviewStatus == "已打分" || work.reviewStatus == "打回重做") {
                                val isRedo = work.reviewStatus == "打回重做"
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isRedo) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                                    ),
                                    border = BorderStroke(1.dp, if (isRedo) Color(0xFFEF9A9A) else Color(0xFFA5D6A7))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isRedo) Icons.Default.Warning else Icons.Default.EmojiEvents,
                                                    contentDescription = null,
                                                    tint = if (isRedo) Color(0xFFD32F2F) else Color(0xFF388E3C),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isRedo) "⚠️ 老师评定：不合格，要再改改哦" else "🏆 老师评阅：通过并打分",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = if (isRedo) Color(0xFFC62828) else Color(0xFF2E7D32)
                                                )
                                            }
                                            if (!isRedo) {
                                                Text(
                                                    text = "得分: ${work.teacherScore ?: 0} 分",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "老师赠言：${work.teacherComment ?: "孩子完成得很棒，继续坚持！"}",
                                            fontSize = 12.sp,
                                            color = Color.DarkGray,
                                            lineHeight = 16.sp
                                        )

                                        if (isRedo) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    viewModel.loadWorkToWorkspace(work)
                                                    Toast.makeText(context, "已载入此版本代码！请在 Scratch 中调整修改，重新提交哦！", Toast.LENGTH_LONG).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(30.dp).align(Alignment.End)
                                            ) {
                                                Text("一键载入重新修改 🛠️", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(work.submitTime))
                                    Text(text = "提交：$dateStr", fontSize = 12.sp, color = Color.Gray)
                                }

                                Row(
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
                                        androidx.compose.material.icons.Icons.Default.PlayArrow
                                        Icon(androidx.compose.material.icons.Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
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
                                        androidx.compose.material.icons.Icons.Default.Info
                                        Icon(androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("看 AI 评测报告", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }"""

replacement = """                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. 头部区域
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = work.workName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333)
                                )

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                                ) {
                                    Text(
                                        text = "提报 ${work.submitCount} 次",
                                        fontSize = 11.sp,
                                        color = Color(0xFF1E88E5),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // 2. 教师评语区（内容区）
                            if (work.reviewStatus == "已打分" || work.reviewStatus == "打回重做") {
                                val isRedo = work.reviewStatus == "打回重做"
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isRedo) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                                    ),
                                    border = BorderStroke(1.dp, if (isRedo) Color(0xFFEF9A9A) else Color(0xFFA5D6A7))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isRedo) Icons.Default.Warning else Icons.Default.EmojiEvents,
                                                    contentDescription = null,
                                                    tint = if (isRedo) Color(0xFFD32F2F) else Color(0xFF388E3C),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isRedo) "⚠️ 老师评定：不合格" else "🏆 老师评定：通过",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isRedo) Color(0xFFC62828) else Color(0xFF2E7D32)
                                                )
                                            }
                                            if (!isRedo) {
                                                Text(
                                                    text = "得分: ${work.teacherScore ?: 0} 分",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "赠言：${work.teacherComment ?: "孩子完成得很棒，继续坚持！"}",
                                            fontSize = 13.sp,
                                            color = Color.DarkGray,
                                            lineHeight = 18.sp
                                        )
                                        
                                        if (isRedo) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    viewModel.loadWorkToWorkspace(work)
                                                    Toast.makeText(context, "已载入此版本代码！请在 Scratch 中调整修改，重新提交哦！", Toast.LENGTH_LONG).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Text("一键载入重新修改 🛠️", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // 分割线
                            HorizontalDivider(color = Color(0xFFF0F0F0))

                            // 3. 底部独立操作区 (Action Area)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 提交日期在左侧
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(work.submitTime))
                                    Text(text = "提交：$dateStr", fontSize = 11.sp, color = Color.Gray)
                                }

                                // 操作按钮在右侧
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
                                }
                            }
                        }
                    }"""

idx = content.find("val isRedo = work.reviewStatus == ")
if idx == -1:
    print("WARNING: couldn't find exact text. Doing exact replace...")

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
        f.write(content)
    print("Patched StudentScreens.kt Card layout!")
else:
    print("Target block not found in StudentScreens.kt")

