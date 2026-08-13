import re

with open('app/src/main/java/com/example/ui/StudentScreens.kt', 'r', encoding='utf-8') as f:
    content = f.read()

original = """@Composable
fun StudentAiAssistHistoricalHub(viewModel: MainViewModel) {
    val history by viewModel.aiRecordHistory.collectAsState()
    val classConfig by viewModel.aiClassConfig.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF00ACC1))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("当前班级专属 AI 指导规范说明", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006064))
                }
                Spacer(modifier = Modifier.height(4.dp))
                classConfig?.let {
                    Text("• AI 提示支持度：${it.aiHintLevel}模式", fontSize = 14.sp, color = Color(0xFF333333))
                    Text("• 创意向单日获取最大调用上限：${it.creativeGuideDailyLimit} 次", fontSize = 14.sp, color = Color(0xFF333333))
                    Text("• 是否阻断直抄完整源码：${if (it.codeGenerationLimit == 0) "全面阻断抄袭 (纯指导模式)" else "允许部分参考"}", fontSize = 14.sp, color = Color(0xFF333333))
                } ?: run {
                    Text("• AI 提示支持度：默认入门模式", fontSize = 14.sp, color = Color(0xFF333333))
                    Text("• 创意向单日获取最大调用上限：5 次", fontSize = 14.sp, color = Color(0xFF333333))
                    Text("• 是否阻断直抄完整源码：全面阻断抄袭 (纯指导模式)", fontSize = 14.sp, color = Color(0xFF333333))
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF3F51B5))
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI 随身指导问答足迹", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
        }

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("你的足迹里还没有问答记录。请快去编程工作区找 AI 提问并分析吧！", color = Color(0xFF666666), fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (record.assistType) {
                                            "语法纠错" -> Color(0xFFFFEBEE)
                                            "创意引导" -> Color(0xFFFCE4EC)
                                            else -> Color(0xFFE0F2F1)
                                        }
                                    )
                                ) {
                                    Text(
                                        text = record.assistType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (record.assistType) {
                                            "语法纠错" -> Color(0xFFC62828)
                                            "创意引导" -> Color(0xFFC2185B)
                                            else -> Color(0xFF004D40)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                val dateStr = java.text.SimpleDateFormat("MM月dd日 HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.callTime))
                                Text(text = dateStr, fontSize = 10.sp, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "请求上下文：${record.requestContent}", fontSize = 11.sp, color = Color.Gray)
                            Divider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFF5F5F5))

                            Text(
                                text = record.aiResult,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = Color(0xFF37474F)
                            )
                        }
                    }
                }
            }
        }
    }
}"""

