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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

            // 主题精确匹配的开源大厅作品封面
            val coverConfig = remember(work.workId, work.workName) {
                val name = work.workName.lowercase()
                when {
                    // 1. 电子琴 / 音乐 / 音效
                    name.contains("琴") || name.contains("音") || name.contains("歌") || name.contains("music") ->
                        HallThemeCover(
                            icon = Icons.Default.MusicNote,
                            themeTag = "🎵 音乐音效",
                            title = "电子琴与按键音效交互",
                            desc = "键盘映射与声音积木联动组合",
                            iconTint = Color(0xFF7C3AED),
                            iconBg = Color(0xFFF3E8FF),
                            bgGradient = listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF))
                        )
                    // 2. 水果 / 接水果 / 捕获
                    name.contains("水果") || name.contains("果") || name.contains("捕获") || name.contains("fruit") ->
                        HallThemeCover(
                            icon = Icons.Default.Fastfood,
                            themeTag = "🍎 接水果",
                            title = "趣味接水果与碰撞侦测",
                            desc = "掉落物理与按键位移得分机制",
                            iconTint = Color(0xFF16A34A),
                            iconBg = Color(0xFFDCFCE7),
                            bgGradient = listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7))
                        )
                    // 3. 迷宫 / 反弹 / 逃跑
                    name.contains("迷宫") || name.contains("反弹") || name.contains("maze") ->
                        HallThemeCover(
                            icon = Icons.Default.SportsEsports,
                            themeTag = "🎮 迷宫游戏",
                            title = "迷宫地图与颜色碰撞侦测",
                            desc = "方向按键控制与壁障反弹逻辑",
                            iconTint = Color(0xFF0284C7),
                            iconBg = Color(0xFFE0F2FE),
                            bgGradient = listOf(Color(0xFFF0F9FF), Color(0xFFE0F2FE))
                        )
                    // 4. 猫咪 / 宠物 / 步态 / 摇摆
                    name.contains("猫") || name.contains("宠") || name.contains("摇摆") || name.contains("cat") ->
                        HallThemeCover(
                            icon = Icons.Default.Pets,
                            themeTag = "🐱 角色动画",
                            title = "猫咪角色动画与舞台步态",
                            desc = "边缘反弹与多造型连续播放",
                            iconTint = Color(0xFFEA580C),
                            iconBg = Color(0xFFFFEDD5),
                            bgGradient = listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5))
                        )
                    // 5. 太空 / 火箭 / 星球 / 宇宙
                    name.contains("太空") || name.contains("火箭") || name.contains("星") || name.contains("space") ->
                        HallThemeCover(
                            icon = Icons.Default.RocketLaunch,
                            themeTag = "🚀 太空探索",
                            title = "太空飞行与物理坐标碰撞",
                            desc = "星际探险与运动控制积木",
                            iconTint = Color(0xFF2563EB),
                            iconBg = Color(0xFFDBEAFE),
                            bgGradient = listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))
                        )
                    // 6. 画笔 / 绘制 / 几何 / 艺术
                    name.contains("画") || name.contains("笔") || name.contains("draw") || name.contains("paint") ->
                        HallThemeCover(
                            icon = Icons.Default.Palette,
                            themeTag = "🎨 几何画笔",
                            title = "算法画笔与几何艺术绘制",
                            desc = "画笔扩展与多重循环图案",
                            iconTint = Color(0xFFDB2777),
                            iconBg = Color(0xFFFCE7F3),
                            bgGradient = listOf(Color(0xFFFDF2F8), Color(0xFFFCE7F3))
                        )
                    // 7. 通用 Scratch 积木
                    else ->
                        HallThemeCover(
                            icon = Icons.Default.Code,
                            themeTag = "🧩 积木编程",
                            title = "Scratch 3.0 逻辑积木作品",
                            desc = "事件驱动与变量控制逻辑",
                            iconTint = Color(0xFF0D9488),
                            iconBg = Color(0xFFCCFBF1),
                            bgGradient = listOf(Color(0xFFF0FDFA), Color(0xFFCCFBF1))
                        )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(coverConfig.bgGradient))
                    .padding(12.dp)
            ) {
                // 水印背景 Icon
                Icon(
                    imageVector = coverConfig.icon,
                    contentDescription = null,
                    tint = coverConfig.iconTint.copy(alpha = 0.12f),
                    modifier = Modifier
                        .size(90.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 10.dp, y = 10.dp)
                )

                // 主体内容
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = coverConfig.iconBg,
                        shadowElevation = 1.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = coverConfig.icon,
                                contentDescription = null,
                                tint = coverConfig.iconTint,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = coverConfig.iconTint.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = coverConfig.themeTag,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = coverConfig.iconTint,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = coverConfig.title,
                            color = Color(0xFF1E293B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = coverConfig.desc,
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

private data class HallThemeCover(
    val icon: ImageVector,
    val themeTag: String,
    val title: String,
    val desc: String,
    val iconTint: Color,
    val iconBg: Color,
    val bgGradient: List<Color>
)
