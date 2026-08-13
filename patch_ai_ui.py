import os

code = """package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

val PrimaryIndigo = Color(0xFF3F51B5)
val SurfaceBg = Color(0xFFF9FAFB)
val SurfaceVariantGray = Color(0xFFF0F2F5)
val AiBubbleBg = Color(0xFFFFFFFF)
val UserBubbleBg = PrimaryIndigo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITutoringScreen(viewModel: MainViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentMode by remember { mutableStateOf("快速") }

    val history by viewModel.aiRecordHistory.collectAsState()
    val chatHistory = history.filter { it.assistType == "在线对答" }.sortedBy { it.callTime }
    val isLoading by viewModel.aiLoading.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = Color.White
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                ExtendedFloatingActionButton(
                    onClick = { scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Rounded.AddComment, contentDescription = null) },
                    text = { Text("新建 AI 辅导对话", fontWeight = FontWeight.Bold) },
                    containerColor = PrimaryIndigo,
                    contentColor = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFE5E7EB))

                Text(
                    text = "历史问答足迹",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(history) { record ->
                        NavigationDrawerItem(
                            label = { Text(record.requestContent, maxLines = 1, fontSize = 14.sp) },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() } },
                            icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = null) },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = PrimaryIndigo.copy(alpha = 0.08f),
                                selectedTextColor = PrimaryIndigo,
                                selectedIconColor = PrimaryIndigo
                            )
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        TopModeSwitcher(
                            currentMode = currentMode,
                            onModeChanged = { currentMode = it }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "打开历史记录")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = SurfaceBg
                    )
                )
            },
            bottomBar = {
                MultiModalBottomBar(
                    onSend = { text -> 
                        if (text.isNotBlank() && !isLoading) {
                            viewModel.callAiCustomQuestion(text, currentMode) {}
                        }
                    },
                    onCameraClick = {  },
                    onGalleryClick = {  },
                    isLoading = isLoading
                )
            },
            containerColor = SurfaceBg
        ) { innerPadding ->
            ChatFlowContent(
                modifier = Modifier.padding(innerPadding),
                chatHistory = chatHistory,
                isLoading = isLoading
            )
        }
    }
}

@Composable
fun TopModeSwitcher(currentMode: String, onModeChanged: (String) -> Unit) {
    val modes = listOf("⚡️快速", "🎓专家")
    val selectedIndex = if (currentMode == "专家") 1 else 0

    val indicatorOffset by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessLow
        ),
        label = "mode_switch_anim"
    )

    Box(
        modifier = Modifier
            .width(180.dp)
            .height(44.dp)
            .background(SurfaceVariantGray, RoundedCornerShape(50))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .offset(x = (86.dp) * indicatorOffset)
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(50))
                .background(Color.White, RoundedCornerShape(50))
        )
        
        Row(modifier = Modifier.fillMaxSize()) {
            modes.forEachIndexed { index, text ->
                val isSelected = selectedIndex == index
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF111827) else Color(0xFF6B7280),
                    label = "text_color_anim"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onModeChanged(if (index == 0) "快速" else "专家") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun MultiModalBottomBar(
    onSend: (String) -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    isLoading: Boolean
) {
    var inputText by remember { mutableStateOf("") }
    
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding() 
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom 
        ) {
            IconButton(onClick = onCameraClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "拍照", tint = Color(0xFF4B5563))
            }
            IconButton(onClick = onGalleryClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Image, contentDescription = "相册", tint = Color(0xFF4B5563))
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(SurfaceVariantGray, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .animateContentSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = Color(0xFF1F2937),
                        lineHeight = 22.sp
                    ),
                    cursorBrush = SolidColor(PrimaryIndigo),
                    maxLines = 4,
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text(
                                text = "向 AI 提问...", 
                                color = Color(0xFF9CA3AF), 
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val canSend = inputText.isNotBlank() && !isLoading
            val buttonColor by animateColorAsState(
                targetValue = if (canSend) PrimaryIndigo else Color(0xFFE5E7EB),
                label = "send_btn_anim"
            )
            
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(buttonColor)
                    .clickable(enabled = canSend) { 
                        onSend(inputText)
                        inputText = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward, 
                        contentDescription = "发送", 
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatFlowContent(modifier: Modifier = Modifier, chatHistory: List<com.example.data.AiAssistRecord>, isLoading: Boolean) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(chatHistory) { msg ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f) 
                        .background(UserBubbleBg, RoundedCornerShape(24.dp, 24.dp, 4.dp, 24.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = msg.requestContent, 
                        color = Color.White, 
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(AiBubbleBg, RoundedCornerShape(24.dp, 24.dp, 24.dp, 4.dp))
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp, 24.dp, 24.dp, 4.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = msg.aiResult, 
                        color = Color(0xFF1F2937), 
                        fontSize = 15.sp,
                        lineHeight = 24.sp
                    )
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
}
"""

with open('app/src/main/java/com/example/ui/AITutoringScreen.kt', 'w', encoding='utf-8') as f:
    f.write(code)

with open('app/src/main/java/com/example/ui/AppNavigation.kt', 'r', encoding='utf-8') as f:
    nav_code = f.read()

nav_code = nav_code.replace('StudentAiAssistHistoricalHub(viewModel = viewModel)', 'AITutoringScreen(viewModel = viewModel)')

with open('app/src/main/java/com/example/ui/AppNavigation.kt', 'w', encoding='utf-8') as f:
    f.write(nav_code)
