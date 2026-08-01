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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherClassManagementUnifiedScreen(viewModel: MainViewModel) {
    val classes by viewModel.classesList.collectAsState()
    val students by viewModel.studentsList.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 新班级添加表单输入状态
    var newClassNameInput by remember { mutableStateOf(TextFieldValue("")) }
    var newClassGradeInput by remember { mutableStateOf(TextFieldValue("三年级")) }
    var newClassDescInput by remember { mutableStateOf(TextFieldValue("")) }
    var manualGradeDropdownExpanded by remember { mutableStateOf(false) }

    // 各种弹窗管理
    var showEditClassDialog by remember { mutableStateOf(false) }
    var activeClassToEdit by remember { mutableStateOf<ClassEntity?>(null) }
    var editClassName by remember { mutableStateOf(TextFieldValue("")) }
    var editClassGrade by remember { mutableStateOf(TextFieldValue("三年级")) }
    var editClassDesc by remember { mutableStateOf(TextFieldValue("")) }
    var editGradeDropdownExpanded by remember { mutableStateOf(false) }

    var editClassLevel by remember { mutableStateOf("三年级") }
    var editClassDailyLimit by remember { mutableStateOf(10) }
    var editClassGrammarCorrect by remember { mutableStateOf(true) }
    var editClassCreativeGuide by remember { mutableStateOf(true) }
    var editClassKnowledgeExplain by remember { mutableStateOf(true) }
    var editClassCodeGenerate by remember { mutableStateOf(false) }
    var editClassStyle by remember { mutableStateOf("趣味活泼") }
    var editClassWeightGrammar by remember { mutableStateOf(25) }
    var editClassWeightLogic by remember { mutableStateOf(30) }
    var editClassWeightTask by remember { mutableStateOf(25) }
    var editClassWeightCreative by remember { mutableStateOf(20) }
    var editClassTeachingLock by remember { mutableStateOf(false) }
    var editClassRemark by remember { mutableStateOf(TextFieldValue("")) }

    var editClassLevelDropdownExpanded by remember { mutableStateOf(false) }
    var editClassStyleDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(showEditClassDialog, activeClassToEdit, editClassDesc) {
        if (showEditClassDialog && activeClassToEdit != null) {
            val parsedResult = viewModel.parseConfigFromDescription(editClassDesc.text)
            editClassLevel = parsedResult["level"] as? String ?: "三年级"
            editClassDailyLimit = parsedResult["dailyLimit"] as? Int ?: 10
            editClassGrammarCorrect = parsedResult["grammarCorrect"] as? Boolean ?: true
            editClassCreativeGuide = parsedResult["creativeGuide"] as? Boolean ?: true
            editClassKnowledgeExplain = parsedResult["knowledgeExplain"] as? Boolean ?: true
            editClassCodeGenerate = parsedResult["codeGenerate"] as? Boolean ?: false
            editClassStyle = parsedResult["style"] as? String ?: "趣味活泼"
            editClassWeightGrammar = parsedResult["weightGrammar"] as? Int ?: 25
            editClassWeightLogic = parsedResult["weightLogic"] as? Int ?: 30
            editClassWeightTask = parsedResult["weightTask"] as? Int ?: 25
            editClassWeightCreative = parsedResult["weightCreative"] as? Int ?: 20
            editClassTeachingLock = parsedResult["teachingLock"] as? Boolean ?: false
            editClassRemark = TextFieldValue(parsedResult["remark"] as? String ?: "")
        }
    }

    var showDeleteClassConfirm by remember { mutableStateOf(false) }
    var activeClassToDelete by remember { mutableStateOf<ClassEntity?>(null) }

    // 单个学生建档弹窗
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var targetClassForStudent by remember { mutableStateOf<ClassEntity?>(null) }
    var newStudentNum by remember { mutableStateOf("") }
    var newStudentName by remember { mutableStateOf("") }
    var newStudentPass by remember { mutableStateOf("123456") } // 默认密码

    // 批量导入学生弹窗
    var showBatchImportDialog by remember { mutableStateOf(false) }
    var targetClassForBatch by remember { mutableStateOf<ClassEntity?>(null) }
    var batchNamesInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部控制台标题卡
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECE0)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "班级教务与 3D 创意 AI 指导规范",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = "管理各班级 parameters、分配学生账号、设置本班专属 AI 提示支持度与灵感纠错限额等",
                            fontSize = 11.sp,
                            color = Color(0xFF5D4037)
                        )
                    }
                }
            }
        }

        // 添加新教学班级卡片
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryAdd,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🆕 建立新的班级大纲",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 班级名称
                        OutlinedTextField(
                            value = newClassNameInput,
                            onValueChange = { newClassNameInput = it },
                            label = { Text("自定义班级名", fontSize = 12.sp) },
                            placeholder = { Text("如：培优1班", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )

                        // 年级下拉框选择
                        Box(modifier = Modifier.weight(0.8f)) {
                            OutlinedTextField(
                                value = newClassGradeInput,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("阶段", fontSize = 12.sp) },
                                trailingIcon = {
                                    IconButton(onClick = { manualGradeDropdownExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { manualGradeDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = manualGradeDropdownExpanded,
                                onDismissRequest = { manualGradeDropdownExpanded = false }
                            ) {
                                listOf("三年级", "四年级", "五年级", "六年级").forEach { grade ->
                                    DropdownMenuItem(
                                        text = { Text(grade, fontSize = 14.sp) },
                                        onClick = {
                                            newClassGradeInput = TextFieldValue(grade)
                                            manualGradeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newClassDescInput,
                        onValueChange = { newClassDescInput = it },
                        label = { Text("班级专属 AI 参数描述及教学锁", fontSize = 12.sp) },
                        placeholder = { Text("如：本班AI辅导锁三年级复杂度，单日创意向限5次...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 手动建档
                        Button(
                            onClick = {
                                if (newClassNameInput.text.isBlank()) {
                                    Toast.makeText(context, "请输入班级名称！", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.createNewClassByTeacher(
                                    newClassNameInput.text,
                                    newClassGradeInput.text,
                                    newClassDescInput.text
                                ) { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    newClassNameInput = TextFieldValue("")
                                    newClassDescInput = TextFieldValue("")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("手动建档此班", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // 班级卡片展示列表标签
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = Color(0xFF455A64),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "📋 当前负责的教学班级档案 (${classes.size} 个)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF37474F)
                )
            }
        }

        if (classes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "王老师，您还没有创建班级哦！\n请在上方输入班级名字或点「一键生成」快速创建。",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        } else {
            items(classes) { classEntity ->
                // 获取此班内注册的学生
                val classStudents = students.filter { it.classId == classEntity.classId }
                val classDesc = viewModel.getClassDescription(classEntity.classId)

                // 异步获取此班级产生的累计 AI 辅助计数
                var aiPointsCount by remember { mutableStateOf<Int?>(null) }
                LaunchedEffect(classEntity.classId) {
                    aiPointsCount = viewModel.getClassAiAssistCount(classEntity.classId)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                                    ) {
                                        Text(
                                            text = classEntity.grade,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0D47A1),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = classEntity.className,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF212121)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val parsedDesc = if (classDesc.trim().startsWith("{") && classDesc.trim().endsWith("}")) {
                                    try {
                                        val json = org.json.JSONObject(classDesc)
                                        val lvl = json.optString("level", "基础班")
                                        val lim = json.optInt("dailyLimit", 10)
                                        val gc = if (json.optBoolean("grammarCorrect", true)) "开" else "关"
                                        val cg = if (json.optBoolean("creativeGuide", true)) "开" else "关"
                                        val ke = if (json.optBoolean("knowledgeExplain", true)) "开" else "关"
                                        val cd = if (json.optBoolean("codeGenerate", false)) "开" else "关"
                                        "难度【$lvl】| 限额值【${lim}次/天】| 权限【纠错:$gc, 创意:$cg, 讲解:$ke, 代码:$cd】"
                                    } catch (e: Exception) {
                                        classDesc
                                    }
                                } else {
                                    classDesc
                                }
                                Text(
                                    text = if (parsedDesc.isNotBlank()) "💡 AI锁配: $parsedDesc" else "💡 AI锁配: 暂无特定说明 (锁定默认难度)",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            // 操作区域：编辑与删除
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        activeClassToEdit = classEntity
                                        editClassName = TextFieldValue(classEntity.className)
                                        editClassGrade = TextFieldValue(classEntity.grade)
                                        editClassDesc = TextFieldValue(classDesc)
                                        showEditClassDialog = true
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "修改班级",
                                        tint = Color(0xFF1976D2),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        activeClassToDelete = classEntity
                                        showDeleteClassConfirm = true
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除班级",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color(0xFFF5F5F5))
                        Spacer(modifier = Modifier.height(8.dp))

                        // AI 指导消耗量
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF8E24AA),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "本班级学生累计索取 AI 智能辅导：",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
                            ) {
                                Text(
                                    text = "${aiPointsCount ?: 0} 次指导",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7B1FA2),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 学生花名册展示
                        Text(
                            text = "👥 学生花名册 (${classStudents.size} 人注册)：",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF37474F)
                        )

                        if (classStudents.isEmpty()) {
                            Text(
                                text = "暂无学生。请使用下方按钮开始建档或者一键导入！",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                classStudents.forEach { student ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                            // 去除学生名称中的括号内容（如"（三年级一班）"）
                                            val displayName = student.name.substringBefore('(')
                                            Text(
                                                text = displayName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF263238),
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = student.studentNumber,
                                                fontSize = 9.sp,
                                                color = Color.LightGray,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 单个建档
                            OutlinedButton(
                                onClick = {
                                    targetClassForStudent = classEntity
                                    newStudentNum = ""
                                    newStudentName = ""
                                    newStudentPass = "123456"
                                    showAddStudentDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF2196F3)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF1976D2))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("手工注册学生", fontSize = 10.sp, color = Color(0xFF1976D2))
                            }

                            // 批量快捷导入
                            OutlinedButton(
                                onClick = {
                                    targetClassForBatch = classEntity
                                    batchNamesInput = ""
                                    showBatchImportDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF388E3C))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("批量秒速导入", fontSize = 10.sp, color = Color(0xFF388E3C))
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. 修改班级弹窗
    if (showEditClassDialog && activeClassToEdit != null) {
        val selClass = activeClassToEdit!!
        val dialogScrollState = rememberScrollState()
        AlertDialog(
            onDismissRequest = { showEditClassDialog = false },
            title = { Text("✏️ 修改班级与 AI 参数配置", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(dialogScrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editClassName,
                        onValueChange = { editClassName = it },
                        label = { Text("班级名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editClassGrade,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("对应年级") },
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
                            listOf("三年级", "四年级", "五年级", "六年级").forEach { grade ->
                                DropdownMenuItem(
                                    text = { Text(grade, fontSize = 14.sp) },
                                    onClick = {
                                        editClassGrade = TextFieldValue(grade)
                                        editGradeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text("💡 AI 指导参数与安全规范配置", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5), modifier = Modifier.padding(top = 8.dp))

                    // 1. 难度登记
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editClassLevel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("AI 阶梯指导难度（难度控制）") },
                            trailingIcon = {
                                IconButton(onClick = { editClassLevelDropdownExpanded = true }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { editClassLevelDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = editClassLevelDropdownExpanded,
                            onDismissRequest = { editClassLevelDropdownExpanded = false }
                        ) {
                            listOf("三年级", "四年级", "五年级", "六年级").forEach { lv ->
                                DropdownMenuItem(
                                    text = { Text(lv, fontSize = 14.sp) },
                                    onClick = {
                                        editClassLevel = lv
                                        editClassLevelDropdownExpanded = false
                                    }
                               )
                            }
                        }
                    }

                    // 2. 风格限制风格下拉选择
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editClassStyle,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("AI 辅导提示词语调语气风格") },
                            trailingIcon = {
                                IconButton(onClick = { editClassStyleDropdownExpanded = true }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { editClassStyleDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = editClassStyleDropdownExpanded,
                            onDismissRequest = { editClassStyleDropdownExpanded = false }
                        ) {
                            listOf("趣味活泼", "通俗易懂", "专业严谨").forEach { style ->
                                DropdownMenuItem(
                                    text = { Text(style, fontSize = 14.sp) },
                                    onClick = {
                                        editClassStyle = style
                                        editClassStyleDropdownExpanded = false
                                    }
                               )
                            }
                        }
                    }

                    // 3. 创意引导单日上限
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("创意引导单日上限", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("学生每日可用限额 (1~20次)", fontSize = 10.sp, color = Color.Gray)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (editClassDailyLimit > 1) editClassDailyLimit-- },
                                enabled = editClassDailyLimit > 1,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (editClassDailyLimit > 1) Color.Black else Color.LightGray)
                            }
                            Text(
                                text = editClassDailyLimit.toString(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            IconButton(
                                onClick = { if (editClassDailyLimit < 20) editClassDailyLimit++ },
                                enabled = editClassDailyLimit < 20,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (editClassDailyLimit < 20) Color.Black else Color.LightGray)
                            }
                        }
                    }

                    // 4. 雷达维度权重配置
                    Text("📊 雷达评测维度权重配置 (0% - 100%)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    
                    listOf(
                        "语法规范权重" to editClassWeightGrammar to { v: Int -> editClassWeightGrammar = v },
                        "逻辑思维权重" to editClassWeightLogic to { v: Int -> editClassWeightLogic = v },
                        "任务匹配权重" to editClassWeightTask to { v: Int -> editClassWeightTask = v },
                        "创意想象权重" to editClassWeightCreative to { v: Int -> editClassWeightCreative = v }
                    ).forEach { pair ->
                        val label = pair.first.first
                        val weight = pair.first.second
                        val setter = pair.second
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, Color(0xFFE0E0E0)), RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (weight >= 5) setter(weight - 5) },
                                    enabled = weight >= 5,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "$weight%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                IconButton(
                                    onClick = { if (weight <= 95) setter(weight + 5) },
                                    enabled = weight <= 95,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 5. 4个功能开关
                    Text("⚙️ 智能助理功能权限控制", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    
                    listOf(
                        Triple("语法纠错", "帮助学生快速定位拼搭中的块语法和运行逻辑错误", editClassGrammarCorrect) to { v: Boolean -> editClassGrammarCorrect = v },
                        Triple("创意引导", "允许学生通过输入主题定制获取趣味拼搭创意引导", editClassCreativeGuide) to { v: Boolean -> editClassCreativeGuide = v },
                        Triple("知识点讲解", "针对循环、变量、克隆等核心要点进行深度辅导", editClassKnowledgeExplain) to { v: Boolean -> editClassKnowledgeExplain = v },
                        Triple("完整代码生成", "允许AI返回完整积木代码（默认低度提示，防止抄袭）", editClassCodeGenerate) to { v: Boolean -> editClassCodeGenerate = v }
                    ).forEach { (info, setter) ->
                        val (title, detail, checked) = info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                Text(detail, fontSize = 10.sp, color = Color.Gray, lineHeight = 13.sp)
                            }
                            androidx.compose.material3.Switch(
                                checked = checked,
                                onCheckedChange = setter,
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF1E88E5)
                                )
                            )
                        }
                    }

                    // 6. 教学锁设置
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF3E0), RoundedCornerShape(6.dp))
                            .border(BorderStroke(1.dp, Color(0xFFFFB74D)), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🔒 开启一键教学锁 (Teaching Lock)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            Text("锁定后，学生端AI将严格执行上述限制且隐藏不相关开关", fontSize = 10.sp, color = Color(0xFFEF6C00), lineHeight = 13.sp)
                        }
                        androidx.compose.material3.Switch(
                            checked = editClassTeachingLock,
                            onCheckedChange = { editClassTeachingLock = it },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEF6C00)
                            )
                        )
                    }

                    OutlinedTextField(
                        value = editClassRemark,
                        onValueChange = { editClassRemark = it },
                        label = { Text("班级备注信息") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editClassName.text.isBlank()) {
                            Toast.makeText(context, "班级名不能为空！", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        // Serialize to JSON string
                        val json = org.json.JSONObject().apply {
                            put("level", editClassLevel)
                            put("dailyLimit", editClassDailyLimit)
                            put("grammarCorrect", editClassGrammarCorrect)
                            put("creativeGuide", editClassCreativeGuide)
                            put("knowledgeExplain", editClassKnowledgeExplain)
                            put("codeGenerate", editClassCodeGenerate)
                            put("style", editClassStyle)
                            put("weightGrammar", editClassWeightGrammar)
                            put("weightLogic", editClassWeightLogic)
                            put("weightTask", editClassWeightTask)
                            put("weightCreative", editClassWeightCreative)
                            put("teachingLock", editClassTeachingLock)
                            put("remark", editClassRemark.text)
                        }
                        val serializedJson = json.toString()

                        viewModel.updateClassByTeacher(
                            classId = selClass.classId,
                            className = editClassName.text,
                            grade = editClassGrade.text,
                            description = serializedJson
                        ) { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            showEditClassDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) {
                    Text("保存更新")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditClassDialog = false }) {
                    Text("取消", color = Color.Gray)
                }
            }
        )
    }

    // 2. 删除班级警示弹窗
    if (showDeleteClassConfirm && activeClassToDelete != null) {
        val selClass = activeClassToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteClassConfirm = false },
            title = { Text("💥 安全级联删除警示", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "您当前正在申请删除教学班级：【${selClass.className}】。\n该业务将连带强制清空该班级档案下注册的全部学生绑定信息及成果足迹等！此行为不可挽回！\n请确认无误后小心点击！",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteClassByTeacher(selClass.classId) { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            showDeleteClassConfirm = false
                            activeClassToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("执意强制删除", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteClassConfirm = false }) {
                    Text("安全退出取消", color = Color.Gray)
                }
            }
        )
    }

    // 3. 手工注册单个学生弹窗
    if (showAddStudentDialog && targetClassForStudent != null) {
        val selClass = targetClassForStudent!!
        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = {
                Text(
                    text = "👤 将学生手动追加至【${selClass.className}】",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newStudentNum,
                        onValueChange = { newStudentNum = it },
                        label = { Text("学号 (登录唯一标识)") },
                        placeholder = { Text("如：20260301") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newStudentName,
                        onValueChange = { newStudentName = it },
                        label = { Text("姓名") },
                        placeholder = { Text("如：张小华") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newStudentPass,
                        onValueChange = { newStudentPass = it },
                        label = { Text("初始密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newStudentNum.isBlank() || newStudentName.isBlank() || newStudentPass.isBlank()) {
                            Toast.makeText(context, "所有字段均必填！", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.registerStudentByTeacher(
                            studentNumber = newStudentNum,
                            name = newStudentName,
                            pass = newStudentPass,
                            classId = selClass.classId
                        ) { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (msg.contains("成功") || msg.contains("完成")) {
                                showAddStudentDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("立即手动注册并建档")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStudentDialog = false }) {
                    Text("取消", color = Color.Gray)
                }
            }
        )
    }

    // 4. 批量导入学生弹窗
    if (showBatchImportDialog && targetClassForBatch != null) {
        val selClass = targetClassForBatch!!
        AlertDialog(
            onDismissRequest = { showBatchImportDialog = false },
            title = {
                Text(
                    text = "📥 批量录入学生至【${selClass.className}】",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "本班会将指定的多名新学生合并成生册，支持通过中文逗号、英文逗号或空格、换行进行拆分。系统将自动批量注册并生成默认初始密码 123456 的学生账号，方便老师一次性全搞定！",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = batchNamesInput,
                        onValueChange = { batchNamesInput = it },
                        label = { Text("学生姓名列表") },
                        placeholder = { Text("如：张小明, 李小红、王五, 赵六\n支持直接从记事本/表格拷贝粘贴...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (batchNamesInput.isBlank()) {
                            Toast.makeText(context, "请输入学生姓名！", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.batchImportStudentsByTeacher(
                            namesStr = batchNamesInput,
                            classEntity = selClass
                        ) { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            showBatchImportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) {
                    Text("立即一键合规导入 🚀")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchImportDialog = false }) {
                    Text("取消", color = Color.Gray)
                }
            }
        )
    }
}

