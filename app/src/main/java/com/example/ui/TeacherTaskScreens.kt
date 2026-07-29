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
fun TeacherTaskManagementScreen(viewModel: MainViewModel) {
    val classes by viewModel.classesList.collectAsState()
    val currentIdentifier by viewModel.currentIdentifier.collectAsState() // 获取当前登录教师的工号

    var taskNameInput by remember { mutableStateOf(TextFieldValue("")) }
    var taskDetailInput by remember { mutableStateOf(TextFieldValue("")) }
    var taskGradeInput by remember { mutableStateOf("三年级") }
    var taskDeadlineInput by remember { mutableStateOf("2026-06-30") }

    var classSelectIndex by remember { mutableStateOf(0) }
    var classSelectExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Feature Header Control Console
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${currentIdentifier.ifEmpty { "教师" }}的管理事务中心 - 发布控制台",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }

        // 2. Clear Visual Separator (Divider) as requested by Problem 3.1
        item {
            Divider(
                color = Color(0xFFB0BEC5), 
                thickness = 2.dp, 
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // 3. ✨ 新建任务框 (新建学习任务表单)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🎺 创作并向班级快捷下发新任务", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. 任务名称 (优化四) - 优化1
                    Text("任务名称", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = taskNameInput,
                        onValueChange = { taskNameInput = it },
                        placeholder = { Text("输入任务名称 (例如: 快乐猫捉老鼠)", color = Color(0xFF666666), fontSize = 14.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1976D2),
                            unfocusedBorderColor = Color(0xFF2196F3)
                        ),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. 面向年级 - 优化1
                    var taskGradeDropdownExpanded by remember { mutableStateOf(false) }
                    Text("面向年级", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = taskGradeInput.ifEmpty { "请选择年级" },
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("请选择年级", color = Color(0xFF666666), fontSize = 14.sp) },
                            trailingIcon = {
                                IconButton(onClick = { taskGradeDropdownExpanded = true }) {
                                    Icon(imageVector = if (taskGradeDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFF2196F3)
                            ),
                            modifier = Modifier.fillMaxWidth().height(56.dp).clickable { taskGradeDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = taskGradeDropdownExpanded,
                            onDismissRequest = { taskGradeDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            listOf("三年级", "四年级", "五年级", "六年级").forEach { grade ->
                                DropdownMenuItem(
                                    text = { Text(grade) },
                                    onClick = {
                                        taskGradeInput = grade
                                        taskGradeDropdownExpanded = false
                                        // Auto-reset target class index to stay safe!
                                        classSelectIndex = 0
                                    }
                                    )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. 截止日期 - 优化1
                    Text("截止日期", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = taskDeadlineInput,
                        onValueChange = { taskDeadlineInput = it },
                        placeholder = { Text("格式: YYYY-MM-DD", color = Color(0xFF666666), fontSize = 14.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1976D2),
                            unfocusedBorderColor = Color(0xFF2196F3)
                        ),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. 派发目标班级 (仅显示当前选定年级下的所有班级 - 优化四) - 优化1
                    val filteredClasses = remember(classes, taskGradeInput) {
                        if (taskGradeInput.isBlank() || taskGradeInput == "请选择年级") {
                            classes
                        } else {
                            classes.filter { it.grade == taskGradeInput }
                        }
                    }

                    Text("派发目标班级", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))
                    if (filteredClasses.isNotEmpty()) {
                        val safeIndex = classSelectIndex.coerceIn(0, filteredClasses.size - 1)
                        val selClass = filteredClasses[safeIndex]
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = "发派目标班级：${selClass.className}",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("请选择班级", color = Color(0xFF666666), fontSize = 14.sp) },
                                trailingIcon = {
                                    IconButton(onClick = { classSelectExpanded = true }) {
                                        Icon(imageVector = if (classSelectExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1976D2),
                                    unfocusedBorderColor = Color(0xFF2196F3)
                                ),
                                modifier = Modifier.fillMaxWidth().height(56.dp).clickable { classSelectExpanded = true }
                            )
                            DropdownMenu(
                                expanded = classSelectExpanded,
                                onDismissRequest = { classSelectExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                filteredClasses.forEachIndexed { i, c ->
                                    DropdownMenuItem(
                                        text = { Text(c.className) },
                                        onClick = {
                                            classSelectIndex = i
                                            classSelectExpanded = false
                                        }
                                        )
                                }
                            }
                        }
                    } else {
                        // Display message if there are no classes in the filtered list
                        OutlinedTextField(
                            value = "该年级暂无已建班级，请在上方“班级管理”中添加！",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFF2196F3)
                            ),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. 详细描述 (优化一 200dp height with 8dp padding in scrollable layout) - 优化1
                    Text("具体编程任务指引与积木块要求详情", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = taskDetailInput,
                        onValueChange = { taskDetailInput = it },
                        placeholder = { Text("请输入具体的作业设计内容与评分块要求...", color = Color(0xFF666666), fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1976D2),
                            unfocusedBorderColor = Color(0xFF2196F3)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .padding(8.dp),
                        maxLines = 10
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6. 下发发布按钮 (56dp height, match_parent width, bold 16sp centered - 优化四)
                    Button(
                        onClick = {
                            if (taskNameInput.text.isEmpty() || taskDetailInput.text.isEmpty() || filteredClasses.isEmpty()) {
                                Toast.makeText(context, "请填齐基本字段，并确认当前所选年级已建班级！", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val safeIndex = classSelectIndex.coerceIn(0, filteredClasses.size - 1)
                            val classId = filteredClasses[safeIndex].classId
                            viewModel.publishNewTaskByTeacher(
                                name = taskNameInput.text,
                                detail = taskDetailInput.text,
                                grade = taskGradeInput,
                                deadlineStr = taskDeadlineInput,
                                classId = classId
                            ) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                taskNameInput = TextFieldValue("")
                                taskDetailInput = TextFieldValue("")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("立即向选定班级下发发布", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherTaskListScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val classes by viewModel.classesList.collectAsState()
    val tasks by viewModel.tasksList.collectAsState()
    val allWorks by viewModel.allWorksList.collectAsState()
    val students by viewModel.studentsList.collectAsState()

    val currentTeacherId by viewModel.currentUserId.collectAsState()

    // 优先筛选出当前登录教师发布的任务，如无特定匹配则展示系统中所有教学任务，并按taskId降序排列
    val teacherTasks = remember(tasks, currentTeacherId) {
        val filtered = tasks.filter { it.teacherId == currentTeacherId }
        if (filtered.isNotEmpty()) {
            filtered.sortedByDescending { it.taskId }
        } else {
            tasks.sortedByDescending { it.taskId }
        }
    }

    var selectedTask by remember { mutableStateOf<LearningTask?>(null) }
    
    // Task Management States
    var activeMenuTaskId by remember { mutableStateOf<Int?>(null) }
    var showEditTaskDialog by remember { mutableStateOf<LearningTask?>(null) }
    var showExtendDeadlineDialog by remember { mutableStateOf<LearningTask?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<LearningTask?>(null) }
    var showRevokeConfirmDialog by remember { mutableStateOf<LearningTask?>(null) }

    var editName by remember { mutableStateOf(TextFieldValue("")) }
    var editDetail by remember { mutableStateOf(TextFieldValue("")) }
    var editGrade by remember { mutableStateOf("") }
    var editDeadline by remember { mutableStateOf("") }
    var editClassId by remember { mutableStateOf(-1) }

    var extendDeadlineInput by remember { mutableStateOf("") }
    
    // 批改用临时状态
    var reviewingWork by remember { mutableStateOf<ScratchWork?>(null) }
    var scoreInput by remember { mutableStateOf("90") }
    var commentInput by remember { mutableStateOf("") }

    if (selectedTask == null) {
        // --- 任务列表主页 ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Assignment, contentDescription = null, tint = Color(0xFF1E88E5))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("您已下发给各班的 Scratch 编程任务", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        Text("点击任意任务卡片，即可跳转查看详细的学生作业提交通道、AI初评以及您对孩子们的精细打分状态哦！", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            Text(
                text = "📊 已下发任务列表 (${teacherTasks.size} 个):",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (teacherTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("您目前还没有发布任何 Scratch 教学任务哦。快去「发布任务」下发一下吧！", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(teacherTasks) { t ->
                        val targetClass = classes.find { it.classId == t.classId }
                        val className = targetClass?.className ?: "未知班级"
                        
                        // 统计提交人数
                        val subCount = allWorks.filter { it.taskId == t.taskId }.size
                        val isCancelled = t.status == "已撤销"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isCancelled) Modifier.alpha(0.6f) else Modifier)
                                .clickable { selectedTask = t },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCancelled) Color(0xFFF5F5F5) else Color.White
                            ),
                            border = BorderStroke(1.dp, if (isCancelled) Color(0xFFE0E0E0) else Color(0xFFEEEEEE))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = t.taskName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isCancelled) Color.Gray else Color.DarkGray,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isCancelled) Color(0xFFE0E0E0) else Color(0xFFFFF3E0)
                                            ),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "🏫 $className",
                                                fontSize = 10.sp,
                                                color = if (isCancelled) Color.Gray else Color(0xFFE65100),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        
                                        // 任务状态
                                        val displayStatus = t.getDisplayStatus()
                                        val statusColor = when (displayStatus) {
                                            "已撤销" -> Color(0xFF757575)
                                            "已截止" -> Color(0xFFD32F2F)
                                            else -> Color(0xFF2E7D32)
                                        }
                                        val statusBgColor = when (displayStatus) {
                                            "已撤销" -> Color(0xFFE0E0E0)
                                            "已截止" -> Color(0xFFFFEBEE)
                                            else -> Color(0xFFE8F5E9)
                                        }
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = statusBgColor),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = displayStatus,
                                                fontSize = 10.sp,
                                                color = statusColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Box {
                                            IconButton(
                                                onClick = { activeMenuTaskId = if (activeMenuTaskId == t.taskId) null else t.taskId },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "更多选项",
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            
                                            DropdownMenu(
                                                expanded = activeMenuTaskId == t.taskId,
                                                onDismissRequest = { activeMenuTaskId = null }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("编辑任务", fontSize = 13.sp) },
                                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                    onClick = {
                                                        activeMenuTaskId = null
                                                        showEditTaskDialog = t
                                                        editName = TextFieldValue(t.taskName)
                                                        editDetail = TextFieldValue(t.taskDetail)
                                                        editGrade = t.grade ?: "三年级"
                                                        editDeadline = t.deadline
                                                        editClassId = t.classId
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("延迟截止时间", fontSize = 13.sp) },
                                                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                    onClick = {
                                                        activeMenuTaskId = null
                                                        showExtendDeadlineDialog = t
                                                        extendDeadlineInput = t.deadline
                                                    }
                                                )
                                                val nextStatus = if (t.status == "已撤销") "进行中" else "已撤销"
                                                val statusText = if (t.status == "已撤销") "恢复下发" else "撤销下发"
                                                val statusIcon = if (t.status == "已撤销") Icons.Default.Publish else Icons.Default.Cancel
                                                DropdownMenuItem(
                                                    text = { Text(statusText, fontSize = 13.sp) },
                                                    leadingIcon = { Icon(statusIcon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                    onClick = {
                                                        activeMenuTaskId = null
                                                        if (nextStatus == "已撤销") {
                                                            showRevokeConfirmDialog = t
                                                        } else {
                                                            viewModel.updateTaskStatusByTeacher(t.taskId, nextStatus) { msg ->
                                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                )
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                                DropdownMenuItem(
                                                    text = { Text("删除任务", color = Color.Red, fontSize = 13.sp) },
                                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp)) },
                                                    onClick = {
                                                        activeMenuTaskId = null
                                                        showDeleteConfirmDialog = t
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = t.taskDetail,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⏰ 截止期限: ${t.deadline}",
                                        fontSize = 11.sp,
                                        color = Color.Red,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "✏️ 提交率/人数: $subCount 人",
                                        fontSize = 12.sp,
                                        color = Color(0xFF1E88E5),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- 任务详情与学生提交状况页面 ---
        val task = selectedTask!!
        val taskClass = classes.find { it.classId == task.classId }
        val taskClassName = taskClass?.className ?: "未知班级"
        
        // 筛选此任务的学生作业提交状况
        val taskWorks = remember(allWorks, task.taskId) {
            allWorks.filter { it.taskId == task.taskId }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9F9F9))
                .padding(12.dp)
        ) {
            // 返回及标题控制栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { selectedTask = null },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "任务详情及提交通道",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
            }

            // 任务基础描述卡 (优化二：整体布局、单独多行文本、灰字单独下发班级，间距8dp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFEAEAEA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = task.taskName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "下发班级：$taskClassName",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "截止日期：${task.deadline}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = task.taskDetail,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "🙋 学生作品提交列表 (${taskWorks.size} 件):",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            if (taskWorks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("该班学生目前还没有开始提交本次作业的代码哦 ~", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(taskWorks) { work ->
                        val student = students.find { it.studentId == work.studentId }
                        val studName = student?.let { "${it.name}" } ?: "学生 (ID: ${work.studentId})"
                        val isGraded = work.reviewStatus == "已打分" || work.reviewStatus == "打回重做"
                        val formattedTime = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()).format(Date(work.submitTime))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF1F1F1))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.3f)) {
                                    // 点击姓名直接去快速批改
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            viewModel.loadWorkToWorkspace(work)
                                            viewModel.teacherViewingWorkspace.value = true
                                            Toast.makeText(context, "已成功载入该作品代码，正在为您打开 Scratch 编程工作区...", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(
                                            text = "👤 $studName",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF1E88E5),
                                            style = androidx.compose.ui.text.TextStyle(
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Launch,
                                            contentDescription = null,
                                            tint = Color(0xFF1E88E5),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "提交时间: $formattedTime",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1.2f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    // Reactive AI score display using getReportForWorkFlow helper Composable
                                    AiScoreDisplay(workId = work.workId, viewModel = viewModel)
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isGraded) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                        ),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isGraded) {
                                                if (work.reviewStatus == "打回重做") "↩️ 打回原形" else "🏆 已批改: ${work.teacherScore}分"
                                            } else {
                                                "⏳ 未批改"
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGraded) Color(0xFF2E7D32) else Color(0xFFE65100),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.weight(0.7f),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = {
                                            viewModel.loadWorkToWorkspace(work)
                                            reviewingWork = work
                                            scoreInput = (work.teacherScore ?: 90).toString()
                                            commentInput = work.teacherComment ?: ""
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BorderColor,
                                            contentDescription = "批改",
                                            tint = Color(0xFFFF9800),
                                            modifier = Modifier.size(20.dp)
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

    // 教师专业审查评分和评语弹窗
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
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("请为 $sName 编写成长指导与评价", fontSize = 13.sp, color = Color.Gray)

                    Text("分值快捷映射：", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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

    // --- Task Edit Dialog (Requirement 3 CRUD) ---
    showEditTaskDialog?.let { task ->
        AlertDialog(
            onDismissRequest = { showEditTaskDialog = null },
            title = { Text("✏️ 编辑学习任务", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("任务标题") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDetail,
                        onValueChange = { editDetail = it },
                        label = { Text("任务详情（练习要求及说明）") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = editDeadline,
                        onValueChange = { editDeadline = it },
                        label = { Text("截止期限 (格式: yyyy-MM-dd)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Grade & Class dropdowns in Edit Task Dialog to support full CRUD modification
                    var editGradeDropdownExpanded by remember { mutableStateOf(false) }
                    var editClassDropdownExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = editGrade.ifEmpty { "三年级" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("面向年级") },
                                trailingIcon = {
                                    IconButton(onClick = { editGradeDropdownExpanded = true }) {
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { editGradeDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = editGradeDropdownExpanded,
                                onDismissRequest = { editGradeDropdownExpanded = false }
                            ) {
                                listOf("三年级", "四年级", "五年级", "六年级").forEach { g ->
                                    DropdownMenuItem(
                                        text = { Text(g) },
                                        onClick = {
                                            editGrade = g
                                            editGradeDropdownExpanded = false
                                            val filtered = classes.filter { it.grade == g }
                                            if (filtered.isNotEmpty()) {
                                                editClassId = filtered[0].classId
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            val currentClass = classes.find { it.classId == editClassId }
                            val currentClassName = currentClass?.className ?: "请选择班级"

                            OutlinedTextField(
                                value = currentClassName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("目标班级") },
                                trailingIcon = {
                                    IconButton(onClick = { editClassDropdownExpanded = true }) {
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { editClassDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = editClassDropdownExpanded,
                                onDismissRequest = { editClassDropdownExpanded = false }
                            ) {
                                classes.filter { it.grade == editGrade }.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.className) },
                                        onClick = {
                                            editClassId = c.classId
                                            editClassDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.text.isBlank() || editDetail.text.isBlank() || editDeadline.isBlank()) {
                            Toast.makeText(context, "所有文本字段都不能为空哦", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (editClassId == -1) {
                            Toast.makeText(context, "请先选择一个目标班级哦", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.editTaskByTeacher(
                            taskId = task.taskId,
                            name = editName.text,
                            detail = editDetail.text,
                            grade = editGrade,
                            deadlineStr = editDeadline,
                            classId = editClassId
                        ) { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            showEditTaskDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) {
                    Text("确认保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTaskDialog = null }) {
                    Text("取消", color = Color.Gray)
                }
            }
        )
    }

    // --- Task Extend Deadline Dialog (Requirement 3 CRUD) ---
    showExtendDeadlineDialog?.let { task ->
        // Native Android DatePickerDialog trigger
        val currentParts = task.deadline.split("-")
        val defYear = currentParts.getOrNull(0)?.toIntOrNull() ?: 2026
        val defMonth = (currentParts.getOrNull(1)?.toIntOrNull() ?: 6) - 1
        val defDay = currentParts.getOrNull(2)?.toIntOrNull() ?: 15
        
        LaunchedEffect(task.taskId) {
            android.app.DatePickerDialog(
                context,
                { _, year, monthOfYear, dayOfMonth ->
                    val selectedDateFormatted = String.format("%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth)
                    viewModel.editTaskByTeacher(
                        taskId = task.taskId,
                        name = task.taskName,
                        detail = task.taskDetail,
                        grade = task.grade ?: "三年级",
                        deadlineStr = selectedDateFormatted,
                        classId = task.classId
                    ) { msg ->
                        Toast.makeText(context, "截止日期已成功延长！且全班学生端立即同步通知！", Toast.LENGTH_SHORT).show()
                        showExtendDeadlineDialog = null
                    }
                },
                defYear,
                defMonth,
                defDay
            ).apply {
                setOnCancelListener {
                    showExtendDeadlineDialog = null
                }
            }.show()
        }
    }

    // --- Task Delete Confirm Dialog (Requirement 3 CRUD) ---
    showDeleteConfirmDialog?.let { task ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("🚨 警告：彻底删除该任务吗？", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
            text = {
                Text(
                    text = "您正尝试删除 Scratch 学习任务【${task.taskName}】。该操作将一并清理：\n\n" +
                            "1. 该任务所分派班级内所有学生的 Scratch 编程进度。\n" +
                            "2. 所有的 AI 多维度对答初评细节。\n" +
                            "3. 班级已批完打分与成长评论数据。\n\n" +
                            "⚠️ 注意：删除后该大纲下数据将永不恢复，确定要彻底抹去吗？",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTaskByTeacher(task.taskId) { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            showDeleteConfirmDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("彻底永久删除", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("容我再想一下", color = Color.Gray)
                }
            }
        )
    }

    // --- Task Revoke Confirm Dialog ---
    showRevokeConfirmDialog?.let { task ->
        AlertDialog(
            onDismissRequest = { showRevokeConfirmDialog = null },
            title = { Text("⚠️ 确定撤销此任务吗？", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
            text = {
                Text("确定撤销此任务吗？撤销后学生将无法查看和提交该编程任务【${task.taskName}】。您依然可以随时点击“恢复下发”重新分派该任务给班级。", fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTaskStatusByTeacher(task.taskId, "已撤销") { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            showRevokeConfirmDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("确认撤销", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeConfirmDialog = null }) {
                    Text("再想想", color = Color.Gray)
                }
            }
        )
    }
}

