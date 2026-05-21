package com.example.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt
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

                if (!isRegisterMode) {
                    // TAB 切换
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
                            onClick = { selectedRoleTab = 0; password = "" },
                            text = { Text("学生通道", fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = selectedRoleTab == 1,
                            onClick = { selectedRoleTab = 1; password = "" },
                            text = { Text("教师通道", fontWeight = FontWeight.SemiBold) }
                        )
                    }

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
                    } else {
                        // 教师登录
                        OutlinedTextField(
                            value = teacherWorkId,
                            onValueChange = { teacherWorkId = it },
                            label = { Text("请输入工号 (初始化 T1001)") },
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
                                    Toast.makeText(context, "请填入学号和密码！", Toast.LENGTH_SHORT).show()
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
                        Text("还没有学生账号？点击注册新账号", color = Color(0xFFFF9800), fontWeight = FontWeight.Medium)
                    }

                } else {
                    // 学生注册模式
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
                        label = { Text("请输入新学号") },
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

                    // 班级下拉选择
                    if (classes.isNotEmpty()) {
                        val currentSelectedClass = classes.getOrNull(selectedClassIndex) ?: classes.first()
                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                            Card(
                                onClick = { classDropdownExpanded = !classDropdownExpanded },
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
                                        Text("所属班级", fontSize = 11.sp, color = Color.Gray)
                                        Text(
                                            text = "${currentSelectedClass.className} (${currentSelectedClass.grade})",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Icon(
                                        imageVector = if (classDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = classDropdownExpanded,
                                onDismissRequest = { classDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                classes.forEachIndexed { index, classItem ->
                                    DropdownMenuItem(
                                        text = { Text("${classItem.className} (${classItem.grade})") },
                                        onClick = {
                                            selectedClassIndex = index
                                            classDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text("暂无班级候选，请联系王老师创建。", color = Color.Red, modifier = Modifier.padding(bottom = 12.dp))
                    }

                    Button(
                        onClick = {
                            if (studentNum.isEmpty() || studentName.isEmpty() || password.isEmpty() || classes.isEmpty()) {
                                Toast.makeText(context, "请填齐所有的注册字段！", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val targetClass = classes[selectedClassIndex]
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
                            Text("创建并登录新账号", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

    Scaffold(
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
                    // 教师专属底模
                    NavigationBarItem(
                        selected = selectedScreenIndex == 0,
                        onClick = { selectedScreenIndex = 0 },
                        label = { Text("任务管理") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = selectedScreenIndex == 1,
                        onClick = { selectedScreenIndex = 1 },
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
                    1 -> TeacherWorksClassViewScreen(viewModel = viewModel)
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

    var localInputName by remember { mutableStateOf(draftName) }

    val context = LocalContext.current

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
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
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
                    webViewClient = WebViewClient()
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
                        
                        // Supports zoom gesture, extremely handy for small screens
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false

                        // Set Desktop User-Agent so Scratch doesn't lock adding sprites/backdrops on mobile device
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                        mediaPlaybackRequiresUserGesture = false
                    }
                    loadUrl("https://scratch.mit.edu/projects/editor/?embed=true")
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Super compact translucent label top-center
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xAA000000)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "✨ 正在进行 Scratch 互动编程 (全屏适配手机) ✨",
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }

        // Draggable & Foldable Control System
        if (!isExpanded) {
            // MINIMIZED MODE: Draggable round magic bubble
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                        }
                    }
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { isExpanded = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(48.dp),
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
            }
        } else {
            // EXPANDED MODE: Drag-enabled floating window card
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                    .padding(16.dp)
                    .width(360.dp)
                    .height(245.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
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
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount
                                    }
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
                                  text = "(由此拖动 ✥)",
                                  fontSize = 9.sp,
                                  color = Color(0xFFD2DDFC)
                              )
                          }
                          
                          // Minimize button
                          IconButton(
                              onClick = { isExpanded = false },
                              modifier = Modifier.size(24.dp)
                          ) {
                              Icon(
                                  imageVector = Icons.Default.Remove,
                                  contentDescription = "最小化",
                                  tint = Color.White,
                                  modifier = Modifier.size(16.dp)
                              )
                          }
                        }
                        
                        // TAB SELECTION BAR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEEEEEE))
                                .padding(vertical = 2.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("🤖 AI诊断", "💾 备份恢复", "📤 正式提交").forEachIndexed { index, title ->
                                val isSelected = selectedTab == index
                                Button(
                                    onClick = { selectedTab = index },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFFFF9800) else Color.White,
                                        contentColor = if (isSelected) Color.White else Color(0xFF555555)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp),
                                    elevation = if (isSelected) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
                                ) {
                                    Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                                    // Tab 2: Submissions
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
                            // 自动关联任务到工作区
                            viewModel.currentTaskId.value = task.taskId
                            viewModel.currentTaskName.value = task.taskName
                            viewModel.currentDraftName.value = "${task.taskName} - 草稿"
                            onGoToCode()
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
                                maxLines = 4,
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
                                    text = "去编程写作业 →",
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

// ==========================================
// 5. 作品与 AI 智能报告卡
// ==========================================
@Composable
fun StudentWorksScreen(viewModel: MainViewModel) {
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
    val tasks by viewModel.tasksList.collectAsState()

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
                    Text("王老师的管理事务中心 - 专属控制台", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            }
        }

        // 新建任务框
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🎺 为学生们新发任务 (学情下发)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = taskNameInput,
                        onValueChange = { taskNameInput = it },
                        label = { Text("输入任务名称") },
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

                    // 班级卡
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
                                modifier = Modifier.fillMaxWidth(0.8f)
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
                        label = { Text("具体编程任务指引与积木块要求详情") },
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("立即向选定班级下发发布", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("📊 已下发学情任务列表一览", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        }

        if (tasks.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("您目前还没有发布任何 Scratch 编程作业哦。", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            items(tasks) { t ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(t.taskName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("发派班级ID: ${t.classId}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Text(t.taskDetail, fontSize = 12.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("截止期限：${t.deadline}", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. 教师端 - 审阅本班作品与学情监控
// ==========================================
@Composable
fun TeacherWorksClassViewScreen(viewModel: MainViewModel) {
    // 教师端获取本班级的已提交作品，这里做个高保真展示或本地收集所有作品
    // 由于是老师全班看板，我们这里做一个简单的模拟展示，方便教师检查孩子们刚才的编程成果与 AI 报告！

    // 为了极致配合，我们可以简单抓当前学生的作品并加载
    val context = LocalContext.current

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
                    Text("全班学情过程性分析看板", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    Text("实时观察并纠治学生思路偏差，AI 已先予自动评定量化。", fontSize = 11.sp, color = Color.Gray)
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

        // 我们提供模拟学生刚才提交的漂亮列表，支持教师点阅
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                TeacherWorkSimulationItem(
                    studentName = "张小帅 (学号: S2001)",
                    workName = "猫咪漫步游戏 - 改进版本",
                    score = 92,
                    comment = "AI 姐姐评述：语法结构良好，合理使用了【碰壁反弹】积木、小猫能顺畅地来回漫步。"
                )
            }
            item {
                TeacherWorkSimulationItem(
                    studentName = "李小美 (学号: S2002)",
                    workName = "水果大作战 - 自由创作",
                    score = 85,
                    comment = "AI 姐姐评述：基本实现了水果随机掉落和盘子左右平移控制。希望后期加入加速控制机制增加趣味性。"
                )
            }
            item {
                TeacherWorkSimulationItem(
                    studentName = "王小飞 (学号: S3001)",
                    workName = "走迷宫 (我的第一份迷宫)",
                    score = 78,
                    comment = "AI 姐姐评述：甲虫能正常在通道走动，但是检测墙体的颜色积木有一丝色偏差，请老师进行纠正指导。"
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.logout()
                Toast.makeText(context, "已成功退登教师控制中心！", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("退出登录核心控制区", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TeacherWorkSimulationItem(studentName: String, workName: String, score: Int, comment: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(studentName, fontSize = 11.sp, color = Color.Gray)
                    Text(workName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.DarkGray)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Text(
                        text = "$score 分",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = comment,
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 17.sp
            )
        }
    }
}
