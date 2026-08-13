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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    val classes by viewModel.classesList.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val isLoading by viewModel.currentBtnLoading.collectAsState()

    var isRegisterMode by remember { mutableStateOf(false) }
    var selectedRoleTab by remember { mutableStateOf(0) } // 0: 学生登录, 1: 教师登录

    // 字段状态值
    var studentNum by remember { mutableStateOf(TextFieldValue("")) }
    var teacherWorkId by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var studentName by remember { mutableStateOf(TextFieldValue("")) }

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
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Supabase 云端数据库连接状态检测区
                val supabaseStatus by viewModel.supabaseStatus.collectAsState()
                val supabaseTesting by viewModel.supabaseTesting.collectAsState()
                val syncStatus by viewModel.syncStatus.collectAsState()
                val syncing by viewModel.syncing.collectAsState()
                val statusColor = when {
                    supabaseStatus.contains("连接成功") -> Color(0xFF4CAF50)
                    supabaseStatus.contains("检测中") -> Color(0xFFFF9800)
                    supabaseStatus.contains("失败") || supabaseStatus.contains("异常") -> Color(0xFFF44336)
                    else -> Color.Gray
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = statusColor,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "云端数据库: $supabaseStatus",
                                fontSize = 11.sp,
                                color = statusColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.testSupabaseConnection() },
                            enabled = !supabaseTesting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E88E5),
                                disabledContainerColor = Color.LightGray
                            ),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            if (supabaseTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("检测连接", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 本地数据同步到云端区域
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "本地数据同步: $syncStatus",
                                fontSize = 11.sp,
                                color = if (syncStatus.contains("成功")) Color(0xFF2E7D32) 
                                       else if (syncStatus.contains("失败") || syncStatus.contains("异常")) Color(0xFFC62828)
                                       else Color.Gray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.syncAllDataToCloud() },
                                enabled = !syncing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF43A047),
                                    disabledContainerColor = Color.LightGray
                                ),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                if (syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("同步到云端", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

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
                        onClick = { selectedRoleTab = 0; password = TextFieldValue(""); isRegisterMode = false },
                        text = { Text("👦 学生通道", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedRoleTab == 1,
                        onClick = { selectedRoleTab = 1; password = TextFieldValue(""); isRegisterMode = false },
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
                                if (studentNum.text.isEmpty() || password.text.isEmpty()) {
                                    Toast.makeText(context, "请填入学号 and 密码！", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.studentLogin(studentNum.text, password.text) {
                                    Toast.makeText(context, "学生登录成功！", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                if (teacherWorkId.text.isEmpty() || password.text.isEmpty()) {
                                    Toast.makeText(context, "请填入教师工号和密码！", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.teacherLogin(teacherWorkId.text, password.text) {
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
                        onClick = { isRegisterMode = true; password = TextFieldValue("") },
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
                                        viewModel.studentLogin("3101", "123456") {
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
                                if (studentNum.text.isEmpty() || studentName.text.isEmpty() || password.text.isEmpty() || rFilteredClasses.isEmpty()) {
                                    Toast.makeText(context, "请填齐所有的学生注册字段并选择班级！", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val targetClass = rFilteredClasses.getOrNull(rSelectedClassIndex)
                                if (targetClass == null) {
                                    Toast.makeText(context, "请先选择一个有效的班级！", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.studentRegister(
                                    studentNum = studentNum.text,
                                    name = studentName.text,
                                    pass = password.text,
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
                                if (teacherWorkId.text.isEmpty() || studentName.text.isEmpty() || password.text.isEmpty()) {
                                    Toast.makeText(context, "请填齐所有的教师注册字段！", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.teacherRegister(
                                    workId = teacherWorkId.text,
                                    name = studentName.text,
                                    pass = password.text
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

@Composable
fun MainPortalScreen(viewModel: MainViewModel, userRole: String) {
    var selectedScreenIndex by remember { mutableIntStateOf(0) }
    val currentUserName by viewModel.currentUserName.collectAsState()
    val context = LocalContext.current
    val teacherViewingWorkspace by viewModel.teacherViewingWorkspace.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

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
                if (!((userRole == "student" && selectedScreenIndex == 0) || (userRole != "student" && teacherViewingWorkspace))) {
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

                    if (userRole == "student") {
                        StudentHorizontalTabBar(
                            selectedScreenIndex = selectedScreenIndex,
                            onTabSelected = { selectedScreenIndex = it }
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (userRole != "student" && !teacherViewingWorkspace && !isWideScreen) {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(
                        selected = selectedScreenIndex == 0,
                        onClick = { selectedScreenIndex = 0 },
                        label = { Text("学情大屏") },
                        icon = { Icon(Icons.Default.Analytics, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = selectedScreenIndex == 1,
                        onClick = { selectedScreenIndex = 1 },
                        label = { Text("发布任务") },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = selectedScreenIndex == 2,
                        onClick = { selectedScreenIndex = 2 },
                        label = { Text("任务列表") },
                        icon = { Icon(Icons.Default.Assignment, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = selectedScreenIndex == 3,
                        onClick = { selectedScreenIndex = 3 },
                        label = { Text("本班作品") },
                        icon = { Icon(Icons.Default.SupervisorAccount, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = selectedScreenIndex == 4,
                        onClick = { selectedScreenIndex = 4 },
                        label = { Text("班级管理") },
                        icon = { Icon(Icons.Default.Class, contentDescription = null) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isWideScreen && !teacherViewingWorkspace && !(userRole == "student" && selectedScreenIndex == 0)) {
                NavigationRail(containerColor = Color(0xFFF1F5F9)) {
                    if (userRole == "student") {
                        NavigationRailItem(
                            selected = selectedScreenIndex == 0,
                            onClick = { selectedScreenIndex = 0 },
                            label = { Text("编程工作台", fontSize = 10.sp) },
                            icon = { Icon(Icons.Default.Code, contentDescription = null) }
                        )
                        NavigationRailItem(
                            selected = selectedScreenIndex == 1,
                            onClick = { selectedScreenIndex = 1 },
                            label = { Text("开源大厅", fontSize = 10.sp) },
                            icon = { Icon(Icons.Default.Explore, contentDescription = null) }
                        )
                        NavigationRailItem(
                            selected = selectedScreenIndex == 2,
                            onClick = { selectedScreenIndex = 2 },
                            label = { Text("学习任务", fontSize = 10.sp) },
                            icon = { Icon(Icons.Default.Assignment, contentDescription = null) }
                        )
                        NavigationRailItem(
                            selected = selectedScreenIndex == 3,
                            onClick = { selectedScreenIndex = 3 },
                            label = { Text("我的作品", fontSize = 10.sp) },
                            icon = { Icon(Icons.Default.Collections, contentDescription = null) }
                        )
                        NavigationRailItem(
                            selected = selectedScreenIndex == 4,
                            onClick = { selectedScreenIndex = 4 },
                            label = { Text("AI 辅助", fontSize = 10.sp) },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
                        )
                        NavigationRailItem(
                            selected = selectedScreenIndex == 5,
                            onClick = { selectedScreenIndex = 5 },
                            label = { Text("个人中心", fontSize = 10.sp) },
                            icon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                    } else {
                        NavigationRailItem(
                            selected = selectedScreenIndex == 0,
                            onClick = { selectedScreenIndex = 0 },
                            label = { Text("学情大屏", fontSize = 10.sp) },
                            icon = { Icon(Icons.Default.Analytics, contentDescription = null) }
                        )
                        NavigationRailItem(
                            selected = selectedScreenIndex == 1,
                            onClick = { selectedScreenIndex = 1 },
                            label = { Text("发布任务", fontSize = 10.sp) },
                            icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        NavigationRailItem(
                            selected = selectedScreenIndex == 2,
                            onClick = { selectedScreenIndex = 2 },
                            label = { Text("任务列表", fontSize = 10.sp) },
                            icon = { Icon(Icons.Default.Assignment, contentDescription = null) }
                        )
                        NavigationRailItem(
                            selected = selectedScreenIndex == 3,
                            onClick = { selectedScreenIndex = 3 },
                            label = { Text("本班作品", fontSize = 10.sp) },
                            icon = { Icon(Icons.Default.SupervisorAccount, contentDescription = null) }
                        )
                        NavigationRailItem(
                            selected = selectedScreenIndex == 4,
                            onClick = { selectedScreenIndex = 4 },
                            label = { Text("班级管理", fontSize = 10.sp) },
                            icon = { Icon(Icons.Default.Class, contentDescription = null) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (userRole == "student") {
                    when (selectedScreenIndex) {
                        0 -> InteractiveScratchProgrammingScreen(viewModel = viewModel, onBackToHall = { selectedScreenIndex = 2 })
                        1 -> OpenHallScreen(viewModel = viewModel, onNavigateToEditor = { selectedScreenIndex = 0 })
                        2 -> StudentTasksScreen(viewModel = viewModel, onGoToCode = { selectedScreenIndex = 0 })
                        3 -> StudentWorksScreen(viewModel = viewModel, onGoToCode = { selectedScreenIndex = 0 })
                        4 -> AITutoringScreen(viewModel = viewModel)
                        5 -> StudentProfileScreen(viewModel = viewModel, onBack = { selectedScreenIndex = 0 })
                        else -> OpenHallScreen(viewModel = viewModel, onNavigateToEditor = { selectedScreenIndex = 0 })
                    }
                } else {
                    if (teacherViewingWorkspace) {
                        InteractiveScratchProgrammingScreen(viewModel = viewModel, onBackToHall = { viewModel.teacherViewingWorkspace.value = false })
                    } else {
                        when (selectedScreenIndex) {
                            0 -> TeacherAnalyticsScreen(viewModel = viewModel)
                            1 -> TeacherTaskManagementScreen(viewModel = viewModel)
                            2 -> TeacherTaskListScreen(viewModel = viewModel)
                            3 -> TeacherWorksClassViewScreen(viewModel = viewModel)
                            4 -> TeacherClassManagementUnifiedScreen(viewModel = viewModel)
                            else -> TeacherAnalyticsScreen(viewModel = viewModel)
                        }
                    }
                }
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
    val tabs = listOf(
        "Scratch编程" to Icons.Default.Code,
        "开源大厅" to Icons.Default.Explore,
        "学习任务" to Icons.Default.Assignment,
        "我的作品" to Icons.Default.Collections,
        "AI 辅助" to Icons.Default.AutoAwesome,
        "个人中心" to Icons.Default.Person
    )

    ScrollableTabRow(
        selectedTabIndex = selectedScreenIndex.coerceIn(0, tabs.size - 1),
        containerColor = Color(0xFF1A237E),
        contentColor = Color.White,
        edgePadding = 12.dp,
        indicator = { tabPositions ->
            val index = selectedScreenIndex.coerceIn(0, tabs.size - 1)
            if (index in tabPositions.indices) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                    height = 3.dp,
                    color = Color.White
                )
            }
        },
        divider = {}
    ) {
        tabs.forEachIndexed { index, (title, icon) ->
            val isSelected = selectedScreenIndex == index
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else Color(0xFFB0BEC5),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFFB0BEC5)
                        )
                    }
                }
            )
        }
    }
}

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
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class TemplateItem(val title: String, val desc: String, val code: String)

@Composable
fun MagicBoxDrawerPanel(
    webView: WebView?,
    viewModel: MainViewModel,
    onClose: () -> Unit,
    onInsertText: (String) -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier,
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

    val currentTemplate = showTemplateDialog
    if (currentTemplate != null) {
        val t = currentTemplate
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

