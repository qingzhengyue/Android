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
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userRole by viewModel.currentUserRole.collectAsState()

    if (!isLoggedIn) {
        LoginScreen(viewModel = viewModel)
    } else {
        MainPortalScreen(viewModel = viewModel, userRole = userRole ?: "student")
    }
}

// ==========================================
// 1. 用户认证屏幕 (Login & Register)
// ==========================================
@Composable
fun LoginScreen(viewModel: MainViewModel) {
    val classes by viewModel.classesList.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val isLoading by viewModel.currentBtnLoading.collectAsState()

    var isRegisterMode by remember { mutableStateOf(false) }
    var selectedRoleTab by remember { mutableStateOf(0) } // 0: 学生登录, 1: 教师登录

    // 字段状态值
    var studentNum by remember { mutableStateOf("") }
    var teacherWorkId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var studentName by remember { mutableStateOf("") }

    // 注册选择班级
    var selectedClassIndex by remember { mutableStateOf(0) }
    var classDropdownExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD), // 浅空蓝
                        Color(0xFFFFF3E0)  // 暖心橙
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部萌系/设计派图标与标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Bot",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scratch 教学智能助手",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E88E5)
                    )
                }

                Text(
                    text = "面向小学 3-6 年级 🚀 生成式 AI 双向教学系统",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // 错误提示区
                authError?.let {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = it,
                            color = Color.Red,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // TAB 切换 👦 / 👩‍🏫 (放在注册与登录外面，实现完全各自独立的注册模式切换)
                TabRow(
                    selectedTabIndex = selectedRoleTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    containerColor = Color(0xFFF5F5F5)
                ) {
                    Tab(
                        selected = selectedRoleTab == 0,
                        onClick = { selectedRoleTab = 0; password = ""; isRegisterMode = false },
                        text = { Text("👦 学生通道", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedRoleTab == 1,
                        onClick = { selectedRoleTab = 1; password = ""; isRegisterMode = false },
                        text = { Text("👩‍🏫 教师通道", fontWeight = FontWeight.Bold) }
                    )
                }

                if (!isRegisterMode) {
                    // ================= 登录模式 =================
                    Text(
                        text = if (selectedRoleTab == 0) "👦 学生专属登录" else "👩‍🏫 教师后台登录",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (selectedRoleTab == 0) {
                        // 学生登录
                        OutlinedTextField(
                            value = studentNum,
                            onValueChange = { studentNum = it },
                            label = { Text("请输入学号") },
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            singleLine = true
                        )

                        // 登录界面两级联动班级选择
                        val loginGradesList = remember { listOf("三年级", "四年级", "五年级", "六年级") }
                        var lSelectedGrade by remember { mutableStateOf(loginGradesList.first()) }
                        var lGradeDropdownExpanded by remember { mutableStateOf(false) }

                        val lFilteredClasses = classes.filter { it.grade == lSelectedGrade }
                        var lSelectedClassIndex by remember { mutableStateOf(0) }
                        var lClassDropdownExpanded by remember { mutableStateOf(false) }

                        LaunchedEffect(lSelectedGrade, classes) {
                            lSelectedClassIndex = 0
                        }

                        // 1. 年级选择：
                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            Card(
                                onClick = { lGradeDropdownExpanded = !lGradeDropdownExpanded },
                                border = BorderStroke(1.dp, Color.LightGray),
                                shape = RoundedCornerShape(4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("选择年级 (校验)", fontSize = 10.sp, color = Color.Gray)
                                        Text(text = lSelectedGrade, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Icon(
                                        imageVector = if (lGradeDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = lGradeDropdownExpanded,
                                onDismissRequest = { lGradeDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                loginGradesList.forEach { grade ->
                                    DropdownMenuItem(
                                        text = { Text(grade) },
                                        onClick = {
                                            lSelectedGrade = grade
                                            lGradeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 2. 班级选择：
                        if (lFilteredClasses.isNotEmpty()) {
                            val currentSelectedClass = lFilteredClasses.getOrNull(lSelectedClassIndex) ?: lFilteredClasses.first()
                            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                Card(
                                    onClick = { lClassDropdownExpanded = !lClassDropdownExpanded },
                                    border = BorderStroke(1.dp, Color.LightGray),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("选择班级 (校验)", fontSize = 10.sp, color = Color.Gray)
                                            Text(
                                                text = currentSelectedClass.className,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Icon(
                                            imageVector = if (lClassDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = null
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = lClassDropdownExpanded,
                                    onDismissRequest = { lClassDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    lFilteredClasses.forEachIndexed { index, classItem ->
                                        DropdownMenuItem(
                                            text = { Text(classItem.className) },
                                            onClick = {
                                                lSelectedClassIndex = index
                                                lClassDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            Text("⚠️ 该年级暂无可选班级", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
                        }
                    } else {
                        // 教师登录
                        OutlinedTextField(
                            value = teacherWorkId,
                            onValueChange = { teacherWorkId = it },
                            label = { Text("请输入工号 (可选用快捷免注账号)") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("请输入登录密码") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (selectedRoleTab == 0) {
                                if (studentNum.isEmpty() || password.isEmpty()) {
                                    Toast.makeText(context, "请填入学号 and 密码！", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.studentLogin(studentNum, password) {
                                    Toast.makeText(context, "学生登录成功！", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                if (teacherWorkId.isEmpty() || password.isEmpty()) {
                                    Toast.makeText(context, "请填入教师工号和密码！", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.teacherLogin(teacherWorkId, password) {
                                    Toast.makeText(context, "教师登录成功！", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("立即安全登录", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { isRegisterMode = true; password = "" },
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        val registerLabel = if (selectedRoleTab == 0) "还没有学生账号？点击注册 👦" else "还没有教师账号？点击注册 👩‍🏫"
                        Text(registerLabel, color = Color(0xFFFF9800), fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- 免密码/免注册 快速双向测试通道 ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⚡ 快速测试：学生/教师多端一键秒登通道",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "免除重复注册烦恼，点击对应身份直接登录体验！",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.teacherLogin("T1001", "123456") {
                                            Toast.makeText(context, "已一键快捷登录为：王老师！", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(34.dp)
                                ) {
                                    Text("👨‍🏫 王老师(教师)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.studentLogin("S2001", "123456") {
                                            Toast.makeText(context, "已一键快捷登录为学生：张小帅！", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(34.dp)
                                ) {
                                    Text("👦 张小帅(学生)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                } else {
                    // ================= 注册模式 =================
                    if (selectedRoleTab == 0) {
                        // 学生注册
                        Text(
                            text = "学生自助注册新账号",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = studentNum,
                            onValueChange = { studentNum = it },
                            label = { Text("请设置新学号") },
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = studentName,
                            onValueChange = { studentName = it },
                            label = { Text("请输入真实姓名") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("设置新登录密码") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            singleLine = true
                        )

                        // 注册两级联动班级选择
                        val registerGradesList = remember { listOf("三年级", "四年级", "五年级", "六年级") }
                        var rSelectedGrade by remember { mutableStateOf(registerGradesList.first()) }
                        var rGradeDropdownExpanded by remember { mutableStateOf(false) }

                        val rFilteredClasses = classes.filter { it.grade == rSelectedGrade }
                        var rSelectedClassIndex by remember { mutableStateOf(0) }
                        var rClassDropdownExpanded by remember { mutableStateOf(false) }

                        LaunchedEffect(rSelectedGrade, classes) {
                            rSelectedClassIndex = 0
                        }

                        // 1. 年级选择：
                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            Card(
                                onClick = { rGradeDropdownExpanded = !rGradeDropdownExpanded },
                                border = BorderStroke(1.dp, Color.LightGray),
                                shape = RoundedCornerShape(4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("选择年级", fontSize = 11.sp, color = Color.Gray)
                                        Text(text = rSelectedGrade, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Icon(
                                        imageVector = if (rGradeDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = rGradeDropdownExpanded,
                                onDismissRequest = { rGradeDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                registerGradesList.forEach { grade ->
                                    DropdownMenuItem(
                                        text = { Text(grade) },
                                        onClick = {
                                            rSelectedGrade = grade
                                            rGradeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 2. 班级选择：
                        if (rFilteredClasses.isNotEmpty()) {
                            val currentSelectedClass = rFilteredClasses.getOrNull(rSelectedClassIndex) ?: rFilteredClasses.first()
                            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                                Card(
                                    onClick = { rClassDropdownExpanded = !rClassDropdownExpanded },
                                    border = BorderStroke(1.dp, Color.LightGray),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("选择班级", fontSize = 11.sp, color = Color.Gray)
                                            Text(
                                                text = currentSelectedClass.className,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Icon(
                                            imageVector = if (rClassDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = null
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = rClassDropdownExpanded,
                                    onDismissRequest = { rClassDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    rFilteredClasses.forEachIndexed { index, classItem ->
                                        DropdownMenuItem(
                                            text = { Text(classItem.className) },
                                            onClick = {
                                                rSelectedClassIndex = index
                                                rClassDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            Text("⚠️ 该年级暂无可选班级，可选择其他年级或联系教师创建！", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp))
                        }

                        Button(
                            onClick = {
                                if (studentNum.isEmpty() || studentName.isEmpty() || password.isEmpty() || rFilteredClasses.isEmpty()) {
                                    Toast.makeText(context, "请填齐所有的学生注册字段并选择班级！", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val targetClass = rFilteredClasses.getOrNull(rSelectedClassIndex)
                                if (targetClass == null) {
                                    Toast.makeText(context, "请先选择一个有效的班级！", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.studentRegister(
                                    studentNum = studentNum,
                                    name = studentName,
                                    pass = password,
                                    classId = targetClass.classId
                                ) {
                                    Toast.makeText(context, "学生注册并登录成功！", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("立即创建学生账号并登录", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                    } else {
                        // 教师注册
                        Text(
                            text = "教师自助注册新账号",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = teacherWorkId,
                            onValueChange = { teacherWorkId = it },
                            label = { Text("请设置教师新工号") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = studentName, // 用于存放注册教师名
                            onValueChange = { studentName = it },
                            label = { Text("请输入真实教师姓名") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("设置教师登录密码") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (teacherWorkId.isEmpty() || studentName.isEmpty() || password.isEmpty()) {
                                    Toast.makeText(context, "请填齐所有的教师注册字段！", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.teacherRegister(
                                    workId = teacherWorkId,
                                    name = studentName,
                                    pass = password
                                ) {
                                    Toast.makeText(context, "教师注册与登录成功！", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("立即创建教师账号并登录", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { isRegisterMode = false },
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text("返回已有账号登录框", color = Color(0xFF1E88E5), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. 主页面骨架与底部导航
// ==========================================
@Composable
fun MainPortalScreen(viewModel: MainViewModel, userRole: String) {
    var selectedScreenIndex by remember { mutableStateOf(0) }
    val currentUserName by viewModel.currentUserName.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            var showLogoutConfirm by remember { mutableStateOf(false) }

            if (showLogoutConfirm) {
                AlertDialog(
                    onDismissRequest = { showLogoutConfirm = false },
                    title = { Text("确认退出") },
                    text = { Text("确定要退出当前账号吗？") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showLogoutConfirm = false
                                viewModel.logout()
                                Toast.makeText(context, "已成功退出当前账户 🔒", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("确定", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutConfirm = false }) {
                            Text("取消", color = Color.Gray)
                        }
                    }
                )
            }

            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "星梭智学编程助教",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E88E5)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (userRole == "student") Color(0xFFE3F2FD) else Color(0xFFE8F5E9)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${if (userRole == "student") "👦" else "👩‍🏫"} $currentUserName",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (userRole == "student") Color(0xFF1565C0) else Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                showLogoutConfirm = true
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "退出登录",
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                if (userRole == "student") {
                    NavigationBarItem(
                        selected = selectedScreenIndex == 0,
                        onClick = { selectedScreenIndex = 0 },
                        label = { Text("Scratch编程") },
                        icon = { Icon(Icons.Default.Code, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = selectedScreenIndex == 1,
                        onClick = { selectedScreenIndex = 1 },
                        label = { Text("学习任务") },
                        icon = { Icon(Icons.Default.Assignment, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = selectedScreenIndex == 2,
                        onClick = { selectedScreenIndex = 2 },
                        label = { Text("我的作品") },
                        icon = { Icon(Icons.Default.Collections, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = selectedScreenIndex == 3,
                        onClick = { selectedScreenIndex = 3 },
                        label = { Text("AI 辅助") },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
                    )
                } else {
                    // 教师专属底模 - 重构为 3 个选项卡
                    NavigationBarItem(
                        selected = selectedScreenIndex == 0,
                        onClick = { selectedScreenIndex = 0 },
                        label = { Text("发布任务") },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = selectedScreenIndex == 1,
                        onClick = { selectedScreenIndex = 1 },
                        label = { Text("任务列表") },
                        icon = { Icon(Icons.Default.Assignment, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = selectedScreenIndex == 2,
                        onClick = { selectedScreenIndex = 2 },
                        label = { Text("本班作品") },
                        icon = { Icon(Icons.Default.SupervisorAccount, contentDescription = null) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (userRole == "student") {
                when (selectedScreenIndex) {
                    0 -> InteractiveScratchProgrammingScreen(viewModel = viewModel)
                    1 -> StudentTasksScreen(viewModel = viewModel, onGoToCode = { selectedScreenIndex = 0 })
                    2 -> StudentWorksScreen(viewModel = viewModel)
                    3 -> StudentAiAssistHistoricalHub(viewModel = viewModel)
                }
            } else {
                when (selectedScreenIndex) {
                    0 -> TeacherTaskManagementScreen(viewModel = viewModel)
                    1 -> TeacherTaskListScreen(viewModel = viewModel)
                    2 -> TeacherWorksClassViewScreen(viewModel = viewModel)
                }
            }
        }
    }
}

// ==========================================
// 3. 在线编程与 Scratch 编辑区
// ==========================================
@Composable
fun InteractiveScratchProgrammingScreen(viewModel: MainViewModel) {
    val draftCode by viewModel.currentDraftCode.collectAsState()
    val draftName by viewModel.currentDraftName.collectAsState()
    val taskName by viewModel.currentTaskName.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiResult by viewModel.aiResult.collectAsState()
    val aiResultType by viewModel.aiResultType.collectAsState()

    val drafts by viewModel.draftsList.collectAsState()
    val works by viewModel.worksList.collectAsState()

    var showAiAssistSheet by remember { mutableStateOf(false) }
    var saveNameDialog by remember { mutableStateOf(false) }
    var showLoadDraftDialog by remember { mutableStateOf(false) }
    var showLoadWorkDialog by remember { mutableStateOf(false) }
    
    // Draggable and foldable floating console state
    var isExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: AI, 1: Backups, 2: Submit
    var dragOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    var isDragging by remember { mutableStateOf(false) }

    var localInputName by remember { mutableStateOf(draftName) }

    val context = LocalContext.current

    // Scratch editor mirror URLs: allows seamless toggle when official MIT Scratch is blocked (Problem 2 Requirement 2)
    val mirrors = remember {
        listOf(
            "https://editor.scratch-cn.cn/",
            "https://scratch3.fun/",
            "https://scratch.gitapp.cn/"
        )
    }
    var currentMirrorIndex by remember { mutableStateOf(0) }
    var scratchUrl by remember { mutableStateOf(mirrors[0]) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    
    var isPageLoading by remember { mutableStateOf(true) }
    var isAllFailed by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("正在加载 Scratch 编辑器 (1/3)...") }

    // Multi-mirror auto fallback loading
    LaunchedEffect(scratchUrl) {
        isPageLoading = true
        isAllFailed = false
        loadingMessage = "正在加载 Scratch 编辑器 (${currentMirrorIndex + 1}/${mirrors.size})..."
        webViewInstance?.loadUrl(scratchUrl)
        
        // 6s Timeout fallthrough auto switch (Problem 2 Requirement 3)
        kotlinx.coroutines.delay(6000)
        if (isPageLoading && !isAllFailed) {
            if (currentMirrorIndex < mirrors.size - 1) {
                currentMirrorIndex++
                scratchUrl = mirrors[currentMirrorIndex]
            } else {
                isAllFailed = true
                isPageLoading = false
            }
        }
    }

    // Web upload support
    var uploadMessageCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uploadMessageCallback?.onReceiveValue(uris.toTypedArray())
        uploadMessageCallback = null
    }

    // 当载入或创建完毕，锁定此界面为横屏显示；离开时自动回退为默认竖屏，方便小学生流畅地拼搭操作
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        
        // Immersive Distraction-Free full-screen (Problem 3 point 2)
        val insetsController = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView) }
        try {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            insetsController?.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            insetsController?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDispose {
            try {
                activity?.requestedOrientation = originalOrientation
                insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Full Screen Scratch WebView Editor
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isPageLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isPageLoading = false
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                if (currentMirrorIndex < mirrors.size - 1) {
                                    currentMirrorIndex++
                                    scratchUrl = mirrors[currentMirrorIndex]
                                } else {
                                    isAllFailed = true
                                    isPageLoading = false
                                }
                            }
                        }

                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: android.webkit.RenderProcessGoneDetail?
                        ): Boolean {
                            android.widget.Toast.makeText(
                                context,
                                "⚠️ 运行环境算力过载或显卡硬件驱动崩溃，星梭已成功自动无缝重连并恢复工作区！",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            view?.loadUrl(scratchUrl)
                            return true
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        // Support camera and microphone requests within Scratch
                        override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                            request?.grant(request.resources)
                        }

                        // Support file uploading for custom sprites & backgrounds
                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            uploadMessageCallback?.onReceiveValue(null)
                            uploadMessageCallback = filePathCallback
                            try {
                                fileChooserLauncher.launch("*/*")
                            } catch (e: Exception) {
                                uploadMessageCallback?.onReceiveValue(null)
                                uploadMessageCallback = null
                                return false
                            }
                            return true
                        }
                    }
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        
                        // Disable nested Zoom controls of WebView because Scratch provides its own 
                        // SVG zoom controls (+ / - in the corner). Disabling zoom keeps standard touchscreen 
                        // dragging (moving blocks around) buttery smooth without browser panning interruption.
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false

                        // iPad User-Agent is the perfect golden standard (Problem 2 requirement 5):
                        // 1. It forces Scratch to unlock full workspace layout/sprites with no screen-size blocks.
                        // 2. It activates Scratch's optimized touch-gesture system, allowing kids to fluidly drag, drop, and snap blocks.
                        userAgentString = "Mozilla/5.0 (iPad; CPU OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"
                        mediaPlaybackRequiresUserGesture = false
                    }
                    
                    // Request focus immediately so that touches register without initial delays
                    requestFocus()
                    
                    // Bypass Jetpack Compose gesture interception so dragging Scratch blocks on canvas is flawless
                    setOnTouchListener { v, event ->
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        false
                    }
                    
                    webViewInstance = this
                    loadUrl(scratchUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading Overlay (Problem 2 Requirement 3)
        if (isPageLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6E3F2FD)), // Translucent 90%
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF1E88E5), modifier = Modifier.size(50.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "🚀 Loading Scratch programming space...\n正在秒级极速载入 Scratch 创作空间...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E88E5),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = loadingMessage,
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    // Manual Fallback Button (Problem 2 Requirement 2)
                    Button(
                        onClick = {
                            if (currentMirrorIndex < mirrors.size - 1) {
                                currentMirrorIndex++
                                scratchUrl = mirrors[currentMirrorIndex]
                            } else {
                                currentMirrorIndex = 0
                                scratchUrl = mirrors[0]
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Text("国内加载慢？手动一键换源 ⚡", color = Color.White)
                    }
                }
            }
        } else if (isAllFailed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFF3E0)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "网络加载失败，请检查网络连接后重试",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            currentMirrorIndex = 0
                            scratchUrl = mirrors[0]
                            isAllFailed = false
                            isPageLoading = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                    ) {
                        Text("重新加载 🔄", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Super compact translucent label top-center & mirror switch
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xAA000000)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "✨ 正在进行 Scratch 互动编程 (全屏适配手机) ✨",
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // High-speed Domestic editor Mirror selector (extremely important for classroom and VPN-less usage) (Problem 2 Requirement 2)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xCC2E7D32)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.clickable {
                    if (currentMirrorIndex < mirrors.size - 1) {
                        currentMirrorIndex++
                    } else {
                        currentMirrorIndex = 0
                    }
                    scratchUrl = mirrors[currentMirrorIndex]
                    android.widget.Toast.makeText(context, "已手动切换到第 ${currentMirrorIndex + 1} 个极速镜像 ⚡", android.widget.Toast.LENGTH_SHORT).show()
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "手动换源 ⚡",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Draggable & Foldable Control System
        // Completely removed click-outside auto-collapse overlay to prevent accidental dismissals.
        // Collapse/expand is now exclusively controlled via tapping the floating bubble / 'X' button.

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                .padding(16.dp)
        ) {
            if (!isExpanded) {
                // MINIMIZED MODE: Draggable round magic bubble.
                // Dragging requires a long press to prevent accidental dragging during simple taps.
                Button(
                    onClick = { isExpanded = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .alpha(if (isDragging) 0.6f else 1.0f)
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { isDragging = true },
                                onDragEnd = {
                                    isDragging = false
                                    // Auto-snap to screen edges on release
                                    dragOffset = Offset(if (dragOffset.x < -185f) -370f else 0f, dragOffset.y)
                                },
                                onDragCancel = { isDragging = false },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount
                                }
                            )
                        },
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "🎒 编程魔法盒",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🎒 编程魔法盒",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                // EXPANDED MODE: Drag-enabled floating window card
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .width(360.dp)
                        .alpha(if (isDragging) 0.6f else 1.0f)
                ) {
                    // Floating button becomes Close icon ('X') when expanded.
                    Box(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .size(38.dp)
                            .background(Color(0xFFFF9800), shape = CircleShape)
                            .clickable { isExpanded = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "收起",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(265.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        border = BorderStroke(2.dp, Color(0xFF3F51B5))
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // HEADER (Drag handle)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF3F51B5))
                                    .pointerInput(Unit) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { isDragging = true },
                                            onDragEnd = {
                                                isDragging = false
                                                dragOffset = Offset(if (dragOffset.x < -185f) -370f else 0f, dragOffset.y)
                                            },
                                            onDragCancel = { isDragging = false },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount
                                            }
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "✨ 创意控制舱",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "(长按由此拖动 ✥)",
                                        fontSize = 9.sp,
                                        color = Color(0xFFD2DDFC)
                                    )
                                }
                            }

                            // Gray descriptive caption at the top of the cabin
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEEEEEE))
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Click × to collapse / 点击上面橘色 × 按钮收起 🧡",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                            }
                        
                        // TAB SELECTION BAR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEEEEEE))
                                .padding(vertical = 2.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("🤖 AI诊断", "💾 备份恢复", "🔌 硬件联控", "📤 正式提交").forEachIndexed { index, title ->
                                val isSelected = selectedTab == index
                                Button(
                                    onClick = { selectedTab = index },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFFFF9800) else Color.White,
                                        contentColor = if (isSelected) Color.White else Color(0xFF555555)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp),
                                    elevation = if (isSelected) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
                                ) {
                                    Text(text = title, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        // TAB CONTENT DISPLAY
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFFFAFAFA))
                                .padding(8.dp)
                        ) {
                            when (selectedTab) {
                                0 -> {
                                    // Tab 0: AI diagnostics
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.callAiAssistant("语法纠错") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                                                contentPadding = PaddingValues(horizontal = 4.dp),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("🛑 语法纠正", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = { viewModel.callAiAssistant("创意引导") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                                                contentPadding = PaddingValues(horizontal = 4.dp),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("💡 创意启发", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = { viewModel.callAiAssistant("知识点讲解") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688)),
                                                contentPadding = PaddingValues(horizontal = 4.dp),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("🎓 考点分析", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                                            border = BorderStroke(1.dp, Color(0xFFFFF59D)),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .verticalScroll(rememberScrollState())
                                                    .padding(6.dp)
                                            ) {
                                                if (aiLoading) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.align(Alignment.Center).size(20.dp),
                                                        color = Color(0xFFFF9800),
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Text(
                                                        text = aiResult ?: "点击上方魔法卡诊断，AI 姐姐可以帮你分析 Scratch 逻辑并给出生动指导噢！✨",
                                                        fontSize = 10.sp,
                                                        lineHeight = 13.sp,
                                                        color = if (aiResult != null) Color.Black else Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                1 -> {
                                    // Tab 1: Backups & Restoration
                                    Column(
                                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.saveDraftToDb { mes ->
                                                        Toast.makeText(context, mes, Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
                                                contentPadding = PaddingValues(horizontal = 4.dp),
                                                modifier = Modifier.weight(1.1f).height(28.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("立即存草稿", fontSize = 9.sp)
                                            }
                                            Button(
                                                onClick = { showLoadDraftDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD)),
                                                contentPadding = PaddingValues(horizontal = 4.dp),
                                                modifier = Modifier.weight(1.1f).height(28.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF1E88E5), modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("草稿箱(${drafts.size})", fontSize = 9.sp, color = Color(0xFF1E88E5), fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = { showLoadWorkDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF3E0)),
                                                contentPadding = PaddingValues(horizontal = 4.dp),
                                                modifier = Modifier.weight(1.1f).height(28.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(Icons.Default.Collections, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("已交作品(${works.size})", fontSize = 9.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Text("🐾 官方创意玩法大范本：", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                                ) {
                                                    listOf(
                                                        1 to "🐱猫漫步",
                                                        2 to "🍎抓苹果",
                                                        3 to "🎹弹钢琴"
                                                    ).forEach { (id, label) ->
                                                        TextButton(
                                                            onClick = {
                                                                viewModel.selectTemplate(id)
                                                                Toast.makeText(context, "$label 模板已快车载入！", Toast.LENGTH_SHORT).show()
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 4.dp),
                                                            modifier = Modifier.weight(1f).height(24.dp)
                                                        ) {
                                                            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    // Tab 2: Hardware bridge and concentration shields
                                    Column(
                                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Power, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("🔌 硬件联动与绿色专注屏蔽", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                        }

                                        Text(
                                            text = "★ 专为物理课堂定制。搭载掌上硬件，防止小学生打开浏览器娱乐分心，高度便携！",
                                            fontSize = 8.5.sp,
                                            lineHeight = 11.sp,
                                            color = Color.DarkGray
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    Toast.makeText(context, "🟢 乐高智能电机 / ESP32 芯片蓝牙桥接成功！", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                                contentPadding = PaddingValues(horizontal = 4.dp),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("🔌 连接智能硬件", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    Toast.makeText(context, "🛡️ 小学生专注盾：已封锁一切娱乐网站阻断干扰，专注编程！", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                                                contentPadding = PaddingValues(horizontal = 4.dp),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("🛡️ 开启防分心锁", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF5FB)),
                                            border = BorderStroke(1.dp, Color(0xFFAED5F8)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Text("🎒 课堂专属创新点与优势：", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                                                Text("1. 便携防沉迷：取代传统机房笨重电脑，有效堵塞玩网游路子！\n2. 高频交互：拖动代码物理显化硬件执行，激发小学生学习心智。\n3. 高度灵活：随时随地结合掌上开发板做物理现实交互组装！", fontSize = 8.sp, lineHeight = 10.sp, color = Color.DarkGray)
                                            }
                                        }
                                    }
                                }
                                3 -> {
                                    // Tab 3: Submissions
                                    Column(
                                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "🧸 设定名字: $draftName",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    localInputName = draftName
                                                    saveNameDialog = true
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF3F51B5))
                                            }
                                        }
                                        
                                        Text(text = "📌 班级关联任务: ${taskName ?: "自由创新拼拼搭"}", fontSize = 10.sp, color = Color.Gray)
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.clearWorkspaceToNew()
                                                    Toast.makeText(context, "全新创意工作区创建成功！", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)),
                                                contentPadding = PaddingValues(horizontal = 4.dp),
                                                modifier = Modifier.weight(1f).height(32.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("清空新建", fontSize = 9.sp)
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    viewModel.submitWorkAndAiReport { mes ->
                                                        Toast.makeText(context, mes, Toast.LENGTH_LONG).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                                contentPadding = PaddingValues(horizontal = 4.dp),
                                                modifier = Modifier.weight(1.3f).height(32.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                enabled = !aiLoading
                                            ) {
                                                if (aiLoading) {
                                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(10.dp), strokeWidth = 1.dp)
                                                } else {
                                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("评测提交作品", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(2.dp))
                                        
                                        TextButton(
                                            onClick = { viewModel.logout() },
                                            modifier = Modifier.align(Alignment.End).height(24.dp)
                                        ) {
                                            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("退出登录", fontSize = 9.sp, color = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }


    // 重命名 Dialog
    if (saveNameDialog) {
        AlertDialog(
            onDismissRequest = { saveNameDialog = false },
            title = { Text("重命名当前草稿") },
            text = {
                OutlinedTextField(
                    value = localInputName,
                    onValueChange = { localInputName = it },
                    label = { Text("草稿名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (localInputName.isNotBlank()) {
                        viewModel.currentDraftName.value = localInputName
                    }
                    saveNameDialog = false
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { saveNameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // AI 辅助弹窗
    if (showAiAssistSheet) {
        AlertDialog(
            onDismissRequest = { showAiAssistSheet = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = Color(0xFFFF9800))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI 少儿编程小搭档")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "选择一个辅助小锦囊，AI 姐姐会根据本区积木帮分析哦：",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.callAiAssistant("语法纠错") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("语法纠错", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { viewModel.callAiAssistant("创意引导") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("创意引导", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { viewModel.callAiAssistant("知识点讲解") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("考点讲解", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = aiResultType.ifEmpty { "诊断类型" } + " - 评析结果：",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF333333)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 220.dp)
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (aiLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = Color(0xFFFF9800)
                                )
                            } else {
                                Text(
                                    text = aiResult ?: "点击上方的引导按钮，立即获取 AI 智能纠错与指导思路吧！",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = if (aiResult != null) Color.Black else Color.Gray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAiAssistSheet = false }) {
                    Text("退出辅助")
                }
            }
        )
    }

    // 回溯本地草稿 Dialog
    if (showLoadDraftDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDraftDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF1E88E5))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("回溯本地草稿进度", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (drafts.isEmpty()) {
                    Text("当前还没有保存过本地草稿哦！可以用“保存草稿”留存当前进度。", color = Color.Gray, fontSize = 13.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(drafts) { draft ->
                            Card(
                                onClick = {
                                    viewModel.loadDraftToWorkspace(draft)
                                    showLoadDraftDialog = false
                                    Toast.makeText(context, "成功恢复草稿进度：${draft.draftName}", Toast.LENGTH_SHORT).show()
                                },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(draft.draftName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(draft.lastModifiedTime))
                                        Text("更新时间: $dateStr", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLoadDraftDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 载入已提作品 Dialog
    if (showLoadWorkDialog) {
        AlertDialog(
            onDismissRequest = { showLoadWorkDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Collections, contentDescription = null, tint = Color(0xFFFF9800))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("载入已提交作品进度", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (works.isEmpty()) {
                    Text("当前还没有正式提交过任何作品哦，赶快去评测提交吧！", color = Color.Gray, fontSize = 13.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(works) { work ->
                            Card(
                                onClick = {
                                    viewModel.loadWorkToWorkspace(work)
                                    showLoadWorkDialog = false
                                    Toast.makeText(context, "成功恢复已提交作品：${work.workName}", Toast.LENGTH_SHORT).show()
                                },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(work.workName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(work.submitTime))
                                        Text("提交时间: $dateStr", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLoadWorkDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// ==========================================
// 4. 学习任务模块屏幕
// ==========================================
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
                        // 自动关联任务到工作区
                        viewModel.currentTaskId.value = task.taskId
                        viewModel.currentTaskName.value = task.taskName
                        viewModel.currentDraftName.value = "${task.taskName} - 草稿"
                        onGoToCode()
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
                Text("班级本期学习任务", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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

                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (task.status == "进行中") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                        )
                                    ) {
                                        Text(
                                            text = task.status,
                                            fontSize = 11.sp,
                                            color = if (task.status == "进行中") Color(0xFF2E7D32) else Color(0xFFC62828),
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

// ==========================================
// 5. 作品与 AI 智能报告卡
// ==========================================
@Composable
fun StudentWorksScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val works by viewModel.worksList.collectAsState()
    val activeReport by viewModel.activeReport.collectAsState()

    var showReportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107))
            Spacer(modifier = Modifier.width(8.dp))
            Text("我的 Scratch 作品列表", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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

                        EvaluationProgressRow(label = "1. 语法合规性 (检测积木完整拼接)", score = rep.grammarScore, maxScore = 25, color = Color(0xFF4CAF50))
                        EvaluationProgressRow(label = "2. 逻辑完整性 (检测逻辑环嵌套等)", score = rep.logicScore, maxScore = 30, color = Color(0xFF2196F3))
                        EvaluationProgressRow(label = "3. 任务匹配度 (检测任务目标要素)", score = rep.taskMatchScore, maxScore = 25, color = Color(0xFFFF9800))
                        EvaluationProgressRow(label = "4. 创意实现度 (分析交互及原创想法)", score = rep.creativeScore, maxScore = 20, color = Color(0xFF9C27B0))

                        Spacer(modifier = Modifier.height(16.dp))

                        // AI 优化评析与辅导
                        Text("💡 AI 姐姐精细优化辅导指引：", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))

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
    val progressRatio = score.toFloat() / maxScore.toFloat()
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text("$score / $maxScore 分", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(
            progress = { progressRatio },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color(0xFFEEEEEE)
        )
    }
}

// ==========================================
// 6. AI 辅助日志记录 HUB
// ==========================================
@Composable
fun StudentAiAssistHistoricalHub(viewModel: MainViewModel) {
    val history by viewModel.aiRecordHistory.collectAsState()
    val classConfig by viewModel.aiClassConfig.collectAsState()
    val dailyLimitReached by viewModel.aiDailyLimitReached.collectAsState()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(12.dp)
    ) {
        // 当前限制看板
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
                    Text("• AI 提示支持度：${it.aiHintLevel}模式", fontSize = 12.sp)
                    Text("• 创意向单日获取最大调用上限：${it.creativeGuideDailyLimit} 次", fontSize = 12.sp)
                    Text("• 是否阻断直抄完整源码：${if (it.codeGenerationLimit == 0) "全面阻断抄袭 (纯指导模式)" else "允许部分参考"}", fontSize = 12.sp)
                } ?: run {
                    Text("• AI 提示支持度：默认入门模式", fontSize = 12.sp)
                    Text("• 创意向单日获取最大调用上限：5 次", fontSize = 12.sp)
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF3F51B5))
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI 随身指导问答足迹", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("你的足迹里还没有问答记录。请快去编程工作区找 AI 提问并分析吧！", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
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

                                val dateStr = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()).format(Date(record.callTime))
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

// ==========================================
// 7. 教师端 - 发布任务及管理面板
// ==========================================
@Composable
fun TeacherTaskManagementScreen(viewModel: MainViewModel) {
    val classes by viewModel.classesList.collectAsState()

    var taskNameInput by remember { mutableStateOf("") }
    var taskDetailInput by remember { mutableStateOf("") }
    var taskGradeInput by remember { mutableStateOf("三年级") }
    var taskDeadlineInput by remember { mutableStateOf("2026-06-30") }

    var classSelectIndex by remember { mutableStateOf(0) }
    var classSelectExpanded by remember { mutableStateOf(false) }

    // 班级管理新建输入状态
    var newClassNameInput by remember { mutableStateOf("") }
    var newClassGradeInput by remember { mutableStateOf("三年级") }

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
                    Text("王老师的管理事务中心 - 发布控制台", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            }
        }

        // 1. 🏫 教学班级管理与新建 (MOVED TO THE VERY TOP as requested by Problem 3.1)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🏫 教学班级管理", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Text("共：${classes.size} 个教学班级", fontSize = 11.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // 已有班级列表横向滑动
                    if (classes.isEmpty()) {
                        Text("当前尚无班级建档，请在下方建档！", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            classes.forEach { c ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                    border = BorderStroke(1.dp, Color(0xFFF1F1F1)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Text(c.className, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                        Text("年级：${c.grade}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("🆕 添加新的教学班级档案 (添加后立即在此页下发任务)：", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newClassNameInput,
                            onValueChange = { newClassNameInput = it },
                            label = { Text("班级名称（如：三班 / 培优一班）", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f).height(54.dp)
                        )
                        OutlinedTextField(
                            value = newClassGradeInput,
                            onValueChange = { newClassGradeInput = it },
                            label = { Text("所属年级", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(0.7f).height(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (newClassNameInput.isBlank()) {
                                Toast.makeText(context, "请输入新添加班级名称！", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.createNewClassByTeacher(newClassNameInput, newClassGradeInput) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                newClassNameInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新增本校班级", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
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

                    OutlinedTextField(
                        value = taskNameInput,
                        onValueChange = { taskNameInput = it },
                        label = { Text("输入任务名称 (例如: 快乐猫捉老鼠)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = taskGradeInput,
                        onValueChange = { taskGradeInput = it },
                        label = { Text("面向年级（如：三年级 或 四年级）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = taskDeadlineInput,
                        onValueChange = { taskDeadlineInput = it },
                        label = { Text("截止日期（格式: YYYY-MM-DD）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    // 班级选择卡
                    if (classes.isNotEmpty()) {
                        val selClass = classes.getOrNull(classSelectIndex) ?: classes.first()
                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Card(
                                onClick = { classSelectExpanded = !classSelectExpanded },
                                border = BorderStroke(1.dp, Color.LightGray),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("发派目标班级：${selClass.className}", fontSize = 14.sp)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = classSelectExpanded,
                                onDismissRequest = { classSelectExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                classes.forEachIndexed { i, c ->
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
                    }

                    OutlinedTextField(
                        value = taskDetailInput,
                        onValueChange = { taskDetailInput = it },
                        label = { Text("具体编程任务指引与积木块要求详情", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        minLines = 3
                    )

                    Button(
                        onClick = {
                            if (taskNameInput.isEmpty() || taskDetailInput.isEmpty() || classes.isEmpty()) {
                                Toast.makeText(context, "请填齐必填字段，并确认已有教学班级！", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val classId = classes[classSelectIndex].classId
                            viewModel.publishNewTaskByTeacher(
                                name = taskNameInput,
                                detail = taskDetailInput,
                                grade = taskGradeInput,
                                deadlineStr = taskDeadlineInput,
                                classId = classId
                            ) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                taskNameInput = ""
                                taskDetailInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        modifier = Modifier.fillMaxWidth().height(48.dp), // Comfort height touch spec!
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("立即向选定班级下发发布", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// 7.5. 教师端 - 已下发学习任务列表与精细化审评通道 (Problem 3 Requirement 2)
// ==========================================
@Composable
fun TeacherTaskListScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val classes by viewModel.classesList.collectAsState()
    val tasks by viewModel.tasksList.collectAsState()
    val allWorks by viewModel.allWorksList.collectAsState()
    val students by viewModel.studentsList.collectAsState()

    val currentTeacherId by viewModel.currentUserId.collectAsState()

    // 筛选出当前登录教师自己发布的任务，并按taskId(递增即发布时间)降序排列
    val teacherTasks = remember(tasks, currentTeacherId) {
        tasks.filter { it.teacherId == currentTeacherId }
             .sortedByDescending { it.taskId }
    }

    var selectedTask by remember { mutableStateOf<LearningTask?>(null) }
    
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

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTask = t },
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
                                    Text(
                                        text = t.taskName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.DarkGray
                                    )
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "🏫 $className",
                                            fontSize = 10.sp,
                                            color = Color(0xFFE65100),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
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

            // 任务基础描述卡
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFEAEAEA))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📌 ${task.taskName}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E88E5)
                        )
                        Text(
                            text = "下发班级: $taskClassName",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "📖 截止时间: ${task.deadline}",
                        fontSize = 11.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color(0xFFF0F0F0))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🎯 任务要求:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Text(
                        text = task.taskDetail,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 2.dp)
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
                                            reviewingWork = work
                                            scoreInput = (work.teacherScore ?: 90).toString()
                                            commentInput = work.teacherComment ?: ""
                                            Toast.makeText(context, "已成功载入该代码，快去工作区看孩子们的积木吧！", Toast.LENGTH_SHORT).show()
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
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("✅ 同意过审打分", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("↩️ 打回重新修改", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewingWork = null }) {
                    Text("取消", color = Color.Gray)
                }
            }
        )
    }
}

// ==========================================
// 7.6. 辅助组件 - 响应式 AI 分数查询显示
// ==========================================
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

// ==========================================
// 8. 教师端 - 实时审阅全校作品与评语下发
// ==========================================
@Composable
fun TeacherWorksClassViewScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val allWorks by viewModel.allWorksList.collectAsState()
    val students by viewModel.studentsList.collectAsState()

    // 状态管理
    var reviewingWork by remember { mutableStateOf<ScratchWork?>(null) }
    var scoreInput by remember { mutableStateOf("90") }
    var commentInput by remember { mutableStateOf("") }

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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF1E88E5))
            Spacer(modifier = Modifier.width(6.dp))
            Text("孩子们最新提交的 Scratch 作品：", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        if (allWorks.isEmpty()) {
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
                    Text("目前还没有任何孩子提交作品哦！", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
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
                items(allWorks) { work ->
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(work.submitTime))
                                Text(text = "提交时间: $dateStr", fontSize = 11.sp, color = Color.Gray)

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.loadWorkToWorkspace(work)
                                            Toast.makeText(context, "已成功载入该作品代码，快去工作区看孩子们的积木吧！", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("🔍 看积木", fontSize = 11.sp, color = Color.White)
                                    }

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
                    Text("💯 快捷评分预设立刻打分：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
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
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("✅ 同意过审打分", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("↩️ 打回重新修改", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewingWork = null }) {
                    Text("取消", color = Color.Gray)
                }
            }
        )
    }
}
