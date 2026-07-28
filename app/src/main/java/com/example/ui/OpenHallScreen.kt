package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ScratchWork
import com.example.data.WorkComment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenHallScreen(
    viewModel: MainViewModel,
    onNavigateToEditor: () -> Unit
) {
    val context = LocalContext.current
    val publicWorks by viewModel.publicWorksList.collectAsStateWithLifecycle()
    val popularWorks by viewModel.popularWorksList.collectAsStateWithLifecycle()
    val myWorks by viewModel.worksList.collectAsStateWithLifecycle()
    val likedWorkIds by viewModel.likedWorkIds.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0=最新作品, 1=热门推荐, 2=我的作品发布管理
    var activeWorkForComment by remember { mutableStateOf<ScratchWork?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🌟 Scratch 少儿开源社区大厅",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E88E5)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF3F8FF)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC))
        ) {
            // 分类 Tab 切换
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF1E88E5)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("🚀 最新开源", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🔥 热门高赞", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("📂 我的发布状态", fontWeight = FontWeight.SemiBold) }
                )
            }

            val currentDisplayList = when (selectedTab) {
                0 -> publicWorks
                1 -> popularWorks
                else -> myWorks
            }

            if (currentDisplayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTab == 2) "你还没有提交作品或将作品公开到大厅哦~" else "大厅暂无公开作品，快去创作并发布第一个作品吧！",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(currentDisplayList, key = { it.workId }) { work ->
                        val isLikedByMe = likedWorkIds.contains(work.workId)
                        OpenWorkCard(
                            work = work,
                            isLikedByMe = isLikedByMe,
                            isMyWorkTab = (selectedTab == 2),
                            onFork = {
                                viewModel.forkWork(work) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    if (success) {
                                        onNavigateToEditor()
                                    }
                                }
                            },
                            onToggleLike = {
                                viewModel.toggleLikeWork(work.workId) { isLiked, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onTogglePublic = { isPub ->
                                viewModel.toggleWorkPublic(work.workId, isPub)
                                Toast.makeText(context, if (isPub) "已公开发布到大厅！" else "已从大厅取消公开", Toast.LENGTH_SHORT).show()
                            },
                            onOpenComments = {
                                activeWorkForComment = work
                            }
                        )
                    }
                }
            }
        }
    }

    // 弹窗：同伴互动评论对话框 (带 AI 实时风控检测 Task 3)
    activeWorkForComment?.let { work ->
        WorkCommentsBottomSheet(
            work = work,
            viewModel = viewModel,
            onDismiss = { activeWorkForComment = null }
        )
    }
}