replacement = """@Composable
fun StudentAiAssistHistoricalHub(viewModel: MainViewModel) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        TabRow(selectedTabIndex = selectedTabIndex, containerColor = Color.White) {
            Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }) {
                Text("指导足迹", modifier = Modifier.padding(16.dp), fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal, color = if (selectedTabIndex == 0) Color(0xFF3F51B5) else Color.Gray)
            }
            Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }) {
                Text("智能问答", modifier = Modifier.padding(16.dp), fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal, color = if (selectedTabIndex == 1) Color(0xFF3F51B5) else Color.Gray)
            }
        }
        
        Box(modifier = Modifier.weight(1f)) {
            if (selectedTabIndex == 0) {
                StudentAiHistoryTab(viewModel)
            } else {
                SmartAgentChatTab(viewModel)
            }
        }
    }
}

@Composable
fun StudentAiHistoryTab(viewModel: MainViewModel) {
    val history by viewModel.aiRecordHistory.collectAsState()
    val classConfig by viewModel.aiClassConfig.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF00ACC1))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("当前班级专属 AI 指导规范说明", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006064))
                }
                Spacer(modifier = Modifier.height(4.dp))
                classConfig?.let {
                    Text("• AI 提示支持度：${it.aiHintLevel}模式", fontSize = 14.sp, color = Color(0xFF333333))
                    Text("• 创意向单日获取最大调用上限：${it.creativeGuideDailyLimit} 次", fontSize = 14.sp, color = Color(0xFF333333))
                    Text("• 是否阻断直抄完整源码：${if (it.codeGenerationLimit == 0) "全面阻断抄袭 (纯指导模式)" else "允许部分参考"}", fontSize = 14.sp, color = Color(0xFF333333))
                } ?: run {
                    Text("• AI 提示支持度：默认入门模式", fontSize = 14.sp, color = Color(0xFF333333))
                    Text("• 创意向单日获取最大调用上限：5 次", fontSize = 14.sp, color = Color(0xFF333333))
                    Text("• 是否阻断直抄完整源码：全面阻断抄袭 (纯指导模式)", fontSize = 14.sp, color = Color(0xFF333333))
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF3F51B5))
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI 随身指导问答足迹", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
        }

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("你的足迹里还没有问答记录。请快去编程工作区找 AI 提问并分析吧！", color = Color(0xFF666666), fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (record.assistType) {
                                            "语法纠错" -> Color(0xFFFFEBEE)
                                            "创意引导" -> Color(0xFFFCE4EC)
                                            else -> Color(0xFFE0F2F1)
                                        }
                                    )
                                ) {
                                    Text(
                                        text = record.assistType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (record.assistType) {
                                            "语法纠错" -> Color(0xFFC62828)
                                            "创意引导" -> Color(0xFFC2185B)
                                            else -> Color(0xFF004D40)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                val dateStr = java.text.SimpleDateFormat("MM月dd日 HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.callTime))
                                Text(text = dateStr, fontSize = 10.sp, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "请求上下文：${record.requestContent}", fontSize = 11.sp, color = Color.Gray)
                            Divider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFF5F5F5))

                            Text(
                                text = record.aiResult,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = Color(0xFF37474F)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartAgentChatTab(viewModel: MainViewModel) {
    var chatMode by remember { mutableStateOf("快速") } // "快速" or "专家"
    var input by remember { mutableStateOf("") }
    val history by viewModel.aiRecordHistory.collectAsState()
    val chatHistory = history.filter { it.assistType == "在线对答" }.sortedBy { it.callTime }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val isLoading by viewModel.aiLoading.collectAsState()

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Mode Selector
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("智能体模式：", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(50))
                    .padding(4.dp)
            ) {
                listOf("快速", "专家").forEachIndexed { index, mode ->
                    val isSelected = chatMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) Color(0xFF3F51B5) else Color.Transparent)
                            .clickable { chatMode = mode }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mode,
                            color = if (isSelected) Color.White else Color(0xFF666666),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Chat History List
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chatHistory) { msg ->
                // User message (right side)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Box(modifier = Modifier
                        .background(Color(0xFFE8EAF6), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp))
                        .padding(12.dp)
                    ) {
                        Text(msg.requestContent, color = Color(0xFF1A237E), fontSize = 14.sp)
                    }
                }
                // AI message (left side)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Box(modifier = Modifier
                        .background(Color.White, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                        .padding(12.dp)
                    ) {
                        Text(msg.aiResult, color = Color(0xFF333333), fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
            if (isLoading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Box(modifier = Modifier
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                        ) {
                            Text("精灵姐姐正在思考...", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        
        // Input Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(Color.White, RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("有关 Scratch 的问题，向 AI 提问...", fontSize = 14.sp) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                maxLines = 3
            )
            IconButton(
                onClick = { 
                    if (input.isNotBlank() && !isLoading) {
                        viewModel.callAiCustomQuestion(input, chatMode) { _ -> }
                        input = ""
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, contentDescription = "发送", tint = Color(0xFF3F51B5))
                }
            }
        }
    }
}
"""

if original in content:
    content = content.replace(original, replacement)
    with open('app/src/main/java/com/example/ui/StudentScreens.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Patched successfully!")
else:
    print("Original string not found!")
