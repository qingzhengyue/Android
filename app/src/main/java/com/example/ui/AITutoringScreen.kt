package com.example.ui

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

data class SessionGroup(
    val sessionId: String,
    val records: List<AiAssistRecord>,
    val firstRecord: AiAssistRecord,
    val latestTime: Long,
    val isScratch: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITutoringScreen(viewModel: MainViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var currentMode by remember { mutableStateOf("快速") }

    // 数据库全量历史记录 (Flow)
    val history by viewModel.aiRecordHistory.collectAsState()
    val isLoading by viewModel.aiLoading.collectAsState()
    val activeSessionId by viewModel.activeAiSessionId.collectAsState()

    // 1. 侧边栏选中的特定对话 Session ID (null 表示当前正在进行的新对话)
    var selectedSessionId by remember { mutableStateOf<String?>(null) }
    
    // 2. 侧边栏的分类筛选状态 (全部 / 🧠 AI辅导 / 🧩 Scratch精灵)
    var drawerFilter by remember { mutableStateOf(DrawerFilter.ALL) }

    // 3. 待发送的图片 Bitmap
    var attachedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // 相册选择器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                attachedImageBitmap = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "读取相册图片失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 相机拍摄 launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            attachedImageBitmap = bitmap
        } else {
            Toast.makeText(context, "未拍摄照片", Toast.LENGTH_SHORT).show()
        }
    }

    // 相机权限申请 launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "启动硬件相机失败，已为你切换至相册选图", Toast.LENGTH_SHORT).show()
                galleryLauncher.launch("image/*")
            }
        } else {
            Toast.makeText(context, "未获得相机权限，已为你引导至相册选图", Toast.LENGTH_SHORT).show()
            galleryLauncher.launch("image/*")
        }
    }

    // 按 session 隔离归类侧边栏显示的记录，避免同一对话内重复多条新记录
    val sessionGroups = remember(history, drawerFilter) {
        val filtered = when (drawerFilter) {
            DrawerFilter.ALL -> history
            DrawerFilter.AI_TUTOR -> history.filter { it.assistType == "在线对答" || it.assistType == "AI 辅导" }
            DrawerFilter.SCRATCH -> history.filter { it.assistType != "在线对答" && it.assistType != "AI 辅导" }
        }

        filtered.groupBy { record ->
            if (record.sessionId.isNotBlank()) record.sessionId else "legacy_${record.callId}"
        }.map { (sid, recordsInGroup) ->
            val sorted = recordsInGroup.sortedBy { it.callTime }
            SessionGroup(
                sessionId = sid,
                records = sorted,
                firstRecord = sorted.first(),
                latestTime = sorted.last().callTime,
                isScratch = sorted.first().assistType != "在线对答" && sorted.first().assistType != "AI 辅导"
            )
        }.sortedByDescending { it.latestTime }
    }

    // 当前主区域展示的对话记录
    val currentTargetSid = selectedSessionId ?: activeSessionId
    val displayRecords = remember(history, currentTargetSid) {
        history.filter { record ->
            val recordSid = if (record.sessionId.isNotBlank()) record.sessionId else "legacy_${record.callId}"
            recordSid == currentTargetSid
        }.sortedBy { it.callTime }
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
                        selectedSessionId = null
                        viewModel.startNewAiSession()
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

                // 功能 2 & 3：按 Session 聚合列出历史对话列表
                if (sessionGroups.isEmpty()) {
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
                        items(sessionGroups, key = { it.sessionId }) { group ->
                            val isSelected = group.sessionId == currentTargetSid

                            DrawerHistoryListItem(
                                group = group,
                                isSelected = isSelected,
                                onClick = {
                                    selectedSessionId = group.sessionId
                                    viewModel.setActiveAiSessionId(group.sessionId)
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
                        if (selectedSessionId != null) {
                            val activeGroup = sessionGroups.find { it.sessionId == selectedSessionId }
                            val isScratch = activeGroup?.isScratch == true
                            Surface(
                                color = if (isScratch) Color(0xFFFEF3C7) else Color(0xFFEEF2FF),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.clickable {
                                    selectedSessionId = null
                                    viewModel.startNewAiSession()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isScratch) "🧩 Scratch 历史答疑" else "🧠 AI 辅导会话",
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
                        if (selectedSessionId != null) {
                            IconButton(onClick = {
                                selectedSessionId = null
                                viewModel.startNewAiSession()
                            }) {
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
                        var sendPrompt = text.trim()
                        if (attachedImageBitmap != null) {
                            sendPrompt = if (sendPrompt.isBlank()) "请智能识别并解答图片中的少儿 Scratch 题目或脚本逻辑" else "[拍照图文识图解析] $sendPrompt"
                            attachedImageBitmap = null
                        }
                        if (sendPrompt.isNotBlank() && !isLoading) {
                            val targetSid = selectedSessionId ?: activeSessionId
                            viewModel.callAiCustomQuestion(sendPrompt, currentMode, targetSessionId = targetSid) { _ -> }
                        }
                    },
                    onCameraClick = {
                        val hasCameraPerm = ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasCameraPerm) {
                            try {
                                cameraLauncher.launch(null)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "未启动硬件相机，已为你切至相册导入", Toast.LENGTH_SHORT).show()
                                galleryLauncher.launch("image/*")
                            }
                        } else {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    onGalleryClick = {
                        galleryLauncher.launch("image/*")
                    },
                    attachedImageBitmap = attachedImageBitmap,
                    onClearImage = { attachedImageBitmap = null },
                    isLoading = isLoading
                )
            },
            containerColor = SurfaceBg
        ) { innerPadding ->
            if (displayRecords.isEmpty() && !isLoading) {
                ElegantEmptyState(
                    onPromptClick = { prompt ->
                        val targetSid = selectedSessionId ?: activeSessionId
                        viewModel.callAiCustomQuestion(prompt, currentMode, targetSessionId = targetSid) { _ -> }
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
 * 1. 侧边栏：历史会话列表项 (按 Session Group 归类，Macaron 色系大圆角卡片)
 */
@Composable
fun DrawerHistoryListItem(
    group: SessionGroup,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isScratchModule = group.isScratch
    val record = group.firstRecord
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
                        text = "${group.records.size}条对话",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(text = "•", fontSize = 10.sp, color = Color(0xFFD1D5DB))
                    Text(
                        text = formatTime(group.latestTime),
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
    attachedImageBitmap: Bitmap? = null,
    onClearImage: () -> Unit = {},
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
            Column(modifier = Modifier.fillMaxWidth()) {
                // 待解解答题目图片缩略图区域
                if (attachedImageBitmap != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(54.dp)) {
                            Image(
                                bitmap = attachedImageBitmap.asImageBitmap(),
                                contentDescription = "待分析题目图片",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, PrimaryIndigo.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = onClearImage,
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .background(Color.Red, CircleShape)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "删除图片",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "📷 已附加题目/脚本图片",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigo
                            )
                            Text(
                                text = "发送后，精灵姐姐将通过识图算法精准提炼积木逻辑",
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }

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
                            tint = if (attachedImageBitmap != null) PrimaryIndigo else Color(0xFF6B7280)
                        )
                    }
                    IconButton(onClick = onGalleryClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = "相册",
                            tint = if (attachedImageBitmap != null) PrimaryIndigo else Color(0xFF6B7280)
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
                                        text = if (attachedImageBitmap != null) "可补充图片提问（或直接点击发送）..." else "向 AI 提问 Scratch 难题...",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val canSend = (inputText.isNotBlank() || attachedImageBitmap != null) && !isLoading
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
