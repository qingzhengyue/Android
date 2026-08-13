package com.example.ui
import androidx.compose.ui.text.withStyle


import android.app.Activity

import android.content.pm.ActivityInfo

import android.webkit.WebChromeClient

import android.webkit.WebSettings

import android.webkit.WebView

import android.webkit.WebViewClient

import android.webkit.WebResourceRequest

import android.webkit.WebResourceError

import android.widget.Toast

import androidx.compose.animation.*

import androidx.compose.animation.core.*

import androidx.compose.foundation.*

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.layout.FlowRow

import androidx.compose.ui.text.input.TextFieldValue

import androidx.compose.ui.text.input.VisualTransformation

import androidx.compose.ui.text.input.PasswordVisualTransformation

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.rounded.Analytics

import androidx.compose.material.icons.rounded.Extension

import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontFamily

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.compose.ui.viewinterop.AndroidView

import androidx.compose.ui.window.DialogProperties

import android.net.Uri

import android.webkit.ValueCallback

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.gestures.detectDragGestures

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress

import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.unit.IntOffset

import androidx.compose.ui.geometry.Offset

import kotlin.math.roundToInt

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.ui.draw.alpha

import androidx.compose.ui.draw.scale

import androidx.compose.ui.focus.onFocusChanged

import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.ui.text.input.ImeAction

import androidx.compose.ui.text.input.KeyboardType

import com.example.data.*

import java.text.SimpleDateFormat

import java.util.Date

import java.util.Locale

import kotlinx.coroutines.launch

import kotlinx.coroutines.delay

@Composable
fun StudentTasksScreen(viewModel: MainViewModel, onGoToCode: () -> Unit) {
    val tasks by viewModel.tasksList.collectAsState()
    var selectedTaskForDetail by remember { mutableStateOf<com.example.data.LearningTask?>(null) }

    if (selectedTaskForDetail != null) {
        val task = selectedTaskForDetail!!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F8FC))
                .padding(16.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedTaskForDetail = null }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color(0xFF1E88E5)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "任务说明书与修炼目标",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )
            }

            // High Contrast Task Main details
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE3F2FD))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title and status badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = task.taskName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0D47A1)
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (task.status == "进行中") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            )
                        ) {
                            Text(
                                text = task.status,
                                fontSize = 12.sp,
                                color = if (task.status == "进行中") Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "截至提交时间：${task.deadline}",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // "✨ 编程修行任务指南" - Beautiful subsection
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🎒 下达挑战任务规则及提示：",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF37474F)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Detail Body text
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFFB3E5FC))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = task.taskDetail,
                                fontSize = 14.sp,
                                color = Color(0xFF0277BD),
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tips for Children
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFFFFF176))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color(0xFFF57F17),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "小叮咛：如果在搭建 Scratch 积木时遇到搞不懂的问题，可在右侧辅助面板点击「求助 AI 精灵」，精灵姐姐会时刻给你温柔的步骤启发，陪伴你共同打通难关！",
                                fontSize = 11.sp,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { selectedTaskForDetail = null },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E88E5))
                ) {
                    Text("返回列表", color = Color(0xFF1E88E5), fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        // 自动加载/创建关联任务的草稿并跳转到工作区
                        viewModel.enterTaskProgramming(task.taskId, task.taskName) {
                            onGoToCode()
                        }
                        selectedTaskForDetail = null // 回到列表，保证下次进属于列表
                    },
                    modifier = Modifier.weight(1.5f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) {
                    Icon(
                        imageVector = Icons.Default.Launch,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("开始 Scratch 闯关 🚀", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFBFBFB))
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.LocalActivity, contentDescription = null, tint = Color(0xFF1E88E5))
                Spacer(modifier = Modifier.width(8.dp))
                Text("班级本学期学习任务", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
            }

            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Text("老师太好啦，本班当前没有学习任务哦！", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tasks) { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedTaskForDetail = task
                            },
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
                                        text = task.taskName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF212121),
                                        modifier = Modifier.weight(1f)
                                    )

                                    val displayStatus = task.getDisplayStatus()
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (displayStatus == "已截止") Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                                        )
                                    ) {
                                        Text(
                                            text = displayStatus,
                                            fontSize = 11.sp,
                                            color = if (displayStatus == "已截止") Color(0xFFC62828) else Color(0xFF2E7D32),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = task.taskDetail,
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    lineHeight = 18.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("截止日期：${task.deadline}", fontSize = 12.sp, color = Color.Gray)
                                    }

                                    Text(
                                        text = "查看任务详情并闯关 ➔",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E88E5)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentWorksScreen(viewModel: MainViewModel, onGoToCode: (() -> Unit)? = null) {
    val context = LocalContext.current
    val works by viewModel.worksList.collectAsState()
    val activeReport by viewModel.activeReport.collectAsState()
    val isReportLoading by viewModel.isReportLoading.collectAsState()

    var showReportDialog by remember { mutableStateOf(false) }

    val currentClass by viewModel.currentClass.collectAsState()
    val studentNum by viewModel.currentIdentifier.collectAsState()
    val studentName by viewModel.currentUserName.collectAsState()
    val aiRecords by viewModel.aiRecordHistory.collectAsState()

    // Calculate dynamic learning hours record
    val worksCount = works.size
    val aiCount = aiRecords.size
    val baseHours = 12.5f
    val calculatedHours = baseHours + (worksCount * 1.5f) + (aiCount * 0.2f)
    val formattedHours = String.format(java.util.Locale.getDefault(), "%.1f", calculatedHours)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(12.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107))
            Spacer(modifier = Modifier.width(8.dp))
            Text("我的 Scratch 作品列表", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
        }

        if (works.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Text("你还没有提交过作品哦，赶快去编程吧！", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(works) { work ->
                    Card(
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

                                // 操作按钮在右侧，使用 MD3 现代设计语言
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
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, tint = Color(0xFF3F51B5))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("智能 AI 编程评估报告单")
                }
            },
            text = {
                if (isReportLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF3F51B5))
                    }
                } else if (activeReport == null) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF3F51B5), modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("作品已成功提交！\nAI 正在马不停蹄地为您生成专属评估报告\n通常需要 10-15 秒，请稍后重新点开查看哦！", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                } else {
                    val rep = activeReport!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("综合学业评价等级", fontSize = 11.sp, color = Color.Gray)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(text = "${rep.averageScore}", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5))
                                    Text(text = " / 100 分 ", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                                }
                                val badge = when {
                                    rep.averageScore >= 90 -> "卓越五星小神童 ⭐⭐⭐⭐⭐"
                                    rep.averageScore >= 80 -> "四星级优秀小达人 ⭐⭐⭐⭐"
                                    rep.averageScore >= 70 -> "良才闪耀好少年 ⭐⭐⭐"
                                    else -> "持续加油潜力股 ⭐⭐"
                                }
                                Text(badge, fontWeight = FontWeight.Bold, color = Color(0xFFFF5722), fontSize = 13.sp)
                            }
                        }

                        // 细分子项目雷达直条图
                        Text("多维度教学要素测算：", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)

                        StatProgressBar(label = "1. 语法合规性 (检测积木完整拼接)", score = rep.grammarScore, maxScore = 25, gradientColors = listOf(Color(0xFF84DFB4), Color(0xFF28B48F)))
                        StatProgressBar(label = "2. 逻辑完整性 (检测逻辑环嵌套等)", score = rep.logicScore, maxScore = 30, gradientColors = listOf(Color(0xFF8AB4F8), Color(0xFF4285F4)))
                        StatProgressBar(label = "3. 任务匹配度 (检测任务目标要素)", score = rep.taskMatchScore, maxScore = 25, gradientColors = listOf(Color(0xFFFFD180), Color(0xFFFF8F00)))
                        StatProgressBar(label = "4. 创意实现度 (分析交互及原创想法)", score = rep.creativeScore, maxScore = 20, gradientColors = listOf(Color(0xFFD7A1F9), Color(0xFF9333EA)))

                        Spacer(modifier = Modifier.height(16.dp))

                        // AI 优化评析与辅导
                        Text("💡 AI 姐姐精细优化辅导指引：", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = rep.optimizationSuggestions,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFF4E342E),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showReportDialog = false }) {
                    Text("收下报告，去努力！")
                }
            }
        )
    }
}

