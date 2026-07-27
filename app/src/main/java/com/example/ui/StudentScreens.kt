package com.example.ui

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun StudentWorksScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val works by viewModel.worksList.collectAsState()
    val activeReport by viewModel.activeReport.collectAsState()

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

                                Button(
                                    onClick = {
                                        viewModel.loadReportForWork(work.workId)
                                        showReportDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("看 AI 评测报告 👀", fontSize = 11.sp, color = Color.White)
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
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF3F51B5))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("智能 AI 编程评估报告单")
                }
            },
            text = {
                if (activeReport == null) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF3F51B5))
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

                        EvaluationProgressRow(label = "语法合规性", score = rep.grammarScore, maxScore = 25, color = Color(0xFF10B981))
                        EvaluationProgressRow(label = "逻辑完整性", score = rep.logicScore, maxScore = 30, color = Color(0xFF3B82F6))
                        EvaluationProgressRow(label = "任务匹配度", score = rep.taskMatchScore, maxScore = 25, color = Color(0xFFF59E0B))
                        EvaluationProgressRow(label = "创意实现度", score = rep.creativeScore, maxScore = 20, color = Color(0xFF8B5CF6))

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
fun EvaluationProgressRow(label: String, score: Int, maxScore: Int, color: Color) {
    AnimatedQuantitativeScoreBar(
        dimensionName = label,
        score = score,
        maxScore = maxScore,
        themeColor = color
    )
}

@Composable
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
}

