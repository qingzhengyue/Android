package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AiAssistRecord
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val PrimaryIndigo = Color(0xFF3F51B5)
val SurfaceBg = Color(0xFFF9FAFB)
val SurfaceVariantGray = Color(0xFFF0F2F5)
val AiBubbleBg = Color(0xFFFFFFFF)
val UserBubbleBg = PrimaryIndigo

enum class DrawerFilter {
    ALL,        // 全部历史
    AI_TUTOR,   // 🧠 AI 辅导问答
    SCRATCH     // 🧩 Scratch 智能编程精灵
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITutoringScreen(viewModel: MainViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentMode by remember { mutableStateOf("快速") }

    // 数据库全量历史记录 (Flow)
    val history by viewModel.aiRecordHistory.collectAsState()
    val isLoading by viewModel.aiLoading.collectAsState()

    // 1. 侧边栏选中的特定对话 ID (null 表示当前正在进行的新对话)
    var selectedCallId by remember { mutableStateOf<Int?>(null) }
    
    // 2. 侧边栏的分类筛选状态 (全部 / 🧠 AI辅导 / 🧩 Scratch精灵)
    var drawerFilter by remember { mutableStateOf(DrawerFilter.ALL) }

    // 3. 当前对话列表状态
    var activeNewSessionRecords by remember { mutableStateOf<List<AiAssistRecord>>(emptyList()) }

    // 选中特定历史记录对象
    val selectedRecord = history.find { it.callId == selectedCallId }

    // 过滤侧边栏显示的记录
    val filteredHistory = remember(history, drawerFilter) {
        when (drawerFilter) {
            DrawerFilter.ALL -> history.sortedByDescending { it.callTime }
            DrawerFilter.AI_TUTOR -> history.filter { it.assistType == "在线对答" || it.assistType == "AI 辅导" }
                .sortedByDescending { it.callTime }
            DrawerFilter.SCRATCH -> history.filter { it.assistType != "在线对答" && it.assistType != "AI 辅导" }
                .sortedByDescending { it.callTime }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = Color.White
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 功能 1：【新建 AI 辅导对话】高级质感按钮
                NewSessionButton(
                    onClick = {
                        selectedCallId = null
                        activeNewSessionRecords = emptyList()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
                    color = Color(0xFFF3F4F6)
                )

                // 侧边栏标题与三路胶囊型分类 Filter
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "全局智能体历史中枢",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // 胶囊 Filter 筛选组件
                    DrawerFilterCapsuleBar(
                        selectedFilter = drawerFilter,
                        onFilterSelected = { drawerFilter = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 功能 2 & 3：【查看以往 AI 辅导对话】与【查看 Scratch 智能编程精灵对话】
                if (filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🍃", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "暂无相关对话记录",
                                color = Color(0xFF9CA3AF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredHistory, key = { it.callId }) { record ->
                            val isSelected = record.callId == selectedCallId

                            DrawerHistoryListItem(
                                record = record,
                                isSelected = isSelected,
                                onClick = {
                                    selectedCallId = record.callId
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        if (selectedRecord != null) {
                            val isScratch = selectedRecord.assistType != "在线对答" && selectedRecord.assistType != "AI 辅导"
                            Surface(
                                color = if (isScratch) Color(0xFFFEF3C7) else Color(0xFFEEF2FF),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.clickable {
                                    selectedCallId = null
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isScratch) "🧩 Scratch 历史答疑" else "🧠 AI 辅导历史",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isScratch) Color(0xFF92400E) else PrimaryIndigo
                                    )
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "返回新对话",
                                        tint = Color(0xFF6B7280),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            TopModeSwitcher(
                                currentMode = currentMode,
                                onModeChanged = { currentMode = it }
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Rounded.Menu,
                                contentDescription = "打开历史记录中枢",
                                tint = PrimaryIndigo
                            )
                        }
                    },
                    actions = {
                        if (selectedCallId != null) {
                            IconButton(onClick = { selectedCallId = null }) {
                                Icon(Icons.Rounded.Add, contentDescription = "新对话", tint = PrimaryIndigo)
                            }
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
                            if (selectedCallId != null) {
                                selectedCallId = null
                            }
                            viewModel.callAiCustomQuestion(text, currentMode) { _ -> }
                        }
                    },
                    onCameraClick = { },
                    onGalleryClick = { },
                    isLoading = isLoading
                )
            },
            containerColor = SurfaceBg
        ) { innerPadding ->
            val displayRecords = remember(selectedRecord, activeNewSessionRecords, history) {
                if (selectedRecord != null) {
                    listOf(selectedRecord)
                } else if (activeNewSessionRecords.isNotEmpty()) {
                    activeNewSessionRecords
                } else {
                    history.filter { it.assistType == "在线对答" || it.assistType == "AI 辅导" }
                        .sortedBy { it.callTime }
                }
            }

            if (displayRecords.isEmpty()) {
                ElegantEmptyState(
                    onPromptClick = { prompt ->
                        viewModel.callAiCustomQuestion(prompt, currentMode) { _ -> }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                ChatFlowContent(
                    modifier = Modifier.padding(innerPadding),
                    chatHistory = displayRecords,
                    isLoading = isLoading
                )
            }
        }
    }
}

/**
 * 1. 侧边栏：高级质感新建按钮
 */
@Composable
fun NewSessionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = PrimaryIndigo.copy(alpha = 0.2f),
                spotColor = PrimaryIndigo.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF3730A3), // Darker Indigo
                            PrimaryIndigo,
                            Color(0xFF6366F1)  // Bright Indigo
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddComment,
                    contentDescription = "新建对话",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "新建 AI 辅导对话",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

/**
 * 1. 侧边栏：胶囊型分类 Filter
 */
@Composable
fun DrawerFilterCapsuleBar(
    selectedFilter: DrawerFilter,
    onFilterSelected: (DrawerFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF3F4F6), RoundedCornerShape(14.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        DrawerFilterChipItem(
            text = "全部",
            emoji = null,
            isSelected = selectedFilter == DrawerFilter.ALL,
            onClick = { onFilterSelected(DrawerFilter.ALL) },
            modifier = Modifier.weight(1f)
        )
        DrawerFilterChipItem(
            text = "AI辅导",
            emoji = "🧠",
            isSelected = selectedFilter == DrawerFilter.AI_TUTOR,
            onClick = { onFilterSelected(DrawerFilter.AI_TUTOR) },
            modifier = Modifier.weight(1f)
        )
        DrawerFilterChipItem(
            text = "Scratch",
            emoji = "🧩",
            isSelected = selectedFilter == DrawerFilter.SCRATCH,
            onClick = { onFilterSelected(DrawerFilter.SCRATCH) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun DrawerFilterChipItem(
    text: String,
    emoji: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "filter_chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryIndigo else Color(0xFF6B7280),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "filter_chip_text"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (emoji != null) {
                Text(text = emoji, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

/**
 * 1. 侧边栏：历史列表项 (Macaron 色系大圆角卡片)
 */
@Composable
fun DrawerHistoryListItem(
    record: AiAssistRecord,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isScratchModule = record.assistType != "在线对答" && record.assistType != "AI 辅导"
    val containerBg by animateColorAsState(
        targetValue = if (isSelected) PrimaryIndigo.copy(alpha = 0.08f) else Color.Transparent,
        label = "item_container_bg"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(14.dp),
        color = containerBg
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 马卡龙色系大圆角 Icon 容器
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isScratchModule) Color(0xFFFEF3C7) else Color(0xFFEEF2FF)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isScratchModule) "🧩" else "🧠",
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.requestContent.ifBlank { "提问记录" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) PrimaryIndigo else Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isScratchModule) record.assistType else "AI 辅导",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isScratchModule) Color(0xFFD97706) else PrimaryIndigo
                    )
                    Text(text = "•", fontSize = 10.sp, color = Color(0xFFD1D5DB))
                    Text(
                        text = formatTime(record.callTime),
                        fontSize = 11.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }
    }
}

/**
 * 2. 艺术化空状态破冰页 (Elegant Empty State)
 */
@Composable
fun ElegantEmptyState(
    onPromptClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color(0xFFF0F2F5))
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部图标柔光卡片
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFEEF2FF),
                                    Color(0xFFE0E7FF)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "AI 智能精灵",
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "开启 Scratch AI 探秘之旅",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "我是你的专属编程小助手。遇到积木报错、逻辑卡壳还是创意设计？随时问我！",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                // 启发式快捷提问胶囊
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SuggestionPromptChip(
                        emoji = "🧩",
                        text = "怎样让角色在 Scratch 里碰到边缘就反弹？",
                        onClick = { onPromptClick("怎样让角色在 Scratch 里碰到边缘就反弹？") }
                    )
                    SuggestionPromptChip(
                        emoji = "💡",
                        text = "克隆体积木怎么用？请给我一个简单的例子",
                        onClick = { onPromptClick("Scratch 中的克隆体积木怎么用？请给我一个简单的例子") }
                    )
                    SuggestionPromptChip(
                        emoji = "⚡️",
                        text = "循环代码卡住了不走怎么排查？",
                        onClick = { onPromptClick("为什么我的重复执行循环代码卡住了不走？如何调试？") }
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestionPromptChip(
    emoji: String,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF9FAFB),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * 3. 顶部药丸模式切换器
 */
@Composable
fun TopModeSwitcher(currentMode: String, onModeChanged: (String) -> Unit) {
    val modes = listOf("⚡️快速", "🎓专家")
    val selectedIndex = if (currentMode == "专家") 1 else 0

    val indicatorOffset by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessLow
        ),
        label = "mode_switch_anim"
    )

    Box(
        modifier = Modifier
            .width(180.dp)
            .height(42.dp)
            .background(SurfaceVariantGray, RoundedCornerShape(50))
            .padding(3.dp)
    ) {
        // 浮动指示器卡片
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .offset(x = (87.dp) * indicatorOffset)
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(50))
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
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}

/**
 * 3. 底部极简精致输入舱 (MultiModal Bottom Bar)
 */
@Composable
fun MultiModalBottomBar(
    onSend: (String) -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    isLoading: Boolean
) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        HorizontalDivider(color = Color(0xFFF0F2F5), thickness = 1.dp)

        Surface(
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(onClick = onCameraClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = "拍照",
                        tint = Color(0xFF6B7280)
                    )
                }
                IconButton(onClick = onGalleryClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = "相册",
                        tint = Color(0xFF6B7280)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceVariantGray, RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontSize = 14.5.sp,
                            color = Color(0xFF1F2937),
                            lineHeight = 21.sp
                        ),
                        cursorBrush = SolidColor(PrimaryIndigo),
                        maxLines = 4,
                        decorationBox = { innerTextField ->
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "向 AI 提问 Scratch 难题...",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 14.sp
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
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(buttonColor)
                        .clickable(enabled = canSend) {
                            onSend(inputText)
                            inputText = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = "发送",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatFlowContent(
    modifier: Modifier = Modifier,
    chatHistory: List<AiAssistRecord>,
    isLoading: Boolean
) {
    val listState = rememberLazyListState()

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(chatHistory) { msg ->
            // 用户提问气泡
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(22.dp, 22.dp, 4.dp, 22.dp))
                        .background(UserBubbleBg, RoundedCornerShape(22.dp, 22.dp, 4.dp, 22.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = msg.requestContent,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            // AI 回复气泡
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 4.dp))
                        .background(AiBubbleBg, RoundedCornerShape(22.dp, 22.dp, 22.dp, 4.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = msg.aiResult,
                        color = Color(0xFF1F2937),
                        fontSize = 14.5.sp,
                        lineHeight = 23.sp
                    )
                }
            }
        }

        if (isLoading) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = PrimaryIndigo,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("精灵姐姐正在思考解答...", color = Color(0xFF6B7280), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timeMillis: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}