@Composable
fun StatProgressBar(
    label: String,
    score: Int,
    maxScore: Int,
    gradientColors: List<Color>
) {
    val progressRatio = (score.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f)
    
    // 使用 Spring 动画实现丝滑填充效果
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress_animation"
    )

    // 解析主标题和副标题，增加排版层次感
    val titleMatch = Regex("(.*?)\\s*\\((.*?)\\)").find(label)
    val mainTitle = titleMatch?.groupValues?.get(1) ?: label
    val subTitle = titleMatch?.groupValues?.get(2)

    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    withStyle(style = androidx.compose.ui.text.SpanStyle(
                        color = Color(0xFF2C3E50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )) {
                        append(mainTitle)
                    }
                    if (subTitle != null) {
                        append(" ")
                        withStyle(style = androidx.compose.ui.text.SpanStyle(
                            color = Color(0xFF95A5A6),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )) {
                            append("($subTitle)")
                        }
                    }
                }
            )
            
            Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    withStyle(style = androidx.compose.ui.text.SpanStyle(
                        color = gradientColors.last(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )) {
                        append(score.toString())
                    }
                    withStyle(style = androidx.compose.ui.text.SpanStyle(
                        color = Color(0xFFBDC3C7),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )) {
                        append(" / $maxScore 分")
                    }
                }
            )
        }

        // 现代感进度条轨道
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color(0xFFF0F3F4)) // 极浅的高级灰作为底色
        ) {
            // 渐变填充层
            Box(
                modifier = Modifier
                    .width(maxWidth * animatedProgress)
                    .fillMaxHeight()
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(gradientColors))
            )
        }
    }
}

@Composable
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
                colors = OutlinedTextFieldDefaults.colors(
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


