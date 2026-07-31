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
fun AiScoreDisplay(workId: Int, viewModel: MainViewModel) {
    val reportFlow = remember(workId) { viewModel.getReportForWorkFlow(workId) }
    val report by reportFlow.collectAsState(initial = null)
    Text(
        text = report?.let { "🤖 AI评分: ${it.averageScore}分" } ?: "🤖 AI评估中...",
        fontSize = 11.sp,
        color = Color(0xFF1E88E5),
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun TeacherWorksClassViewScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val allWorks by viewModel.allWorksList.collectAsState()
    val students by viewModel.studentsList.collectAsState()
    val classes by viewModel.classesList.collectAsState()

    // 状态管理
    var reviewingWork by remember { mutableStateOf<ScratchWork?>(null) }
    var scoreInput by remember { mutableStateOf("90") }
    var commentInput by remember { mutableStateOf("") }
    
    // 统一作品查看弹窗状态（内部可切换积木视图/代码视图）
    var viewingWorkDetail by remember { mutableStateOf<ScratchWork?>(null) }
    var viewingDetailTab by remember { mutableStateOf("积木视图") }

    // sb3 保存路径弹窗状态
    var sb3SavedPath by remember { mutableStateOf<String?>(null) }
    // 是否在弹窗确认后跳转编辑器
    var pendingNavigateToEditor by remember { mutableStateOf(false) }
    
    // 班级筛选状态
    var selectedClassId by remember { mutableStateOf<Int?>(null) }
    var classDropdownExpanded by remember { mutableStateOf(false) }
    
    // 根据选择的班级ID过滤作品
    val filteredWorks = remember(allWorks, selectedClassId) {
        if (selectedClassId == null) {
            allWorks
        } else {
            allWorks.filter { it.classId == selectedClassId }
        }
    }
    
    // 获取选中班级的名称
    val selectedClassName = remember(selectedClassId, classes) {
        if (selectedClassId == null) "全部班级" else {
            classes.find { it.classId == selectedClassId }?.let { "${it.grade} ${it.className}" } ?: "未知班级"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(12.dp)
    ) {
        // 全班学情看板
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            border = BorderStroke(1.dp, Color(0xFFFFB74D))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFFE65100))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("全校/全班学情过程性分析看板", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    Text("解除班级绑定阻碍，所有学生提交的 Scratch 编程作品都可以在此处集中审阅哦！", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        // 顶部全局班级筛选器 (吸顶作为列表内容的前置条件，采用低饱和度柔和胶囊 Chip 风格)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "全部班级" Chip
            val isAllSelected = selectedClassId == null
            Surface(
                onClick = { selectedClassId = null },
                shape = CircleShape,
                color = if (isAllSelected) Color(0xFFE3F2FD) else Color(0xFFF1F5F9),
                border = BorderStroke(
                    1.dp,
                    if (isAllSelected) Color(0xFF90CAF9) else Color(0xFFE2E8F0)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isAllSelected) Color(0xFF1565C0) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "全部班级 (${allWorks.size})",
                        fontSize = 12.sp,
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAllSelected) Color(0xFF1565C0) else Color(0xFF475569)
                    )
                }
            }

            // 各班级 Chips
            classes.forEach { classEntity ->
                val isSelected = selectedClassId == classEntity.classId
                val classWorkCount = allWorks.count { it.classId == classEntity.classId }
                Surface(
                    onClick = { selectedClassId = classEntity.classId },
                    shape = CircleShape,
                    color = if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF1F5F9),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF90CAF9) else Color(0xFFE2E8F0)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isSelected) Color(0xFF1565C0) else Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${classEntity.grade} ${classEntity.className} (${classWorkCount})",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF1565C0) else Color(0xFF475569)
                        )
                    }
                }
            }
        }

        // 下方列表标题
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF1E88E5))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "孩子们最新提交的 Scratch 作品：",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        if (filteredWorks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedClassId == null) 
                            "目前还没有任何孩子提交作品哦！" 
                        else 
                            "【$selectedClassName】目前还没有学生提交作品哦！",
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💡 提示：您可以退登，使用快捷通道登录「张小帅」写个 Scratch 代码并点击「提作并AI评估」喔，再登回王老师便能在这里对他的做业进行评分批改啦！",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredWorks) { work ->
                    val student = students.find { it.studentId == work.studentId }
                    val studentName = student?.let { "${it.name} (学号: ${it.studentNumber})" } ?: "匿名学生 (ID: ${work.studentId})"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = studentName, fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        text = work.workName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.DarkGray
                                    )
                                }

                                // 状态徽章
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (work.reviewStatus) {
                                            "已打分" -> Color(0xFFE8F5E9)
                                            "打回重做" -> Color(0xFFFFEBEE)
                                            else -> Color(0xFFE3F2FD)
                                        }
                                    )
                                ) {
                                    Text(
                                        text = when (work.reviewStatus) {
                                            "已打分" -> "🏆 得分: ${work.teacherScore} 分"
                                            "打回重做" -> "↩️ 已打回重做"
                                            else -> "🤖 AI先评估"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (work.reviewStatus) {
                                            "已打分" -> Color(0xFF2E7D32)
                                            "打回重做" -> Color(0xFFC62828)
                                            else -> Color(0xFF1E88E5)
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 老师评论内容
                            if (work.reviewStatus == "已打分" || work.reviewStatus == "打回重做") {
                                Text(
                                    text = "👩‍🏫 您的评语：${work.teacherComment ?: "孩子非常棒，继续加油！"}",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    lineHeight = 16.sp
                                )
                            } else {
                                Text(
                                    text = "🧩 这件作品刚提交，还保留着 AI 精灵姐姐的初评意见，需要您确认并通过等级/打回孩子修改哦！",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    lineHeight = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(work.submitTime))
                                Text(text = "提交时间: $dateStr", fontSize = 11.sp, color = Color.Gray)

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            viewingDetailTab = "积木视图"
                                            viewingWorkDetail = work
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("🧩 查看作品", fontSize = 11.sp, color = Color.White)
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Button(
                                        onClick = {
                                            // 将作品 JSON 保存为 .sb3 文件，跳转到 Scratch 镜像站加载
                                            try {
                                                val sb3File = java.io.File(context.cacheDir, "view_source_${work.workId}_${System.currentTimeMillis()}.sb3")
                                                sb3File.writeText(work.workCode, Charsets.UTF_8)
                                                viewModel.teacherPendingSb3Path.value = sb3File.absolutePath
                                                sb3SavedPath = sb3File.absolutePath
                                                pendingNavigateToEditor = true
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "保存源文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("🚀 看源程序", fontSize = 11.sp, color = Color.White)
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Button(
                                        onClick = {
                                            reviewingWork = work
                                            scoreInput = (work.teacherScore ?: 90).toString()
                                            commentInput = work.teacherComment ?: ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("📝 批改点评", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 教师专业审查弹窗
    if (reviewingWork != null) {
        val workForReview = reviewingWork!!
        val student = students.find { it.studentId == workForReview.studentId }
        val sName = student?.name ?: "学生"

        AlertDialog(
            onDismissRequest = { reviewingWork = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BorderColor, contentDescription = null, tint = Color(0xFFE65100))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("评判作品：${workForReview.workName}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("作作者：$sName", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))

                    // 评分预设快捷按钮
                    Text("💯 快捷评分预立刻打分：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("95", "85", "75", "60").forEach { preset ->
                            OutlinedButton(
                                onClick = { scoreInput = preset },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                            ) {
                                Text(
                                    preset + when (preset) {
                                        "95" -> "(优秀)"
                                        "85" -> "(良好)"
                                        "75" -> "(及格)"
                                        else -> "(待改进)"
                                    }, fontSize = 10.sp
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = scoreInput,
                        onValueChange = { scoreInput = it },
                        label = { Text("最终评价得分 (满分100分)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        singleLine = true
                    )

                    // 评审快捷评语按钮
                    Text("💭 常用儿童鼓励性快捷评语：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "✨ 积木搭拼完美，逻辑非常棒！继续保持哦！",
                            "🐱 游戏创意妙趣横生！运行得太完美啦！",
                            "💡 小猫漫步很流畅，加油继续丰富你的剧本细节！",
                            "🧱 积木块好像有一处小错乱，精灵姐姐老师建议你载入重新修改一下哦！"
                        ).forEach { commentPreset ->
                            OutlinedButton(
                                onClick = { commentInput = commentPreset },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = commentPreset,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        label = { Text("撰写具体评价与成长指导") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. 取消按钮 (中性操作，权重 1)
                        OutlinedButton(
                            onClick = { reviewingWork = null },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF4B5563)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("取消", color = Color(0xFF9CA3AF), fontSize = 14.sp, maxLines = 1)
                        }

                        // 2. 打回重做按钮 (负向操作，红色，权重 1.2)
                        Button(
                            onClick = {
                                val scoreVal = scoreInput.toIntOrNull() ?: 60
                                viewModel.submitTeacherReview(
                                    workId = workForReview.workId,
                                    status = "打回重做",
                                    score = scoreVal,
                                    comment = commentInput
                                ) { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    reviewingWork = null
                                }
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("打回重做", fontSize = 14.sp, maxLines = 1)
                        }

                        // 3. 过审并打分按钮 (主操作，绿色，权重 1.5 最大，最显眼)
                        Button(
                            onClick = {
                                val scoreVal = scoreInput.toIntOrNull() ?: 90
                                viewModel.submitTeacherReview(
                                    workId = workForReview.workId,
                                    status = "已打分",
                                    score = scoreVal,
                                    comment = commentInput
                                ) { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    reviewingWork = null
                                }
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("过审并打分", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = null
        )
    }

    // ========== 统一作品查看弹窗（积木视图 / 代码视图切换） ==========
    if (viewingWorkDetail != null) {
        val detailWork = viewingWorkDetail!!
        val detailStudent = students.find { it.studentId == detailWork.studentId }
        val detailStudentName = detailStudent?.let { "${it.name} (学号: ${it.studentNumber})" } ?: "学生ID: ${detailWork.studentId}"

        val formattedJson = remember(detailWork.workCode) {
            if (detailWork.workCode.isBlank() || detailWork.workCode == "{}" || detailWork.workCode == "\"\"") {
                "# [此作品未包含任何积木代码，可能学生提交了空草稿]"
            } else {
                com.example.data.ScratchToPythonConverter.convertJsonToPython(detailWork.workCode)
            }
        }

        // JavascriptInterface 传递项目数据给 WebView
        class TeacherBlockViewerInterface(private val projectData: String) {
            @android.webkit.JavascriptInterface
            fun getProjectJson(): String = projectData

            @android.webkit.JavascriptInterface
            fun getProjectData(): String = projectData
        }

        AlertDialog(
            onDismissRequest = { viewingWorkDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null, tint = Color(0xFF1E88E5))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("🧩 作品查看器", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("作者: $detailStudentName  |  作品: ${detailWork.workName}", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.75f)
                ) {
                    // 切换标签
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { viewingDetailTab = "积木视图" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewingDetailTab == "积木视图") Color(0xFF1E88E5) else Color(0xFFB0BEC5)
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) { Text("🧩 积木视图", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        Button(
                            onClick = { viewingDetailTab = "代码视图" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewingDetailTab == "代码视图") Color(0xFF6A1B9A) else Color(0xFFB0BEC5)
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) { Text("📄 代码视图", fontSize = 11.sp) }
                        Button(
                            onClick = { viewingDetailTab = "AI 评测" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewingDetailTab == "AI 评测") Color(0xFF00897B) else Color(0xFFB0BEC5)
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) { Text("🤖 AI 评测", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }

                    when (viewingDetailTab) {
                        "积木视图" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                            ) {
                                val targetBlocksList = remember(detailWork.workCode) {
                                    val list = mutableListOf<Pair<String, Pair<List<Pair<String, org.json.JSONObject>>, org.json.JSONObject>>>()
                                    try {
                                        val json = org.json.JSONObject(detailWork.workCode)
                                        if (json.has("targets")) {
                                            val targets = json.getJSONArray("targets")
                                            for (i in 0 until targets.length()) {
                                                val target = targets.getJSONObject(i)
                                                val name = target.optString("name", "角色 ${i+1}")
                                                val isStage = target.optBoolean("isStage", false)
                                                val displayName = if (isStage) "🖼️ 舞台 ($name)" else "🐱 角色 ($name)"
                                                val blockItems = mutableListOf<Pair<String, org.json.JSONObject>>()
                                                val blocksObj = target.optJSONObject("blocks") ?: org.json.JSONObject()
                                                
                                                blocksObj.keys().forEach { key ->
                                                    val block = blocksObj.optJSONObject(key)
                                                    val opcode = block?.optString("opcode")
                                                    if (!opcode.isNullOrEmpty()) {
                                                        blockItems.add(Pair(opcode, block))
                                                    }
                                                }
                                                if (blockItems.isNotEmpty() || i == 0) {
                                                    list.add(Pair(displayName, Pair(blockItems, blocksObj)))
                                                }
                                            }
                                        } else if (json.has("blocks")) {
                                            val blockItems = mutableListOf<Pair<String, org.json.JSONObject>>()
                                            val blocksObj = json.optJSONObject("blocks") ?: org.json.JSONObject()
                                            blocksObj.keys().forEach { key ->
                                                val block = blocksObj.optJSONObject(key)
                                                val opcode = block?.optString("opcode")
                                                if (!opcode.isNullOrEmpty()) {
                                                    blockItems.add(Pair(opcode, block))
                                                }
                                            }
                                            list.add(Pair("🐱 角色 1", Pair(blockItems, blocksObj)))
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    list
                                }

                                androidx.compose.foundation.lazy.LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    contentPadding = PaddingValues(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    targetBlocksList.forEach { (targetName, blocksData) ->
                                        val blockItems = blocksData.first
                                        val blocksMap = blocksData.second
                                        
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 16.dp, bottom = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = targetName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF333333),
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp))
                                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${blockItems.size} 个积木",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF1976D2),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                                        }
                                        
                                        if (blockItems.isEmpty()) {
                                            item {
                                                Text("无积木程序", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                                            }
                                        } else {
                                            items(blockItems) { blockItem ->
                                                val opcode = blockItem.first
                                                val blockJson = blockItem.second
                                                
                                                BlockItemView(
                                                    opcode = opcode,
                                                    blockJson = blockJson,
                                                    blocksMap = blocksMap
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "代码视图"   -> {
                            // JSON 代码视图
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF263238))
                            ) {
                                Text(
                                    text = formattedJson,
                                    fontSize = 9.sp,
                                    color = Color(0xFF80CBC4),
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 13.sp,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .horizontalScroll(rememberScrollState())
                                        .padding(8.dp)
                                )
                            }
                        }
                        "AI 评测" -> {
                            val reportFlow = remember(detailWork.workId) { viewModel.getReportForWorkFlow(detailWork.workId) }
                            val rep by reportFlow.collectAsState(initial = null)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFAFAFA))
                                    .padding(10.dp)
                            ) {
                                if (rep != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("AI 综合评测得分", fontSize = 11.sp, color = Color.Gray)
                                                Row(verticalAlignment = Alignment.Bottom) {
                                                    Text(text = "${rep!!.averageScore}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5))
                                                    Text(text = " / 100 分", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                                                }
                                                val badge = when {
                                                    rep!!.averageScore >= 90 -> "卓越五星 ⭐⭐⭐⭐⭐"
                                                    rep!!.averageScore >= 80 -> "四星优秀 ⭐⭐⭐⭐"
                                                    rep!!.averageScore >= 70 -> "三星良好 ⭐⭐⭐"
                                                    else -> "持续加油 ⭐⭐"
                                                }
                                                Text(badge, fontWeight = FontWeight.Bold, color = Color(0xFFD97706), fontSize = 12.sp)
                                            }
                                        }

                                        Text("多维度量化评测结果：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                        Spacer(modifier = Modifier.height(4.dp))

                                        AnimatedQuantitativeScoreBar(dimensionName = "语法合规性", score = rep!!.grammarScore, maxScore = 25, themeColor = Color(0xFF10B981), icon = Icons.Default.Code)
                                        AnimatedQuantitativeScoreBar(dimensionName = "逻辑完整性", score = rep!!.logicScore, maxScore = 30, themeColor = Color(0xFF3B82F6), icon = Icons.Default.Psychology)
                                        AnimatedQuantitativeScoreBar(dimensionName = "任务匹配度", score = rep!!.taskMatchScore, maxScore = 25, themeColor = Color(0xFFF59E0B), icon = Icons.Default.AssignmentTurnedIn)
                                        AnimatedQuantitativeScoreBar(dimensionName = "创意实现度", score = rep!!.creativeScore, maxScore = 20, themeColor = Color(0xFF8B5CF6), icon = Icons.Default.AutoAwesome)

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text("💡 AI 精细优化指引：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = rep!!.optimizationSuggestions,
                                                fontSize = 11.sp,
                                                lineHeight = 16.sp,
                                                color = Color(0xFF78350F),
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SmartToy,
                                            contentDescription = null,
                                            tint = Color(0xFF00897B),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "🤖 正在生成 AI 评测报告中...\n(通常需要 10-15 秒)",
                                            fontSize = 13.sp,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = viewingDetailTab == "代码视图",
                            modifier = if (viewingDetailTab == "代码视图") Modifier.weight(1f) else Modifier,
                            enter = androidx.compose.animation.expandHorizontally() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkHorizontally() + androidx.compose.animation.fadeOut()
                        ) {
                            // 1. 复制代码按钮（主操作）
                            Button(
                                onClick = {
                                    try {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Scratch Work Code", formattedJson)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "代码已复制到剪贴板！", Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "复制失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("复制代码", fontSize = 15.sp, maxLines = 1)
                            }
                        }

                        // 2. 关闭按钮（次操作）
                        OutlinedButton(
                            onClick = { viewingWorkDetail = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF4B5563))
                        ) {
                            Text("关闭", color = Color(0xFFD1D5DB), fontSize = 15.sp, maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = null,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }

    // sb3 保存成功路径弹窗
    if (sb3SavedPath != null) {
        AlertDialog(
            onDismissRequest = { sb3SavedPath = null },
            title = { Text("✅ 源文件保存成功", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("文件已存储到安卓设备本地路径：", fontSize = 13.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = sb3SavedPath!!,
                        fontSize = 12.sp,
                        color = Color(0xFF1565C0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE3F2FD), RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        sb3SavedPath = null
                        if (pendingNavigateToEditor) {
                            pendingNavigateToEditor = false
                            viewModel.teacherViewingWorkspace.value = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) {
                    Text("知道了", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = null
        )
    }
}


