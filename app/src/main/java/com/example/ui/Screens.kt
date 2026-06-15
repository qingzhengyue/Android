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

            Column {
                if (!(userRole == "student" && selectedScreenIndex == 0)) {
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
                }

                if (userRole == "student") {
                    StudentHorizontalTabBar(
                        selectedScreenIndex = selectedScreenIndex,
                        onTabSelected = { selectedScreenIndex = it }
                    )
                }
            }
        },
        bottomBar = {
            if (userRole != "student") {
                NavigationBar(containerColor = Color.White) {
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
                    NavigationBarItem(
                        selected = selectedScreenIndex == 3,
                        onClick = { selectedScreenIndex = 3 },
                        label = { Text("班级管理") },
                        icon = { Icon(Icons.Default.Class, contentDescription = null) }
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
                    0 -> InteractiveScratchProgrammingScreen(viewModel = viewModel, onBackToHall = { selectedScreenIndex = 1 })
                    1 -> StudentTasksScreen(viewModel = viewModel, onGoToCode = { selectedScreenIndex = 0 })
                    2 -> StudentWorksScreen(viewModel = viewModel)
                    3 -> StudentAiAssistHistoricalHub(viewModel = viewModel)
                }
            } else {
                when (selectedScreenIndex) {
                    0 -> TeacherTaskManagementScreen(viewModel = viewModel)
                    1 -> TeacherTaskListScreen(viewModel = viewModel)
                    2 -> TeacherWorksClassViewScreen(viewModel = viewModel)
                    3 -> TeacherClassManagementUnifiedScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun StudentHorizontalTabBar(
    selectedScreenIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        color = Color(0xFF1A237E),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                "Scratch编程" to Icons.Default.Code,
                "学习任务" to Icons.Default.Assignment,
                "我的作品" to Icons.Default.Collections,
                "AI 辅助" to Icons.Default.AutoAwesome
            )
            tabs.forEachIndexed { index, (title, icon) ->
                val isSelected = selectedScreenIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color(0xFFB0BEC5),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFFB0BEC5)
                            )
                        }
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(0.9f)
                                .height(3.dp)
                                .background(Color.White, shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. 在线编程与 Scratch 编辑区
// ==========================================
@Composable
fun TopBarActionButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    containerColor: Color
) {
    Surface(
        onClick = onClick,
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class TemplateItem(
    val title: String,
    val desc: String,
    val code: String
)

@Composable
fun MagicBoxDrawerPanel(
    webView: WebView?,
    viewModel: MainViewModel,
    onClose: () -> Unit,
    onInsertText: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("运动") }
    var showTemplateDialog by remember { mutableStateOf<TemplateItem?>(null) }
    val context = LocalContext.current

    val categories = listOf("运动", "外观", "声音", "事件", "控制", "侦测", "运算", "变量")

    val blocks = mapOf(
        "运动" to listOf(
            "移动10步" to "让角色在舞台上朝它的朝向移动10步",
            "右转15度" to "将角色顺时针旋转15度",
            "左转15度" to "将角色逆时针旋转15度",
            "移到x:0 y:0" to "让角色精准移到屏幕正中央位置坐标 0,0",
            "碰到边缘就反弹" to "适合做来回折返运动的角色，防止移出或卡死在边缘",
            "面向90度方向" to "调整角色的水平朝向，90度代表面向右侧"
        ),
        "外观" to listOf(
            "说\"你好\"2秒" to "在角色头上悬浮气泡文字说你好2秒钟",
            "显示" to "让处于隐藏状态的角色重新显露在舞台上",
            "隐藏" to "让角色在舞台中隐匿消失，常用于怪物死亡或换幕效果",
            "切换造型为造型1" to "切换并改变角色的动作形态或外观造型",
            "下一个造型" to "按顺序切换为角色的下一套外观动作细节切换",
            "将大小增加10" to "使角色的整体缩放比例增加指定的数值，体积变大"
        ),
        "声音" to listOf(
            "播放声音喵" to "后台播放特定喵叫声，并不阻塞后续积木的继续执行",
            "播放声音喵直到结束" to "完整播放完喵叫音效后，才往后前进执行其他后续积木",
            "停止所有声音" to "瞬间强制关停舞台上正在播放的所有音效"
        ),
        "事件" to listOf(
            "当绿旗被点击" to "整套编程的首要控制起点。点击绿旗后全剧本触发开始",
            "当按下空格键" to "通过实体键盘的空格按压，触发特定行为控制，适合做操控",
            "当角色被点击" to "触控打击交互，让角色在被手指或滑鼠点击时作出响应",
            "当接收到消息1" to "接收跨越角色的群聊广播消息，对消息进行接收反馈触发"
        ),
        "控制" to listOf(
            "重复执行10次" to "在内部代码处产生规定好的10次小范围循环流程",
            "永远" to "创造舞台中的无限运行循环，作为动作更新主线程引擎",
            "如果那么" to "条件判定如果分支。判断是否符合判定条件",
            "等待1秒" to "设置特定的运行时间延迟空挡，调节交互缓冲频次操作",
            "停止全部脚本" to "全面叫停终止一切当前已经拉起运行的动作序列"
        ),
        "侦测" to listOf(
            "碰到鼠标指针？" to "雷达防碰撞判定首选，探知角色此时是否接触了外部指针",
            "碰到颜色红色？" to "常用于物理防墙，当边缘探头遇到极佳的目标色时反弹",
            "鼠标的x坐标" to "获取外部输入物理指针当前在主视窗内的水平轴向像素位置",
            "询问\"你好\"并等待" to "呼出问题询问交互框，让玩家能够从键盘键入文字并回传"
        ),
        "运算" to listOf(
            "1+1" to "两个数值相加。可以放入变量或数值进行数学加法合并运算",
            "1>1" to "大于关系对比判断。若左侧比右侧大则传回为真成立",
            "1<1" to "小于关系对比判断。若左侧比右侧小则传回为真成立",
            "在1和10之间取随机数" to "做掉落率、暴击、随机刷新坐标点时不可或缺的随机数产生积木",
            "连接\"hello\"和\"world\"" to "拼接首尾两段文字。做游戏积分文字展示有极大帮助"
        ),
        "变量" to listOf(
            "将变量设为0" to "将自定义存储游戏数据的变量初始重置数值为 0",
            "变量增加1" to "用于打中怪物、吃得香蕉红苹果等玩乐时的计功累分加一"
        )
    )

    val templates = listOf(
        TemplateItem("🐱 小猫走路", "控制小猫在舞台上左右来回走动，并自动完成基础造型动作切换", "事件 -> 1. 当🟢被点击\n控制 -> 2. 重复执行\n  运动 ->   3. 移动 10 步\n  外观 ->   4. 下一个造型\n  控制 ->   5. 等待 0.1 秒\n  运动 ->   6. 碰到边缘就反弹"),
        TemplateItem("🔨 疯狂打地鼠", "随机坐标点浮现地鼠，点击地鼠播放音效并增加游戏积分", "事件 -> 1. 当🟢被点击\n变量 -> 2. 将 [我的得分] 设为 0\n事件 -> 3. 当角色被点击\n声音 -> 4. 播放声音 (打中)\n变量 -> 5. 将 [我的得分] 增加 1\n外观 -> 6. 隐藏\n事件 -> 7. 当🟢被点击\n控制 -> 8. 重复执行\n  运动 ->   9. 移到 (随机位置)\n  外观 ->   10. 显示\n  控制 ->   11. 等待 1.5 秒\n  外观 ->   12. 隐藏\n  控制 ->   13. 等待 1 秒"),
        TemplateItem("🍎 接住红苹果", "苹果在屏幕上方随机水平坐标产生，重力直向下落，碗若接住则得分", "事件 -> 1. 当🟢被点击\n控制 -> 2. 重复执行\n  运动 ->   3. 移到 x:在 -200 到 200 间随机数 y:180\n  控制 ->   4. 重复执行直到 (y 坐标 < -170)\n    运动 ->     5. 将 y 坐标增加 -5\n    控制 ->     6. 如果 碰到 (小碗) 那么\n      声音 ->       7. 播放声音 (得分)\n      变量 ->       8. 将 [金币] 增加 1\n      控制 ->       9. 退出当前循环"),
        TemplateItem("🏓 弹球小游戏", "小球碰壁反弹，如果滑板没接住小球落入深渊则游戏结束", "事件 -> 1. 当🟢被点击\n运动 -> 2. 面向 45 方向\n控制 -> 3. 重复执行\n  运动 ->   4. 移动 6 步\n  控制 ->   5. 如果 碰到 (滑板) 那么\n    运动 ->     6. 旋转 180 度\n  控制 ->   7. 如果 碰到边缘 那么\n    运动 ->     8. 碰到边缘反弹\n  控制 ->   9. 如果 y 坐标 < -170 那么\n    控制 ->     10. 停止全部"),
        TemplateItem("🌀 趣味走迷宫", "玩家使用方向键操控小人出发，碰到黑色迷宫死胡同墙壁则被弹回起点", "事件 -> 1. 当🟢被点击\n运动 -> 2. 移到 x:-200 y:150\n控制 -> 3. 重复执行\n  控制 ->   4. 如果 按下 (右移) 键 那么\n    运动 ->     5. 将 x 坐标增加 5\n  控制 ->   6. 如果 碰到颜色 (迷宫黑色) 那么\n    运动 ->     7. 移到 x:-200 y:150"),
        TemplateItem("♻️ 垃圾分类助手", "拖动垃圾图案，放入正确的分类箱子加分，分错打回", "事件 -> 1. 当角色被点击\n控制 -> 2. 如果 碰到 (可回收垃圾箱) 那么\n  声音 ->   3. 播放声音 (正确)\n  变量 ->   4. 将 [环保积分] 增加 10\n控制 -> 5. 否则\n  声音 ->   6. 播放声音 (错误)\n  外观 ->   7. 说 放错了哦 1 秒"),
        TemplateItem("🎨 自制魔法画笔", "跟着鼠标画出绚丽图案，轻敲空格按键瞬间清屏重来", "事件 -> 1. 当🟢被点击\n画笔 -> 2. 全部擦除\n控制 -> 3. 重复执行\n  运动 ->   4. 移到 (鼠标指针)\n  控制 ->   5. 如果 按下鼠标 那么\n    画笔 ->     6. 落笔\n  控制 ->   7. 否则\n    画笔 ->     8. 抬笔\n事件 -> 9. 当按下 (空格) 键\n画笔 -> 10. 全部擦除"),
        TemplateItem("🐠 蔚蓝海底世界", "各种大小海底小鱼在大洋深处欢快游来游去，碰到大白鲨就被一口吞掉", "事件 -> 1. 当🟢被点击\n控制 -> 2. 重复执行\n  运动 ->   3. 移动 3 步\n  运动 ->   4. 碰到边缘反弹\n  控制 ->   5. 如果 碰到 (大白鲨) 那么\n    外观 ->     6. 隐藏\n    控制 ->     7. 等待 5 秒\n    外观 ->     8. 显示"),
        TemplateItem("⏰ 守护小闹钟", "后台轮询当前时间，当抵达设定秒数后，欢快响起欢天喜地叫醒曲", "事件 -> 1. 当🟢被点击\n控制 -> 2. 重复执行\n  控制 ->   3. 如果 计时器当前秒 = 30 那么\n    声音 ->     4. 播放声音 (起床歌)\n    控制 ->     5. 等待 1 秒"),
        TemplateItem("🚀 太空陨石机战", "雷霆战机随时按鼠标发射子弹，陨石随机刷新直坠，火爆击碎", "事件 -> 1. 当🟢被点击\n控制 -> 2. 重复执行\n  控制 ->   3. 如果 碰到 (自制激光子弹) 那么\n    特效 ->     4. 播放爆炸动画\n    声音 ->     5. 播放声音 (轰鸣)\n    运动 ->     6. 移到 (随机位置)"),
        TemplateItem("🎙️ 声控高空气球", "灵敏侦测麦克风声音响度大小，声音越高气球在舞台越向上升", "事件 -> 1. 当🟢被点击\n控制 -> 2. 重复执行\n  运动 ->   3. 将 y 坐标设为 (麦克风声音响度 * 2.5)")
    )

    Surface(
        color = Color.White,
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight(),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF303F9F))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("编程魔法盒 🎒", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            var drawerTab by remember { mutableStateOf(0) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8EAF6))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { drawerTab = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (drawerTab == 0) Color(0xFF3F51B5) else Color.White,
                        contentColor = if (drawerTab == 0) Color.White else Color(0xFF3F51B5)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(28.dp)
                ) {
                    Text("常用积木 🧩", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { drawerTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (drawerTab == 1) Color(0xFF3F51B5) else Color.White,
                        contentColor = if (drawerTab == 1) Color.White else Color(0xFF3F51B5)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(28.dp)
                ) {
                    Text("项目模板 📒", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (drawerTab == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(Color(0xFFFAFAFA))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = selectedCategory == cat
                        val catColor = when (cat) {
                            "运动" -> Color(0xFF4C97FF)
                            "外观" -> Color(0xFF9966FF)
                            "声音" -> Color(0xFFCF63CF)
                            "事件" -> Color(0xFFFFBF00)
                            "控制" -> Color(0xFFFFAB19)
                            "侦测" -> Color(0xFF4CBFE6)
                            "运算" -> Color(0xFF59C059)
                            "变量" -> Color(0xFFFF8C1A)
                            else -> Color.Gray
                        }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) catColor else Color(0xFFF1F1F1)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else Color.DarkGray,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Divider(color = Color(0xFFECEFF1))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val currentCategoryBlocks = blocks[selectedCategory] ?: emptyList()
                    items(currentCategoryBlocks.size) { i ->
                        val (blockText, blockDesc) = currentCategoryBlocks[i]
                        val themeColor = when (selectedCategory) {
                            "运动" -> Color(0xFF4C97FF)
                            "外观" -> Color(0xFF9966FF)
                            "声音" -> Color(0xFFCF63CF)
                            "事件" -> Color(0xFFFFBF00)
                            "控制" -> Color(0xFFFFAB19)
                            "侦测" -> Color(0xFF4CBFE6)
                            "运算" -> Color(0xFF59C059)
                            "变量" -> Color(0xFFFF8C1A)
                            else -> Color(0xFF555555)
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                            border = BorderStroke(1.dp, themeColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .background(themeColor, shape = RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = blockText,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "💡 用途：$blockDesc",
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(templates.size) { i ->
                        val template = templates[i]
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBE7)),
                            border = BorderStroke(1.dp, Color(0xFF9E9D24)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTemplateDialog = template }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = template.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF558B2F)
                                    )
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF558B2F), modifier = Modifier.size(12.dp))
                                }
                                Text(
                                    text = template.desc,
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    lineHeight = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTemplateDialog != null) {
        val t = showTemplateDialog!!
        AlertDialog(
            onDismissRequest = { showTemplateDialog = null },
            title = { Text(t.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📦 模板创意描述：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(t.desc, fontSize = 11.sp)
                    Divider()
                    Text("🧩 推荐拼搭积木块顺序：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                            .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(4.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = t.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF333333),
                            lineHeight = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tId = when {
                            t.title.contains("小猫") -> 1
                            t.title.contains("苹果") || t.title.contains("地鼠") -> 2
                            else -> 3
                        }
                        try {
                            val templateCode = viewModel.getTemplateCode(tId)
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Scratch Template Code", templateCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "模板代码已复制到剪贴板！请在Scratch编辑器中点击'文件→从电脑上传'导入", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "复制失败，请重试", Toast.LENGTH_SHORT).show()
                        }
                        showTemplateDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
                ) {
                    Text("复制到剪贴板", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTemplateDialog = null }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
fun InteractiveScratchProgrammingScreen(viewModel: MainViewModel, onBackToHall: () -> Unit) {
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
    
    // Draggable and foldable floating console state (优化一 & 优化二)
    var isExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: AI, 1: Backups, 2: Submit
    val coroutineScope = rememberCoroutineScope()
    val animX = remember { androidx.compose.animation.core.Animatable(0f) }
    val animY = remember { androidx.compose.animation.core.Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isSnapped by remember { mutableStateOf(false) }
    var snappedSide by remember { mutableStateOf("right") }
    var hasInitializedPosition by remember { mutableStateOf(false) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val realTimeCheckEnabled by viewModel.realTimeStateEnabled.collectAsState()
    var scratchChangeCounter by remember { mutableStateOf(0) }

    fun getLiveCodeAndCall(funcType: String) {
        val webView = webViewInstance
        if (webView != null) {
            webView.evaluateJavascript(
                "(function() { " +
                "  try { " +
                "    if (window.vm) { return JSON.stringify(window.vm.toJSON()); } " +
                "    else if (window.scratch && window.scratch.vm) { return JSON.stringify(window.scratch.vm.toJSON()); } " +
                "    else if (typeof Blockly !== 'undefined') { " +
                "         var xml = Blockly.Xml.workspaceToDom(Blockly.mainWorkspace); " +
                "         return Blockly.Xml.domToText(xml); " +
                "    } " +
                "  } catch(e) {} " +
                "  return ''; " +
                "})()"
            ) { result: String? ->
                val cleaned = if (result != null && result != "null" && result != "\"\"") {
                    var s = result.trim()
                    if (s.startsWith("\"") && s.endsWith("\"") && s.length >= 2) {
                        s = s.substring(1, s.length - 1)
                        s = s.replace("\\\"", "\"").replace("\\\\", "\\")
                    }
                    s
                } else ""
                viewModel.callAiAssistant(funcType, if (cleaned.isNotBlank()) cleaned else null)
            }
        } else {
            viewModel.callAiAssistant(funcType)
        }
    }

    LaunchedEffect(scratchChangeCounter) {
        if (realTimeCheckEnabled && scratchChangeCounter > 0) {
            delay(300)
            getLiveCodeAndCall("语法纠错")
        }
    }

    var showMagicBoxDrawer by remember { mutableStateOf(false) }

    var localInputName by remember { mutableStateOf(draftName) }

    val context = LocalContext.current

    val coerceInSafe = remember {
        { value: Float, min: Float, max: Float ->
            if (max < min) min else value.coerceIn(min, max)
        }
    }

    // 首次进入编程界面缩放提示 (优化二)
    LaunchedEffect(Unit) {
        android.widget.Toast.makeText(context, "双指捏合可缩放画布 🔍", android.widget.Toast.LENGTH_LONG).show()
    }

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
            if (activity?.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            insetsController?.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            insetsController?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDispose {
            try {
                if (activity?.requestedOrientation != originalOrientation) {
                    activity?.requestedOrientation = originalOrientation
                }
                insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // High 48dp Top Bar (Status Bar) - Problem 3.3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF1A237E))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Return Button
            Row(
                modifier = Modifier
                    .clickable { onBackToHall() }
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (taskName.isNullOrBlank()) "返回创意空间" else "返回学习任务大厅",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 2. Draft & Task Information
            val displayTaskInfo = if (taskName.isNullOrBlank()) "自由创作" else "学习任务: $taskName"
            Text(
                text = "📦 $draftName [$displayTaskInfo]",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            // 3. 编程魔法盒 Button
            TopBarActionButton(
                onClick = { showMagicBoxDrawer = !showMagicBoxDrawer },
                icon = Icons.Default.Widgets,
                text = "编程魔法盒 🎒",
                containerColor = Color(0xFFF57C00) // Deep warm amber
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 4. Manual Fallback Switch Mirror
            TopBarActionButton(
                onClick = {
                    if (currentMirrorIndex < mirrors.size - 1) {
                        currentMirrorIndex++
                    } else {
                        currentMirrorIndex = 0
                    }
                    scratchUrl = mirrors[currentMirrorIndex]
                    android.widget.Toast.makeText(context, "已手动切换到第 ${currentMirrorIndex + 1} 个极速镜像 ⚡", android.widget.Toast.LENGTH_SHORT).show()
                },
                icon = Icons.Default.Language,
                text = "手动换源 ⚡",
                containerColor = Color(0xFF2E7D32) // Soft forest green
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 5. 智能精灵姐姐 Button
            TopBarActionButton(
                onClick = {
                    showAiAssistSheet = !showAiAssistSheet
                },
                icon = Icons.Default.AutoAwesome,
                text = "智能精灵姐姐 👩‍💻",
                containerColor = Color(0xFFC2185B) // Deep rose ruby
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            this@Row.AnimatedVisibility(
                visible = showMagicBoxDrawer,
                enter = slideInHorizontally(animationSpec = androidx.compose.animation.core.tween(durationMillis = 250)) { -it } + fadeIn(animationSpec = androidx.compose.animation.core.tween(250)),
                exit = slideOutHorizontally(animationSpec = androidx.compose.animation.core.tween(250)) { -it } + fadeOut(animationSpec = androidx.compose.animation.core.tween(250))
            ) {
                MagicBoxDrawerPanel(
                    webView = webViewInstance,
                    viewModel = viewModel,
                    onClose = { showMagicBoxDrawer = false },
                    onInsertText = { text ->
                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("scratch", text)
                        clipboardManager.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "星梭拼搭秘籍已写入剪贴板 ⚡！请进入网页编辑区并拼搭它们噢！✨", android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFF5F5F5))
            ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val buttonWidthPx = with(density) { 60.dp.toPx() }
        val buttonHeightPx = with(density) { 60.dp.toPx() }
        val topPaddingPx = with(density) { 50.dp.toPx() }
        val bottomPaddingPx = with(density) { 50.dp.toPx() }

        // Position initial state once screen thickness is measured (优化一)
        LaunchedEffect(screenWidthPx, screenHeightPx) {
            if (!hasInitializedPosition && screenWidthPx > 0) {
                val initX = screenWidthPx - buttonWidthPx - with(density) { 20.dp.toPx() }
                // 距离底部导航栏上方20dp (系统底栏大概56dp, 加上20dp等于76dp, 设置为80dp完美避开)
                val initY = screenHeightPx - buttonHeightPx - with(density) { 80.dp.toPx() }
                animX.snapTo(initX)
                animY.snapTo(initY)
                hasInitializedPosition = true
            }
        }

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
                            
                            // 优化二：注入口：通过给HTML注入自定义Viewport限制双指缩放范围 (0.5x 到 2.0x, 默认 1.0x)
                            val viewportJs = """
                                (function() {
                                    var meta = document.createElement('meta');
                                    meta.name = 'viewport';
                                    meta.content = 'width=device-width, initial-scale=1.0, minimum-scale=0.5, maximum-scale=2.0, user-scalable=yes';
                                    var head = document.getElementsByTagName('head')[0];
                                    if (head) {
                                        var existingViewports = document.querySelectorAll('meta[name="viewport"]');
                                        existingViewports.forEach(function(el) { el.remove(); });
                                        head.appendChild(meta);
                                    }
                                    
                                    function tryAttach() {
                                        var ws = null;
                                        if (typeof Blockly !== 'undefined' && Blockly.getMainWorkspace) {
                                            ws = Blockly.getMainWorkspace();
                                        } else if (typeof Blockly !== 'undefined' && Blockly.mainWorkspace) {
                                            ws = Blockly.mainWorkspace;
                                        }
                                        if (ws) {
                                            ws.addChangeListener(function(event) {
                                                if (event.type === 'move' || event.type === 'create' || event.type === 'delete' || event.type === 'change') {
                                                    if (window.AndroidWorkspace) {
                                                        window.AndroidWorkspace.onCodeChanged();
                                                    }
                                                }
                                            });
                                            return true;
                                        }
                                        var targetVm = window.vm || (document.querySelector('iframe') && document.querySelector('iframe').contentWindow.vm);
                                        if (targetVm) {
                                            targetVm.on('workspaceUpdate', function() {
                                                if (window.AndroidWorkspace) {
                                                    window.AndroidWorkspace.onCodeChanged();
                                                }
                                            });
                                            return true;
                                        }
                                        return false;
                                    }
                                    var attached = tryAttach();
                                    if (!attached) {
                                        var interval = setInterval(function() {
                                            if (tryAttach()) {
                                                clearInterval(interval);
                                            }
                                        }, 1000);
                                    }
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(viewportJs, null)
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
                        
                        // 优化二：启用并且配置底层的 WebSettings 手势双指缩放支持
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false

                        // iPad User-Agent is the perfect golden standard (Problem 2 requirement 5):
                        // 1. It forces Scratch to unlock full workspace layout/sprites with no screen-size blocks.
                        // 2. It activates Scratch's optimized touch-gesture system, allowing kids to fluidly drag, drop, and snap blocks.
                        userAgentString = "Mozilla/5.0 (iPad; CPU OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"
                        mediaPlaybackRequiresUserGesture = false
                    }
                    
                    // Request focus immediately so that touches register without initial delays
                    requestFocus()
                    
                    // 优化二：手势冲突彻底解决：
                    // - 只有当多个手指（pointerCount >= 2）触屏时，才触发系统的 WebView 缩放
                    // - 单指触屏时直接关闭 WebView 的 supportZoom 以免干扰 Scratch 积木正常拼搭
                    setOnTouchListener { v, event ->
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                            if (showAiAssistSheet) {
                                showAiAssistSheet = false
                            }
                            if (showMagicBoxDrawer) {
                                showMagicBoxDrawer = false
                            }
                        }
                        val pointerCount = event.pointerCount
                        if (pointerCount >= 2) {
                            settings.setSupportZoom(true)
                        } else {
                            settings.setSupportZoom(false)
                        }
                        false
                    }
                    
                    addJavascriptInterface(ScratchJsInterface {
                        scratchChangeCounter++
                    }, "AndroidWorkspace")
                    
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
    }

            this@Row.AnimatedVisibility(
                visible = showAiAssistSheet,
                enter = slideInHorizontally(animationSpec = androidx.compose.animation.core.tween(durationMillis = 250)) { it } + fadeIn(animationSpec = androidx.compose.animation.core.tween(250)),
                exit = slideOutHorizontally(animationSpec = androidx.compose.animation.core.tween(250)) { it } + fadeOut(animationSpec = androidx.compose.animation.core.tween(250))
            ) {
                AiAssistPanel(
                    webView = webViewInstance,
                    viewModel = viewModel,
                    realTimeCheckEnabled = realTimeCheckEnabled,
                    onRealTimeCheckChange = { viewModel.setRealTimeStateEnabled(it) },
                    getLiveCodeAndCall = { getLiveCodeAndCall(it) },
                    onClose = { showAiAssistSheet = false }
                )
            }
        } // Closes side-by-side Row
    } // Closes main Column


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

                        EvaluationProgressRow(label = "1. 语法合规性 (检测积木完整拼接)", score = rep.grammarScore, maxScore = 25, color = Color(0xFF4CAF50))
                        EvaluationProgressRow(label = "2. 逻辑完整性 (检测逻辑环嵌套等)", score = rep.logicScore, maxScore = 30, color = Color(0xFF2196F3))
                        EvaluationProgressRow(label = "3. 任务匹配度 (检测任务目标要素)", score = rep.taskMatchScore, maxScore = 25, color = Color(0xFFFF9800))
                        EvaluationProgressRow(label = "4. 创意实现度 (分析交互及原创想法)", score = rep.creativeScore, maxScore = 20, color = Color(0xFF9C27B0))

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

@Composable
fun TeacherTaskManagementScreen(viewModel: MainViewModel) {
    val classes by viewModel.classesList.collectAsState()

    var taskNameInput by remember { mutableStateOf("") }
    var taskDetailInput by remember { mutableStateOf("") }
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
                    Text("王老师的管理事务中心 - 发布控制台", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
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
                            if (taskNameInput.isEmpty() || taskDetailInput.isEmpty() || filteredClasses.isEmpty()) {
                                Toast.makeText(context, "请填齐基本字段，并确认当前所选年级已建班级！", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val safeIndex = classSelectIndex.coerceIn(0, filteredClasses.size - 1)
                            val classId = filteredClasses[safeIndex].classId
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
    
    // Task Management States
    var activeMenuTaskId by remember { mutableStateOf<Int?>(null) }
    var showEditTaskDialog by remember { mutableStateOf<LearningTask?>(null) }
    var showExtendDeadlineDialog by remember { mutableStateOf<LearningTask?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<LearningTask?>(null) }
    var showRevokeConfirmDialog by remember { mutableStateOf<LearningTask?>(null) }

    var editName by remember { mutableStateOf("") }
    var editDetail by remember { mutableStateOf("") }
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
                                        val statusColor = when (t.status) {
                                            "已撤销" -> Color(0xFF757575)
                                            "进行中", null -> Color(0xFF4CAF50)
                                            else -> Color(0xFFF44336)
                                        }
                                        val statusBgColor = when (t.status) {
                                            "已撤销" -> Color(0xFFE0E0E0)
                                            "进行中", null -> Color(0xFFE8F5E9)
                                            else -> Color(0xFFFFEBEE)
                                        }
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = statusBgColor),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = t.status ?: "进行中",
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
                                                        editName = t.taskName
                                                        editDetail = t.taskDetail
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
                        if (editName.isBlank() || editDetail.isBlank() || editDeadline.isBlank()) {
                            Toast.makeText(context, "所有文本字段都不能为空哦", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (editClassId == -1) {
                            Toast.makeText(context, "请先选择一个目标班级哦", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.editTaskByTeacher(
                            taskId = task.taskId,
                            name = editName,
                            detail = editDetail,
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

fun parseConfigFromDescription(desc: String): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    val trimmed = desc.trim()
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
        try {
            val json = org.json.JSONObject(trimmed)
            result["level"] = json.optString("level", "三年级")
            result["dailyLimit"] = json.optInt("dailyLimit", 10)
            result["grammarCorrect"] = json.optBoolean("grammarCorrect", true)
            result["creativeGuide"] = json.optBoolean("creativeGuide", true)
            result["knowledgeExplain"] = json.optBoolean("knowledgeExplain", true)
            result["codeGenerate"] = json.optBoolean("codeGenerate", false)
            result["style"] = json.optString("style", "趣味活泼")
            result["weightGrammar"] = json.optInt("weightGrammar", 25)
            result["weightLogic"] = json.optInt("weightLogic", 30)
            result["weightTask"] = json.optInt("weightTask", 25)
            result["weightCreative"] = json.optInt("weightCreative", 20)
            result["teachingLock"] = json.optBoolean("teachingLock", false)
            result["remark"] = json.optString("remark", "")
            return result
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Fallback/standard values
    result["level"] = "三年级"
    result["dailyLimit"] = 10
    result["grammarCorrect"] = true
    result["creativeGuide"] = true
    result["knowledgeExplain"] = true
    result["codeGenerate"] = false
    result["style"] = "趣味活泼"
    result["weightGrammar"] = 25
    result["weightLogic"] = 30
    result["weightTask"] = 25
    result["weightCreative"] = 20
    result["teachingLock"] = false
    result["remark"] = ""

    val lines = trimmed.split("\n")
    for (line in lines) {
        val parts = line.split(":", "：", limit = 2)
        if (parts.size == 2) {
            val key = parts[0].trim()
            val value = parts[1].trim()
            when (key) {
                "难度等级" -> result["level"] = value
                "单日AI调用总上限" -> result["dailyLimit"] = value.toIntOrNull() ?: 10
                "语法纠错" -> result["grammarCorrect"] = (value == "开启" || value == "true")
                "创意引导" -> result["creativeGuide"] = (value == "开启" || value == "true")
                "知识点讲解" -> result["knowledgeExplain"] = (value == "开启" || value == "true")
                "完整代码生成" -> result["codeGenerate"] = (value == "开启" || value == "true")
                "AI提示风格" -> result["style"] = value
                "语法评分权重" -> result["weightGrammar"] = value.toIntOrNull() ?: 25
                "逻辑评分权重" -> result["weightLogic"] = value.toIntOrNull() ?: 30
                "任务匹配权重" -> result["weightTask"] = value.toIntOrNull() ?: 25
                "创意评分权重" -> result["weightCreative"] = value.toIntOrNull() ?: 20
                "教学锁" -> result["teachingLock"] = (value == "开启" || value == "true")
                "备注", "班级备注" -> result["remark"] = value
            }
        }
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherClassManagementUnifiedScreen(viewModel: MainViewModel) {
    val classes by viewModel.classesList.collectAsState()
    val students by viewModel.studentsList.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 新班级添加表单输入状态
    var newClassNameInput by remember { mutableStateOf("") }
    var newClassGradeInput by remember { mutableStateOf("三年级") }
    var newClassDescInput by remember { mutableStateOf("") }
    var manualGradeDropdownExpanded by remember { mutableStateOf(false) }

    // 各种弹窗管理
    var showEditClassDialog by remember { mutableStateOf(false) }
    var activeClassToEdit by remember { mutableStateOf<ClassEntity?>(null) }
    var editClassName by remember { mutableStateOf("") }
    var editClassGrade by remember { mutableStateOf("三年级") }
    var editClassDesc by remember { mutableStateOf("") }
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
    var editClassRemark by remember { mutableStateOf("") }

    var editClassLevelDropdownExpanded by remember { mutableStateOf(false) }
    var editClassStyleDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(showEditClassDialog, activeClassToEdit, editClassDesc) {
        if (showEditClassDialog && activeClassToEdit != null) {
            val parsedResult = parseConfigFromDescription(editClassDesc)
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
            editClassRemark = parsedResult["remark"] as? String ?: ""
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
                            text = "🆕 建立新的班级大纲（及一键批量生成）",
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
                                            newClassGradeInput = grade
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
                        // 1. 手动建档
                        Button(
                            onClick = {
                                if (newClassNameInput.isBlank()) {
                                    Toast.makeText(context, "请输入班级名称！", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.createNewClassByTeacher(
                                    newClassNameInput,
                                    newClassGradeInput,
                                    newClassDescInput
                                ) { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    newClassNameInput = ""
                                    newClassDescInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("手动建档此班", fontSize = 11.sp, color = Color.White)
                        }

                        // 2. 批量生成 1-6 班
                        Button(
                            onClick = {
                                viewModel.batchCreateClassesByTeacher(newClassGradeInput) { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("一键全装1-6班 ✨", fontSize = 11.sp, color = Color.White)
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
                                        editClassName = classEntity.className
                                        editClassGrade = classEntity.grade
                                        editClassDesc = classDesc
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
                                            Text(
                                                text = student.name,
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
                                        editClassGrade = grade
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
                        if (editClassName.isBlank()) {
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
                            put("remark", editClassRemark)
                        }
                        val serializedJson = json.toString()

                        viewModel.updateClassByTeacher(
                            classId = selClass.classId,
                            className = editClassName,
                            grade = editClassGrade,
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
                            classId = selClass.classId
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

fun injectBlockIntoWebView(webView: WebView?, blockText: String, context: android.content.Context) {
    if (webView == null) {
        Toast.makeText(context, "⚠️ 编程空间未就绪，请稍后", Toast.LENGTH_SHORT).show()
        return
    }

    // Determine the correct Scratch 3.0 official opcode
    val opcode = when {
        blockText.contains("移动") -> "motion_movesteps"
        blockText.contains("右转") -> "motion_turnright"
        blockText.contains("左转") -> "motion_turnleft"
        blockText.contains("移到") && blockText.contains("x:") -> "motion_gotoxy"
        blockText.contains("反弹") -> "motion_ifonedgebounce"
        blockText.contains("面向") -> "motion_pointindirection"
        
        blockText.contains("说") -> "looks_sayforsecs"
        blockText.contains("显示") -> "looks_show"
        blockText.contains("隐藏") -> "looks_hide"
        blockText.contains("下一个造型") -> "looks_nextcostume"
        blockText.contains("造型") -> "looks_switchcostumeto"
        blockText.contains("大小增加") || blockText.contains("将大小") -> "looks_changesizeby"
        
        blockText.contains("直到结束") -> "sound_playuntildone"
        blockText.contains("播放") || blockText.contains("声音") -> "sound_play"
        blockText.contains("所有声音") -> "sound_stopallloops"
        
        blockText.contains("绿旗") -> "event_whenflagclicked"
        blockText.contains("空格") -> "event_whenkeypressed"
        blockText.contains("角色") && blockText.contains("点击") -> "event_whenthisspriteclicked"
        blockText.contains("接收到") || blockText.contains("消息") -> "event_whenbroadcastreceived"
        
        blockText.contains("次") -> "control_repeat"
        blockText.contains("永远") || blockText.contains("永远") -> "control_forever"
        blockText.contains("如果") -> "control_if"
        blockText.contains("等待") && blockText.contains("秒") -> "control_wait"
        blockText.contains("停止") -> "control_stop"
        
        blockText.contains("碰到颜色") -> "sensing_touchingcolor"
        blockText.contains("碰到") -> "sensing_touchingobject"
        blockText.contains("x坐标") || blockText.contains("鼠标") -> "sensing_mousex"
        blockText.contains("询问") -> "sensing_askandwait"
        
        blockText.contains("+") -> "operator_add"
        blockText.contains(">") -> "operator_gt"
        blockText.contains("<") -> "operator_lt"
        blockText.contains("随机数") -> "operator_random"
        blockText.contains("连接") -> "operator_join"
        
        blockText.contains("设为0") || blockText.contains("设为") -> "data_setvariableto"
        blockText.contains("增加") || blockText.contains("增加1") -> "data_changevariableby"
        
        else -> "motion_movesteps"
    }

    val js = """
        (function() {
            try {
                var targetVm = typeof vm !== 'undefined' ? vm : (window.vm || window.ScratchVM || (window.editor && window.editor.vm));
                if (!targetVm) {
                    var el = document.getElementById('scratch') || document.querySelector('[class^="gui_stage-wrapper_"]');
                    if (el) {
                        var keys = Object.keys(el);
                        var key = keys.find(function(k) { return k.startsWith('__reactInternalInstance${'$'}') || k.startsWith('__reactFiber${'$'}'); });
                        if (key) {
                            var fiber = el[key];
                            while (fiber) {
                                if (fiber.stateNode && fiber.stateNode.props && fiber.stateNode.props.vm) {
                                    targetVm = fiber.stateNode.props.vm;
                                    break;
                                }
                                fiber = fiber.return;
                            }
                        }
                    }
                }
                if (!targetVm) {
                    var frames = document.querySelectorAll('iframe');
                    for (var i = 0; i < frames.length; i++) {
                        if (frames[i].contentWindow) {
                            var sw = frames[i].contentWindow;
                            if (sw.vm || sw.ScratchVM || (sw.editor && sw.editor.vm)) {
                                targetVm = sw.vm || sw.ScratchVM || (sw.editor && sw.editor.vm);
                                break;
                            }
                        }
                    }
                }
                if (!targetVm) {
                    return "VM_NOT_READY";
                }
                
                var targetId = targetVm.editingTarget ? targetVm.editingTarget.id : null;
                if (!targetId) {
                    return "NO_TARGET_SELECTED";
                }
                
                var blockX = Math.random() * 200 + 50;
                var blockY = Math.random() * 200 + 50;
                try {
                    var targetWorkspace = window.Blockly || (document.querySelector('iframe') && document.querySelector('iframe').contentWindow.Blockly);
                    if (!targetWorkspace) {
                        var fs = document.querySelectorAll('iframe');
                        for (var j = 0; j < fs.length; j++) {
                            if (fs[j].contentWindow && fs[j].contentWindow.Blockly) {
                                targetWorkspace = fs[j].contentWindow.Blockly;
                                break;
                            }
                        }
                    }
                    if (targetWorkspace && targetWorkspace.getMainWorkspace()) {
                        var ws = targetWorkspace.getMainWorkspace();
                        var metrics = ws.getMetrics();
                        if (metrics) {
                            blockX = Math.round((-ws.scrollX + metrics.viewWidth / 2) / ws.scale) + (Math.random() * 40 - 20);
                            blockY = Math.round((-ws.scrollY + metrics.viewHeight / 2) / ws.scale) + (Math.random() * 40 - 20);
                        }
                    }
                } catch (e) {}

                var blockId = targetVm.runtime.addBlock({
                    opcode: '$opcode',
                    targetId: targetId,
                    fields: {},
                    inputs: {},
                    x: blockX,
                    y: blockY
                });
                
                if (targetVm.selectBlock) {
                    targetVm.selectBlock(blockId);
                }
                if (targetVm.emitWorkspaceUpdate) {
                    targetVm.emitWorkspaceUpdate();
                }
                return "success";
            } catch (e) {
                return "error: " + e.message;
            }
        })();
    """.trimIndent()

    webView.evaluateJavascript(js) { result ->
        if (result != null && result.contains("success")) {
            Toast.makeText(context, "魔法积木已成功插入！", Toast.LENGTH_SHORT).show()
        } else {
            try {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val blockJson = """{
  "opcode": "$opcode",
  "fields": {},
  "inputs": {},
  "x": 100,
  "y": 100
}"""
                val clip = android.content.ClipData.newPlainText("Scratch Block JSON", blockJson)
                clipboard.setPrimaryClip(clip)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Toast.makeText(context, "自动插入失败，积木代码已复制到剪贴板，请在编辑器中粘贴", Toast.LENGTH_LONG).show()
        }
    }
}

fun loadProjectIntoWebView(webView: WebView?, pJson: String, context: android.content.Context) {
    if (webView == null) return
    val cleanJson = pJson.replace("'", "\\'").replace("\n", " ").replace("\r", " ")
    val js = """
        (function() {
            try {
                var targetVm = window.vm || (document.querySelector('iframe') && document.querySelector('iframe').contentWindow.vm);
                if (!targetVm) {
                    var el = document.getElementById('scratch') || document.querySelector('[class^="gui_stage-wrapper_"]');
                    if (el) {
                        var keys = Object.keys(el);
                        var key = keys.find(function(k) { return k.startsWith('__reactInternalInstance${'$'}') || k.startsWith('__reactFiber${'$'}'); });
                        if (key) {
                            var fiber = el[key];
                            while (fiber) {
                                if (fiber.stateNode && fiber.stateNode.props && fiber.stateNode.props.vm) {
                                    targetVm = fiber.stateNode.props.vm;
                                    break;
                                }
                                fiber = fiber.return;
                            }
                        }
                    }
                }
                
                if (!targetVm) {
                    var frames = document.querySelectorAll('iframe');
                    for (var i = 0; i < frames.length; i++) {
                        var sw = frames[i].contentWindow;
                        if (sw && sw.vm) {
                            targetVm = sw.vm;
                            break;
                        }
                    }
                }
                
                if (targetVm) {
                    targetVm.loadProject('$cleanJson').then(function() {
                        console.log("Success");
                    });
                    return "Injected project loading";
                }
                return "VM not found";
            } catch(e) {
                return "Error: " + e.message;
            }
        })();
    """.trimIndent()
    webView.evaluateJavascript(js) { res ->
        android.util.Log.d("ScratchLoader", "Result: $res")
    }
}

fun getXmlForBlockText(blockText: String): String {
    return when {
        blockText.contains("移动") -> "<block type=\"motion_movesteps\"><value name=\"STEPS\"><shadow type=\"math_number\"><field name=\"NUM\">10</field></shadow></value></block>"
        blockText.contains("右转") -> "<block type=\"motion_turnright\"><value name=\"DEGREES\"><shadow type=\"math_number\"><field name=\"NUM\">15</field></shadow></value></block>"
        blockText.contains("左转") -> "<block type=\"motion_turnleft\"><value name=\"DEGREES\"><shadow type=\"math_number\"><field name=\"NUM\">15</field></shadow></value></block>"
        blockText.contains("随机位置") && blockText.contains("移到") -> "<block type=\"motion_goto_menu\"></block>"
        blockText.contains("x:") && blockText.contains("y:") -> "<block type=\"motion_gotoxy\"><value name=\"X\"><shadow type=\"math_number\"><field name=\"NUM\">0</field></shadow></value><value name=\"Y\"><shadow type=\"math_number\"><field name=\"NUM\">0</field></shadow></value></block>"
        blockText.contains("滑行") -> "<block type=\"motion_glideto\"><value name=\"SECS\"><shadow type=\"math_number\"><field name=\"NUM\">1</field></shadow></value></block>"
        blockText.contains("面向") -> "<block type=\"motion_pointindirection\"><value name=\"DIRECTION\"><shadow type=\"math_angle\"><field name=\"NUM\">90</field></shadow></value></block>"
        blockText.contains("反弹") -> "<block type=\"motion_ifonedgebounce\"></block>"
        
        blockText.contains("说") -> "<block type=\"looks_sayforsecs\"><value name=\"MESSAGE\"><shadow type=\"text\"><field name=\"TEXT\">你好！</field></shadow></value><value name=\"SECS\"><shadow type=\"math_number\"><field name=\"NUM\">2</field></shadow></value></block>"
        blockText.contains("思考") -> "<block type=\"looks_thinkforsecs\"><value name=\"MESSAGE\"><shadow type=\"text\"><field name=\"TEXT\">嗯...</field></shadow></value><value name=\"SECS\"><shadow type=\"math_number\"><field name=\"NUM\">2</field></shadow></value></block>"
        blockText.contains("下一个造型") -> "<block type=\"looks_nextcostume\"></block>"
        blockText.contains("造型") -> "<block type=\"looks_switchcostumeto\"></block>"
        blockText.contains("背景") -> "<block type=\"looks_switchbackdropto\"></block>"
        blockText.contains("增加大小") -> "<block type=\"looks_changesizeby\"><value name=\"SIZE\"><shadow type=\"math_number\"><field name=\"NUM\">10</field></shadow></value></block>"
        blockText.contains("大小设为") -> "<block type=\"looks_setsizeto\"><value name=\"SIZE\"><shadow type=\"math_number\"><field name=\"NUM\">100</field></shadow></value></block>"
        blockText.contains("显示") -> "<block type=\"looks_show\"></block>"
        blockText.contains("隐藏") -> "<block type=\"looks_hide\"></block>"
        
        blockText.contains("等待播完") -> "<block type=\"sound_playuntildone\"><value name=\"SOUND_MENU\"><shadow type=\"sound_menu\"><field name=\"SOUND_MENU\">喵</field></shadow></value></block>"
        blockText.contains("播放声音") -> "<block type=\"sound_play\"><value name=\"SOUND_MENU\"><shadow type=\"sound_menu\"><field name=\"SOUND_MENU\">喵</field></shadow></value></block>"
        blockText.contains("所有声音") -> "<block type=\"sound_stopallloops\"></block>"
        blockText.contains("音量增加") -> "<block type=\"sound_changevolumeby\"><value name=\"VOLUME\"><shadow type=\"math_number\"><field name=\"NUM\">-10</field></shadow></value></block>"
        blockText.contains("音量设为") -> "<block type=\"sound_setvolumeto\"><value name=\"VOLUME\"><shadow type=\"math_number\"><field name=\"NUM\">100</field></shadow></value></block>"

        blockText.contains("当 🟢 被点击") || blockText.contains("🟢") -> "<block type=\"event_whenflagclicked\"></block>"
        blockText.contains("按下") -> "<block type=\"event_whenkeypressed\"><field name=\"KEY_OPTION\">space</field></block>"
        blockText.contains("被点击") -> "<block type=\"event_whenthisspriteclicked\"></block>"
        blockText.contains("背景换成") -> "<block type=\"event_whenbackdropswitchesto\"></block>"
        blockText.contains("接收到广播") -> "<block type=\"event_whenbroadcastreceived\"></block>"
        blockText.contains("广播") -> "<block type=\"event_broadcast\"></block>"

        blockText.contains("等待") && blockText.contains("秒") -> "<block type=\"control_wait\"><value name=\"DURATION\"><shadow type=\"math_positive_number\"><field name=\"NUM\">1</field></shadow></value></block>"
        blockText.contains("重复执行") && blockText.contains("次") -> "<block type=\"control_repeat\"><value name=\"TIMES\"><shadow type=\"math_whole_number\"><field name=\"NUM\">10</field></shadow></value></block>"
        blockText.contains("重复执行") -> "<block type=\"control_forever\"></block>"
        blockText.contains("如果") && blockText.contains("那么") && blockText.contains("否则") -> "<block type=\"control_if_else\"></block>"
        blockText.contains("如果") && blockText.contains("那么") -> "<block type=\"control_if\"></block>"
        blockText.contains("一直等待") -> "<block type=\"control_wait_until\"></block>"
        blockText.contains("克隆") && blockText.contains("自己") -> "<block type=\"control_create_clone_of\"><value name=\"CLONE_OPTION\"><shadow type=\"control_create_clone_of_menu\"><field name=\"CLONE_OPTION\">_myself_</field></shadow></value></block>"
        blockText.contains("作为克隆体") -> "<block type=\"control_start_as_clone\"></block>"

        blockText.contains("碰到") && blockText.contains("?") -> "<block type=\"sensing_touchingobject\"><value name=\"TOUCHINGOBJECTMENU\"><shadow type=\"sensing_touchingobjectmenu\"><field name=\"TOUCHINGOBJECTMENU\">_mouse_</field></shadow></value></block>"
        blockText.contains("碰到颜色") -> "<block type=\"sensing_touchingcolor\"></block>"
        blockText.contains("询问") -> "<block type=\"sensing_askandwait\"></block>"
        blockText.contains("键是否按下") -> "<block type=\"sensing_keypressed\"><field name=\"KEY_OPTION\">space</field></block>"
        blockText.contains("鼠标的 x") -> "<block type=\"sensing_mousex\"></block>"
        blockText.contains("鼠标的 y") -> "<block type=\"sensing_mousey\"></block>"
        blockText.contains("计时器") -> "<block type=\"sensing_timer\"></block>"

        blockText.contains("+") -> "<block type=\"operator_add\"></block>"
        blockText.contains("-") -> "<block type=\"operator_subtract\"></block>"
        blockText.contains("随机数") -> "<block type=\"operator_random\"><value name=\"FROM\"><shadow type=\"math_number\"><field name=\"FROM\">1</field></shadow></value><value name=\"TO\"><shadow type=\"math_number\"><field name=\"TO\">10</field></shadow></value></block>"
        blockText.contains(">") -> "<block type=\"operator_gt\"></block>"
        blockText.contains("=") -> "<block type=\"operator_equals\"></block>"
        blockText.contains("与") -> "<block type=\"operator_and\"></block>"
        blockText.contains("或") -> "<block type=\"operator_or\"></block>"
        blockText.contains("连接") -> "<block type=\"operator_join\"></block>"

        blockText.contains("建立一个变量") -> "<block type=\"data_variable\"></block>"
        blockText.contains("设为") -> "<block type=\"data_setvariableto\"></block>"
        blockText.contains("增加") -> "<block type=\"data_changevariableby\"></block>"
        blockText.contains("显示变量") -> "<block type=\"data_showvariable\"></block>"
        blockText.contains("隐藏变量") -> "<block type=\"data_hidevariable\"></block>"
        
        else -> "<block type=\"motion_movesteps\"><value name=\"STEPS\"><shadow type=\"math_number\"><field name=\"NUM\">10</field></shadow></value></block>"
    }
}

class ScratchJsInterface(private val onChanged: () -> Unit) {
    @android.webkit.JavascriptInterface
    fun onCodeChanged() {
        onChanged()
    }
}

// Helper composable for red/green styling of grammar diagnosis
@Composable
fun StyledAiResult(text: String) {
    val lines = text.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            val color = when {
                line.contains("【错误提示】") || line.contains("错误提示") || line.contains("❌") -> Color(0xFFD32F2F) // Red
                line.contains("【修正建议】") || line.contains("修正建议") || line.contains("✅") || line.contains("✔️") -> Color(0xFF388E3C) // Green
                else -> Color.Unspecified
            }
            val fontWeight = if (line.contains("【错误提示】") || line.contains("【修正建议】") || line.startsWith("错误") || line.startsWith("修正")) FontWeight.Bold else FontWeight.Normal
            Text(
                text = line,
                color = color,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = fontWeight
            )
        }
    }
}

// Data class representation for history records
data class DialogueHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val question: String,
    val answer: String,
    val timestamp: String,
    val isExpanded: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AiAssistPanel(
    webView: WebView?,
    viewModel: MainViewModel,
    realTimeCheckEnabled: Boolean,
    onRealTimeCheckChange: (Boolean) -> Unit,
    getLiveCodeAndCall: (String) -> Unit,
    onClose: () -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val panelWidth = (configuration.screenWidthDp / 3).dp
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiResult by viewModel.aiResult.collectAsState()
    val aiResultType by viewModel.aiResultType.collectAsState()
    
    var creativePromptInput by remember { mutableStateOf("") }
    var kbPromptInput by remember { mutableStateOf("") }
    var customQuestionInput by remember { mutableStateOf("") }
    
    // Default active tab to "语法纠错"
    var activeTab by remember { mutableStateOf("语法纠错") }

    // Dialogue history list
    val dialogueHistory = remember { mutableStateListOf<DialogueHistoryItem>() }

    // Initial greeting load
    LaunchedEffect(Unit) {
        if (dialogueHistory.isEmpty()) {
            dialogueHistory.add(
                DialogueHistoryItem(
                    title = "【精灵姐姐】",
                    question = "开始我们今天的编程冒险吧！",
                    answer = "哈喽！我是你的智能精灵姐姐，今天想和我一起探索什么神奇的 Scratch 编程魔法呢？✨",
                    timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                )
            )
        }
    }

    // Capture and automatically append new AI replies into dialogue history
    LaunchedEffect(aiResult) {
        val res = aiResult
        val type = aiResultType
        if (!res.isNullOrBlank()) {
            if (dialogueHistory.none { it.answer == res }) {
                val q = when (type) {
                    "创意引导" -> if (creativePromptInput.isNotBlank()) "主题: $creativePromptInput" else "自由扩展与创意优化"
                    "知识点讲解" -> if (kbPromptInput.isNotBlank()) "知识点: $kbPromptInput" else "知识考点"
                    "语法纠错" -> "语法与逻辑检测"
                    else -> "诊断检测"
                }
                val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                dialogueHistory.add(0, DialogueHistoryItem(
                    title = "【$type】",
                    question = q,
                    answer = res,
                    timestamp = timeStr
                ))
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxHeight()
            .width(panelWidth),
        shape = androidx.compose.ui.graphics.RectangleShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE4EC)),
        border = BorderStroke(1.dp, Color(0xFFF06292))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFC2185B))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI少儿编程小搭档", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            // Area 1: 顶部功能标签区 (Height 48dp, 16dp spacing, underline indicator)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("语法纠错", "创意引导", "考点讲解").forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = tab }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = tab,
                                color = if (isSelected) Color(0xFFC2185B) else Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(2.dp)
                                    .background(if (isSelected) Color(0xFFC2185B) else Color.Transparent)
                            )
                        }
                    }
                }
            }

            // Area 2: 中间内容展示区 (占 70% 比例、支持完整滚动及点击历史记录展开)
            LazyColumn(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Specific Controls depending on selected tab
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (activeTab) {
                            "语法纠错" -> {
                                // Switch for Real-time detection in Optimization 1
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFF8BBD0), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFC2185B), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("开启实时代码检测", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                                    }
                                    Switch(
                                        checked = realTimeCheckEnabled,
                                        onCheckedChange = { onRealTimeCheckChange(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFFC2185B),
                                            uncheckedThumbColor = Color.LightGray,
                                            uncheckedTrackColor = Color.White
                                        ),
                                        modifier = Modifier.scale(0.8f)
                                    )
                                }

                                // Interactive manually trigger button preserved
                                Button(
                                    onClick = { getLiveCodeAndCall("语法纠错") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2185B)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Text("🛑 立即手动语法检测", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            "创意引导" -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFF8BBD0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("💡 创意灵感库介绍", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2185B))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "不知道怎么搭积木了？没关系！在下方输入一个你喜欢的主题（如“走迷宫”、“极速赛车”），点击发送，精灵姐姐就会利用魔法，根据你目前的进度送给你三大创意巧思哦！✨",
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                            "考点讲解" -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFF8BBD0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("🎓 知识点锦囊 (快速点击选择)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2185B))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            listOf("循环", "变量", "广播", "坐标").forEach { chip ->
                                                Box(
                                                    modifier = Modifier
                                                        .height(32.dp)
                                                        .background(Color(0xFFFFEEF0), RoundedCornerShape(12.dp))
                                                        .border(1.dp, Color(0xFFF8BBD0), RoundedCornerShape(12.dp))
                                                        .clickable { 
                                                            kbPromptInput = chip
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                 ) {
                                                     Text(
                                                         text = "🏷️ $chip",
                                                         fontSize = 12.sp,
                                                         fontWeight = FontWeight.Bold,
                                                         color = Color(0xFFC2185B)
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

                // Current diagnostics AI result & loader
                item {
                    val currentTypeShow = when (activeTab) {
                        "语法纠错" -> "语法纠错"
                        "创意引导" -> "创意引导"
                        else -> "知识点讲解"
                    }
                    if (aiLoading && aiResultType == currentTypeShow) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF8BBD0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFFC2185B), strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                val loadingMsg = if (aiResultType == "创意引导") {
                                    "正在分析你的代码，请稍候..."
                                } else {
                                    "精灵姐姐正在全力思索中..."
                                }
                                Text(loadingMsg, fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    } else if (aiResult != null && aiResultType == currentTypeShow) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF8BBD0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "✨ 当前分析回复：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2185B))
                                Spacer(modifier = Modifier.height(6.dp))
                                StyledAiResult(aiResult ?: "")
                            }
                        }
                    }
                }

                // Dialogue history section header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFFC2185B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "对话历史记录 (随时点击展开/收起)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF880E4F)
                        )
                    }
                }

                // Interactive Expandable Dialogue history items list (Optimization 4)
                items(dialogueHistory.size) { index ->
                    val item = dialogueHistory[index]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                dialogueHistory[index] = item.copy(isExpanded = !item.isExpanded)
                            }
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFF8BBD0), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.title} ${item.question}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC2185B),
                                    maxLines = if (item.isExpanded) Int.MAX_VALUE else 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "历史时间: ${item.timestamp}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Icon(
                                imageVector = if (item.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFFC2185B),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (item.isExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFFFCE4EC))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "提问或检测背景：",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = item.question,
                                fontSize = 12.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Text(
                                text = "最佳辅助回复方案：",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC2185B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            StyledAiResult(item.answer)
                        }
                    }
                }
            }

            // Area 3: 固定底部输入区 (Spacing of 16dp, Outlined input box 48dp height with automatic context-aware placeholders in Optimization 3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFCE4EC))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var isInputFocused by remember { mutableStateOf(false) }
                val currentInputValue = when (activeTab) {
                    "语法纠错" -> customQuestionInput
                    "创意引导" -> creativePromptInput
                    else -> kbPromptInput
                }
                
                val currentHintText = when (activeTab) {
                    "语法纠错" -> "直接在这里输入问题或跟精灵姐姐聊天吧..."
                    "创意引导" -> "输入创作主题(如:太空飞行、打地鼠)..."
                    else -> "选择或输入知识点..."
                }

                OutlinedTextField(
                    value = currentInputValue,
                    onValueChange = { newValue ->
                        when (activeTab) {
                            "语法纠错" -> customQuestionInput = newValue
                            "创意引导" -> creativePromptInput = newValue
                            else -> kbPromptInput = newValue
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .background(Color.White, RoundedCornerShape(8.dp)),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color.Black),
                    placeholder = {
                        Text(
                            text = currentHintText,
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        errorBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = Color(0xFFC2185B)
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text
                    ),
                    maxLines = 5,
                    singleLine = false,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (currentInputValue.isNotBlank()) {
                            when (activeTab) {
                                "语法纠错" -> {
                                    val userQ = customQuestionInput
                                    customQuestionInput = ""
                                    val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                    
                                    // Add to history list immediately
                                    dialogueHistory.add(0, DialogueHistoryItem(
                                        title = "【自定义提问】",
                                        question = userQ,
                                        answer = "正在全力思索中...",
                                        timestamp = timeStr
                                    ))
                                    
                                    viewModel.callAiCustomQuestion(userQ) { response ->
                                        val idx = dialogueHistory.indexOfFirst { it.question == userQ && it.answer == "正在全力思索中..." }
                                        if (idx != -1) {
                                            dialogueHistory[idx] = dialogueHistory[idx].copy(answer = response)
                                        }
                                    }
                                }
                                "创意引导" -> {
                                    val liveTheme = creativePromptInput
                                    if (liveTheme.isNotBlank()) {
                                        viewModel.currentDraftName.value = liveTheme
                                    }
                                    getLiveCodeAndCall("创意引导")
                                    creativePromptInput = ""
                                }
                                "考点讲解" -> {
                                    getLiveCodeAndCall("知识点讲解")
                                    kbPromptInput = ""
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2185B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(48.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("发送 🚀", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