@Composable
fun OpenWorkCard(
    work: ScratchWork,
    isLikedByMe: Boolean,
    isMyWorkTab: Boolean,
    onFork: () -> Unit,
    onToggleLike: () -> Unit,
    onTogglePublic: (Boolean) -> Unit,
    onOpenComments: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 头部：作品名称与来源标记
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = work.workName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "学生 ID: ${work.studentId}",
                                fontSize = 11.sp,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (work.forkFromId != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "✨ Fork 二次开发",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (isMyWorkTab) {
                    Switch(
                        checked = work.isPublic,
                        onCheckedChange = { onTogglePublic(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 语义主题风格封面画廊
            val coverConfig = getSemanticCoverConfig(work.workName, work.workId)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(coverConfig.backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = coverConfig.icon,
                        contentDescription = null,
                        tint = coverConfig.primaryColor,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = work.workName,
                            color = coverConfig.primaryColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Scratch 3.0 逻辑作品",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 底部交互按钮组
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 防抖且支持单账号唯一点赞/取消点赞按钮组件
                    LikeButton(
                        isLikedByMe = isLikedByMe,
                        likeCount = work.likesCount,
                        onToggleLike = onToggleLike
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(onClick = onOpenComments) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "评论",
                            tint = Color(0xFF3B82F6)
                        )
                    }
                    Text(
                        text = "互动评论",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }

                if (!isMyWorkTab) {
                    Button(
                        onClick = onFork,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AltRoute,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fork 克隆源码", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * 任务 5：表现层 (Jetpack Compose)
 * 独立可复用的 LikeButton 组件，内置 500ms 防抖机制与点赞状态呈现
 */
@Composable
fun LikeButton(
    isLikedByMe: Boolean, // 当前账号是否已点赞
    likeCount: Int,       // 总点赞数
    onToggleLike: () -> Unit // 触发点赞/取消点赞事件
) {
    // 使用 rememberCoroutineScope 防抖
    val scope = rememberCoroutineScope()
    var isClickable by remember { mutableStateOf(true) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(enabled = isClickable) {
                if (isClickable) {
                    isClickable = false // 暂时禁用点击
                    onToggleLike()
                    
                    // 延迟 500ms 后恢复可点击状态（防抖）
                    scope.launch {
                        delay(500)
                        isClickable = true
                    }
                }
            }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = if (isLikedByMe) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "点赞",
            // 已点赞显示醒目的红色，未点赞显示默认灰色
            tint = if (isLikedByMe) Color(0xFFEF4444) else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = likeCount.toString(),
            color = if (isLikedByMe) Color(0xFFEF4444) else Color.Gray,
            fontWeight = if (isLikedByMe) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

@Composable
fun WorkCommentsBottomSheet(
    work: ScratchWork,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val comments by viewModel.getCommentsForWork(work.workId).collectAsStateWithLifecycle(initialValue = emptyList())
    var newCommentText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        title = {
            Text(
                text = "💬 [${work.workName}] 同伴互动评论",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                if (comments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无留言，快来留下第一个鼓励评语吧！", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(comments) { comment ->
                            CommentItemRow(comment)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 输入评语与 AI 风控审核拦截 (Task 3)
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("写下你的鼓励与建议 (AI 实时安全审查)...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isSubmitting
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (newCommentText.isBlank()) return@Button
                        isSubmitting = true
                        viewModel.submitComment(work.workId, newCommentText) { success, msg ->
                            isSubmitting = false
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) {
                                newCommentText = ""
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = !isSubmitting && newCommentText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("发表评论 (AI风控预审)", fontSize = 12.sp)
                    }
                }
            }
        }
    )
}

@Composable
fun CommentItemRow(comment: WorkComment) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.authorName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB)
                )
                Text(
                    text = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(comment.createTime)),
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.content,
                fontSize = 13.sp,
                color = Color(0xFF334155)
            )
        }
    }
}

data class CoverConfig(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val primaryColor: Color,
    val backgroundColor: Color
)

fun getSemanticCoverConfig(workName: String, workId: Int): CoverConfig {
    val titleLower = workName.lowercase()
    return when {
        titleLower.contains("猫") || titleLower.contains("cat") || titleLower.contains("狗") || titleLower.contains("宠") -> 
            CoverConfig(Icons.Default.Pets, Color(0xFFFF6B6B), Color(0xFFFFE3E3))
            
        titleLower.contains("星") || titleLower.contains("太空") || titleLower.contains("迷宫") || titleLower.contains("space") || titleLower.contains("rocket") -> 
            CoverConfig(Icons.Default.RocketLaunch, Color(0xFF4D96FF), Color(0xFFE3F2FD))
            
        titleLower.contains("游戏") || titleLower.contains("球") || titleLower.contains("跑") || titleLower.contains("game") -> 
            CoverConfig(Icons.Default.SportsEsports, Color(0xFF6BCB77), Color(0xFFE8F5E9))
            
        titleLower.contains("音") || titleLower.contains("乐") || titleLower.contains("舞") || titleLower.contains("歌") || titleLower.contains("music") -> 
            CoverConfig(Icons.Default.MusicNote, Color(0xFFFFD93D), Color(0xFFFFFDE7))
            
        titleLower.contains("画") || titleLower.contains("艺") || titleLower.contains("色") || titleLower.contains("draw") -> 
            CoverConfig(Icons.Default.Palette, Color(0xFF9B51E0), Color(0xFFF3E5F5))
            
        else -> {
            val fallbackThemes = listOf(
                CoverConfig(Icons.Default.Code, Color(0xFF0EA5E9), Color(0xFFF0F9FF)),
                CoverConfig(Icons.Default.AutoAwesome, Color(0xFF8B5CF6), Color(0xFFF5F3FF)),
                CoverConfig(Icons.Default.Psychology, Color(0xFF10B981), Color(0xFFECFDF5)),
                CoverConfig(Icons.Default.Widgets, Color(0xFFF59E0B), Color(0xFFFFFBEB))
            )
            fallbackThemes[kotlin.math.abs(workId) % fallbackThemes.size]
        }
    }
}
