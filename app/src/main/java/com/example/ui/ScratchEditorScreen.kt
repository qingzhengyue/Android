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
import androidx.compose.runtime.saveable.rememberSaveable
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
fun InteractiveScratchProgrammingScreen(viewModel: MainViewModel, onBackToHall: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600
    val drawerWidth = if (isTablet) 240.dp else configuration.screenWidthDp.dp

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
    var showSubmitDialog by remember { mutableStateOf(false) }
    var submitWorkName by remember { mutableStateOf("") }
    
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
    // 保存 JavascriptInterface 实例引用，以便后续通过接口传递项目数据（避免 base64/字符串拼接注入失败）
    var projectLoaderInterface by remember { mutableStateOf<ScratchProjectLoaderInterface?>(null) }
    // 教师查看学生作品 .sb3 自动注入路径（从 ViewModel 获取，页面加载完成后自动注入积木代码）
    val pendingTeacherSb3Path by viewModel.teacherPendingSb3Path.collectAsState()
    val realTimeCheckEnabled by viewModel.realTimeStateEnabled.collectAsState()
    var scratchChangeCounter by remember { mutableStateOf(0) }

    // Task 5: 2秒防抖离线自动保存与断点续传
    LaunchedEffect(draftCode, draftName) {
        if (draftCode.isNotBlank() && draftCode != "{}") {
            delay(2000L)
            viewModel.triggerDebouncedAutoSave(viewModel.currentTaskId.value, draftCode)
        }
    }

    fun getLiveCodeAndCall(funcType: String, param: String = "") {
        android.util.Log.d("GetLiveCode", "[START] getLiveCodeAndCall 被调用: funcType=$funcType, webViewInstance=${webViewInstance != null}")
        // 防止重复调用: 如果AI已经在加载中, 不再发起新请求
        if (viewModel.aiLoading.value) {
            android.util.Log.w("GetLiveCode", "[GUARD] AI调用正在进行中, 忽略重复的getLiveCodeAndCall: funcType=$funcType")
            return
        }
        val webView = webViewInstance
        if (webView != null) {
            // 安全机制: 防止evaluateJavascript回调永不触发导致UI卡死
            val callbackFired = java.util.concurrent.atomic.AtomicBoolean(false)
            
            // 启动超时保护协程: 5秒后如果回调还没触发，则直接调用AI(不携带代码)
            coroutineScope.launch {
                kotlinx.coroutines.delay(5000L)
                if (!callbackFired.getAndSet(true)) {
                    android.util.Log.w("GetLiveCode", "[TIMEOUT] evaluateJavascript callback 超时(5s) for $funcType, 直接调用AI(不携带代码)")
                    viewModel.callAiAssistant(funcType, param = param)
                } else {
                    android.util.Log.d("GetLiveCode", "[TIMEOUT] 超时协程结束: callback已触发, 不再重复调用")
                }
            }
            
            android.util.Log.d("GetLiveCode", "[JS] 开始 evaluateJavascript...")
            webView.evaluateJavascript(
                "(function() { " +
                "  try { " +
                "    var vm = window.vm || (window.scratch && window.scratch.vm); " +
                "    if (vm) { " +
                "      var result = {targets: []}; " +
                "      var targets = vm.runtime.targets; " +
                "      for (var i = 0; i < targets.length; i++) { " +
                "        var t = targets[i]; " +
                "        var blocks = []; " +
                "        if (t.blocks && t.blocks._blocks) { " +
                "          var allB = t.blocks._blocks; " +
                "          for (var id in allB) { " +
                "            if (allB.hasOwnProperty(id) && allB[id].opcode) { " +
                "              blocks.push({opcode: allB[id].opcode}); " +
                "            } " +
                "          } " +
                "        } " +
                "        result.targets.push({isStage: t.isStage, name: t.getName ? t.getName() : '', blocks: blocks}); " +
                "      } " +
                "      var json = JSON.stringify(result); " +
                "      if (json.length > 50000) { " +
                "        var simple = {targets: []}; " +
                "        for (var i = 0; i < targets.length; i++) { " +
                "          var ops = []; " +
                "          if (targets[i].blocks && targets[i].blocks._blocks) { " +
                "            var b = targets[i].blocks._blocks; " +
                "            for (var id in b) { if (b.hasOwnProperty(id) && b[id].opcode) ops.push(b[id].opcode); } " +
                "          } " +
                "          simple.targets.push({name: targets[i].getName ? targets[i].getName() : '', opcodes: ops}); " +
                "        } " +
                "        json = JSON.stringify(simple); " +
                "      } " +
                "      return json; " +
                "    } " +
                "    else if (typeof Blockly !== 'undefined') { " +
                "         var xml = Blockly.Xml.workspaceToDom(Blockly.mainWorkspace); " +
                "         return Blockly.Xml.domToText(xml); " +
                "    } " +
                "  } catch(e) { return 'ERROR:' + e.message; } " +
                "  return ''; " +
                "})()"
            ) { result: String? ->
                android.util.Log.d("GetLiveCode", "[CALLBACK] evaluateJavascript 回调触发: result长度=${result?.length ?: "null"}")
                // 如果超时保护已经触发过，则不再重复调用
                if (!callbackFired.getAndSet(true)) {
                    val cleaned = if (result != null && result != "null" && result != "\"\"") {
                        var s = result.trim()
                        if (s.startsWith("\"") && s.endsWith("\"") && s.length >= 2) {
                            s = s.substring(1, s.length - 1)
                            s = s.replace("\\\"", "\"").replace("\\\\", "\\")
                        }
                        s
                    } else ""
                    // 安全限制: 截断过长的代码(超过8000字符则截断), 防止发送给AI的Prompt过大导致卡死
                    val maxCodeLen = 8000
                    val finalCode = if (cleaned.length > maxCodeLen) {
                        android.util.Log.w("GetLiveCode", "[TRUNCATE] 代码过长(${cleaned.length}字符), 截断为${maxCodeLen}字符")
                        cleaned.take(maxCodeLen) + "\n...[代码已截断]"
                    } else {
                        cleaned
                    }
                    android.util.Log.d("GetLiveCode", "[CALL] 代码清理完成: cleaned长度=${cleaned.length}, 最终长度=${finalCode.length}, 即将调用callAiAssistant($funcType)")
                    viewModel.callAiAssistant(funcType, if (finalCode.isNotBlank()) finalCode else null, param = param)
                } else {
                    android.util.Log.w("GetLiveCode", "[SKIP] evaluateJavascript 回调触发时超时保护已执行过, 跳过重复调用")
                }
            }
            android.util.Log.d("GetLiveCode", "[WAIT] evaluateJavascript 已提交, 等待回调...")
        } else {
            android.util.Log.w("GetLiveCode", "[NULL] webViewInstance 为 null, 直接调用AI(不携带代码), funcType=$funcType")
            viewModel.callAiAssistant(funcType, param = param)
        }
    }

    LaunchedEffect(scratchChangeCounter) {
        if (realTimeCheckEnabled && scratchChangeCounter > 0) {
            delay(300)
            getLiveCodeAndCall("语法纠错")
        }
    }

    val workspaceLoadEvent by viewModel.workspaceLoadEvent.collectAsState()
    LaunchedEffect(workspaceLoadEvent, webViewInstance) {
        val code = workspaceLoadEvent
        val webView = webViewInstance
        if (code != null) {
            projectLoaderInterface?.setProjectData(code)
            if (webView != null) {
                if (viewModel.teacherPendingSb3Path.value != null) {
                    viewModel.workspaceLoadEvent.value = null
                } else {
                    loadProjectIntoWebView(webView, code, context)
                    viewModel.workspaceLoadEvent.value = null
                }
            }
        }
    }

    LaunchedEffect(draftCode) {
        if (draftCode.isNotBlank()) {
            projectLoaderInterface?.setProjectData(draftCode)
        }
    }

    // 手动触发 .sb3 加载（教师点击"载入作品积木"按钮时，页面已加载完毕，需延迟注入等待 VM 就绪）
    // 【修复】通过 @JavascriptInterface 接口传递数据，而非字符串拼接注入（大 JSON 字符串拼接会导致 JS 解析失败）
    // 【关键修复】将 webViewInstance 也作为 key，否则首次组合时 webViewInstance 为 null 直接 return，后续 WebView 创建后 effect 不会重跑
    LaunchedEffect(pendingTeacherSb3Path, webViewInstance) {
        val sb3Path = pendingTeacherSb3Path ?: return@LaunchedEffect
        val webView = webViewInstance ?: return@LaunchedEffect
        val loader = projectLoaderInterface ?: return@LaunchedEffect
        val sb3File = java.io.File(sb3Path)
        if (!sb3File.exists()) return@LaunchedEffect
        // 延迟 3 秒确保 Scratch VM 完全初始化
        kotlinx.coroutines.delay(3000)
        if (pendingTeacherSb3Path != sb3Path) return@LaunchedEffect
        val projectJson = sb3File.readText(Charsets.UTF_8)
        // 【关键修复】通过 @JavascriptInterface 实例设置项目数据，JS 端调用 getProjectData() 即可获取
        loader.setProjectData(projectJson)
        val loadJs = """
            (function() {
                var attempts = 0, maxAttempts = 120;
                function findVm() {
                    if (window.vm && window.vm.loadProject) return window.vm;
                    var frames = document.querySelectorAll('iframe');
                    for (var i = 0; i < frames.length; i++) {
                        try {
                            if (frames[i].contentWindow && frames[i].contentWindow.vm && frames[i].contentWindow.vm.loadProject)
                                return frames[i].contentWindow.vm;
                        } catch(e) {}
                    }
                    if (window.scratch && window.scratch.vm && window.scratch.vm.loadProject) return window.scratch.vm;
                    if (window.__turboWarp__ && window.__turboWarp__.vm && window.__turboWarp__.vm.loadProject) return window.__turboWarp__.vm;
                    return null;
                }
                function tryLoad() {
                    attempts++;
                    var vm = findVm();
                    if (!vm) return false;
                    try {
                        var raw = window.AndroidProjectLoader.getProjectData();
                        console.log('ProjectLoader: raw length=' + raw.length + ', first100=' + raw.substring(0, 100));
                        if (!raw || raw.length === 0) {
                            console.error('ProjectLoader: getProjectData() returned empty, interface not set');
                            return false;
                        }
                        var obj = JSON.parse(raw);
                        if (!obj.meta) { obj = { meta: { semver: '3.0.0', vm: '0.2.0', agent: 'Android' }, targets: obj.targets || obj } }
                        console.log('ProjectLoader: calling vm.loadProject, targets=' + (obj.targets ? obj.targets.length : 0));
                        vm.loadProject(obj).then(function() {
                            console.log('ProjectLoader: SUCCESS after ' + attempts + ' attempts');
                            if (vm.emitWorkspaceUpdate) vm.emitWorkspaceUpdate();
                            if (vm.runtime && vm.runtime.targets) vm.emit('targetsUpdate');
                        }).catch(function(err) { console.error('ProjectLoader: loadProject FAILED: ' + err); });
                    } catch(e) { console.error('ProjectLoader: parse FAILED: ' + e.message); }
                    return true;
                }
                if (!tryLoad()) {
                    var itv = setInterval(function() {
                        if (tryLoad() || attempts >= maxAttempts) {
                            clearInterval(itv);
                            if (attempts >= maxAttempts) console.log('ProjectLoader: Max attempts reached');
                        }
                    }, 1000);
                }
                return 'ProjectLoader started';
            })();
        """.trimIndent()
        webView.evaluateJavascript(loadJs, null)
        sb3File.delete()
        viewModel.teacherPendingSb3Path.value = null
    }

    var showMagicBoxDrawer by remember { mutableStateOf(false) }

    var localInputName by remember { mutableStateOf(draftName) }

    val coerceInSafe = remember {
        { value: Float, min: Float, max: Float ->
            if (max < min) min else value.coerceIn(min, max)
        }
    }

    // 首次进入编程界面缩放提示 (优化二)
    LaunchedEffect(Unit) {
        android.widget.Toast.makeText(context, "双指捏合可缩放画布 🔍", android.widget.Toast.LENGTH_LONG).show()
        if (draftCode.isNotBlank()) {
            viewModel.workspaceLoadEvent.value = draftCode
        }
    }

    // Scratch editor mirror URLs: 优先使用本地离线引擎，国内镜像与 TurboWarp 作为备用
    val mirrors = remember {
        listOf(
            "file:///android_asset/scratch_blocks_viewer.html", // 本地离线引擎（100%秒开免联网）
            "https://editor.scratch-cn.cn/",                     // 国内镜像 1
            "https://scratch3.fun/",                             // 国内镜像 2
            "https://turbowarp.org/"                             // TurboWarp 国际版
        )
    }
    var currentMirrorIndex by rememberSaveable { mutableStateOf(0) }
    var scratchUrl by remember { mutableStateOf(mirrors[0]) }
    
    var isPageLoading by rememberSaveable { mutableStateOf(true) }
    var isAllFailed by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("正在加载 Scratch 编辑器 (1/${mirrors.size})...") }

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
            // Left container: Return Button + Title with limited weight to avoid taking up too much space
            Row(
                modifier = Modifier
                    .weight(1f),
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
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 2. Draft & Task Information
                val displayTaskInfo = if (taskName.isNullOrBlank()) "自由创作" else "学习任务: $taskName"
                Text(
                    text = "📦 $draftName [$displayTaskInfo]",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right container: Buttons with explicit gap and no weight, ensuring enough space for all 3 buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 3. 编程魔法盒 Button
                TopBarActionButton(
                    onClick = {
                        showMagicBoxDrawer = !showMagicBoxDrawer
                        if (!isTablet && showMagicBoxDrawer) {
                            showAiAssistSheet = false
                        }
                    },
                    icon = Icons.Default.Widgets,
                    text = "编程魔法盒 🎒",
                    containerColor = Color(0xFFF57C00) // Deep warm amber
                )

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

                // 5. 智能精灵姐姐 Button
                TopBarActionButton(
                    onClick = {
                        showAiAssistSheet = !showAiAssistSheet
                        if (!isTablet && showAiAssistSheet) {
                            showMagicBoxDrawer = false
                        }
                    },
                    icon = Icons.Default.AutoAwesome,
                    text = "智能精灵姐姐 👩‍💻",
                    containerColor = Color(0xFFC2185B) // Deep rose ruby
                )

                // 6. 提交作品 Button (学生专属作品提交通道)
                val userRole by viewModel.currentUserRole.collectAsState()
                if (userRole == "student") {
                    TopBarActionButton(
                        onClick = {
                            submitWorkName = draftName
                            showSubmitDialog = true
                        },
                        icon = Icons.Default.Send,
                        text = "提交作品 🚀",
                        containerColor = Color(0xFF1E88E5) // Nice blue
                    )
                }
                                
                // 7. 载入作品积木 Button (教师专用，通过@JavascriptInterface加载到编辑器)
                val teacherViewingWorkspace by viewModel.teacherViewingWorkspace.collectAsState()
                if (teacherViewingWorkspace && draftCode.isNotBlank()) {
                    TopBarActionButton(
                        onClick = {
                            try {
                                // 【修复】通过 @JavascriptInterface 接口直接传递数据，避免写文件再读取的间接方式
                                val loader = projectLoaderInterface
                                if (loader != null && webViewInstance != null) {
                                    loader.setProjectData(draftCode)
                                    val loadJs = """
                                        (function() {
                                            var attempts = 0, maxAttempts = 60;
                                            function findVm() {
                                                if (window.vm && window.vm.loadProject) return window.vm;
                                                var frames = document.querySelectorAll('iframe');
                                                for (var i = 0; i < frames.length; i++) {
                                                    try {
                                                        if (frames[i].contentWindow && frames[i].contentWindow.vm && frames[i].contentWindow.vm.loadProject)
                                                            return frames[i].contentWindow.vm;
                                                    } catch(e) {}
                                                }
                                                if (window.scratch && window.scratch.vm && window.scratch.vm.loadProject) return window.scratch.vm;
                                                return null;
                                            }
                                            function tryLoad() {
                                                attempts++;
                                                var vm = findVm();
                                                if (!vm) return false;
                                                try {
                                                    var raw = window.AndroidProjectLoader.getProjectData();
                                                    if (!raw || raw.length === 0) return false;
                                                    var obj = JSON.parse(raw);
                                                    if (!obj.meta) { obj = { meta: { semver: '3.0.0', vm: '0.2.0', agent: 'Android' }, targets: obj.targets || obj } }
                                                    vm.loadProject(obj).then(function() {
                                                        console.log('ManualLoader: SUCCESS');
                                                        if (vm.emitWorkspaceUpdate) vm.emitWorkspaceUpdate();
                                                        if (vm.runtime && vm.runtime.targets) vm.emit('targetsUpdate');
                                                    }).catch(function(err) { console.error('ManualLoader: FAILED: ' + err); });
                                                } catch(e) { console.error('ManualLoader: parse FAILED: ' + e.message); }
                                                return true;
                                            }
                                            if (!tryLoad()) {
                                                var itv = setInterval(function() {
                                                    if (tryLoad() || attempts >= maxAttempts) clearInterval(itv);
                                                }, 500);
                                            }
                                            return 'ManualLoader started';
                                        })();
                                    """.trimIndent()
                                    webViewInstance?.evaluateJavascript(loadJs, null)
                                    android.widget.Toast.makeText(context, "正在载入作品积木到编辑器，请稍候... ✨", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    // 降级方案：写文件方式
                                    val sb3File = java.io.File(context.cacheDir, "manual_load_${'$'}{System.currentTimeMillis()}.sb3")
                                    sb3File.writeText(draftCode, Charsets.UTF_8)
                                    viewModel.teacherPendingSb3Path.value = sb3File.absolutePath
                                    android.widget.Toast.makeText(context, "正在载入作品积木到编辑器，请稍候... ✨", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "载入失败: ${'$'}{e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        icon = Icons.Default.Refresh,
                        text = "载入作品积木 🧩",
                        containerColor = Color(0xFF9C27B0) // Purple for teacher tool
                    )
                }
            }
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
                    },
                    modifier = Modifier.width(drawerWidth)
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 300.dp)
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
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            // Block navigation to login/community pages on mirror sites
                            if (url.contains("login", ignoreCase = true) || 
                                url.contains("signin", ignoreCase = true) ||
                                url.contains("/community", ignoreCase = true) ||
                                url.contains("/register", ignoreCase = true) ||
                                url.contains("/signup", ignoreCase = true)) {
                                android.util.Log.w("ScratchWebView", "Blocked redirect to: $url")
                                return true // Prevent navigation
                            }
                            return false // Allow normal navigation
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isPageLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isPageLoading = false
                            
                            // 注入口：通过给HTML注入自定义Viewport限制双指缩放范围并屏蔽三方冗余登录弹窗
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
                                        
                                        // 注入自定义 CSS 屏蔽三方冗余登录对话框与顶栏多余按钮
                                        var css = '[class*="modal_modal-overlay"], [class*="login-modal"], [class*="login-dialog"], [class*="prompt_prompt-"], [class*="alert_alert"], [class*="alert_alert-container"], div[class*="modal_modal-content"], div[class*="prompt_prompt-overlay"], .react-modal-sheet-container, .login-dialog, #login-dialog, .login-modal, .alert-container, [class*="menu-bar_account-info-group"], [class*="menu-bar_login-button"], [class*="menu-bar_mystuff-button"], div[class*="menu-bar_account-info-group"], div[class*="menu-bar_mystuff-button"] { display: none !important; }';
                                        var style = document.createElement('style');
                                        style.type = 'text/css';
                                        style.appendChild(document.createTextNode(css));
                                        head.appendChild(style);
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

                            // 【修复】移除 onPageFinished 中的重复 sb3 加载逻辑，统一由 LaunchedEffect(pendingTeacherSb3Path) 处理
                            // 避免 onPageFinished 和 LaunchedEffect 竞争导致文件被提前删除
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

                        // Bridge WebView console.log to Android Logcat for debugging
                        override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                            if (message != null) {
                                android.util.Log.d("WebViewJS", "[${'$'}{message.messageLevel()}] ${'$'}{message.message()} (line ${'$'}{message.lineNumber()})")
                            }
                            return true
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
                        allowFileAccessFromFileURLs = true
                        allowUniversalAccessFromFileURLs = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        
                        // 优化二：启用并且配置底层的 WebSettings 手势双指缩放支持
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false

                        // Desktop Chrome User-Agent to avoid mobile redirects and login walls
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
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
                    
                    // 【修复】保存 JavascriptInterface 实例引用，以便后续通过接口传递项目数据
                    val loaderInterface = ScratchProjectLoaderInterface(draftCode)
                    projectLoaderInterface = loaderInterface
                    addJavascriptInterface(ScratchJsInterface {
                        scratchChangeCounter++
                    }, "AndroidWorkspace")
                    addJavascriptInterface(loaderInterface, "AndroidProjectLoader")
                    addJavascriptInterface(loaderInterface, "AndroidBlockViewer")
                    
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Icon(imageVector = Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "❌ Scratch 编辑器加载失败",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "已尝试 ${mirrors.size} 个镜像源均无法访问，请检查：",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 诊断建议列表
                    Card(
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White),
                        elevation = androidx.compose.material3.CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🔍 可能的原因及解决方案：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                            Text("1️⃣ 平板 WiFi 未连接或信号弱", fontSize = 11.sp, color = Color.Gray)
                            Text("2️⃣ 网络需要登录认证（如校园网）", fontSize = 11.sp, color = Color.Gray)
                            Text("3️⃣ 防火墙/家长控制阻止了访问", fontSize = 11.sp, color = Color.Gray)
                            Text("4️⃣ DNS 解析失败，尝试切换 WiFi", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                currentMirrorIndex = 0
                                scratchUrl = mirrors[0]
                                isAllFailed = false
                                isPageLoading = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("重新加载 🔄", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        
                        Button(
                            onClick = {
                                // 尝试使用备用国际版 TurboWarp
                                currentMirrorIndex = mirrors.size - 1
                                scratchUrl = mirrors[currentMirrorIndex]
                                isAllFailed = false
                                isPageLoading = true
                                android.widget.Toast.makeText(context, "正在尝试国际版 TurboWarp...", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("换国际版 🌐", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
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
                    getLiveCodeAndCall = { type, param -> getLiveCodeAndCall(type, param) },
                    onClose = { showAiAssistSheet = false },
                    modifier = Modifier.width(drawerWidth)
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



    // 提交作品 Dialog
    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF1E88E5))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("提交 Scratch 编程作业 🚀", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "提交后，AI 精灵姐姐将立即帮您做代码拼搭分析并出具精细的诊断评分报告，您的辅导老师也会在教师端同步看到您的优秀作品并进行点评噢！✨",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color.DarkGray
                    )
                    OutlinedTextField(
                        value = submitWorkName,
                        onValueChange = { submitWorkName = it },
                        label = { Text("作品名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (submitWorkName.isBlank()) {
                            android.widget.Toast.makeText(context, "请输入作品名称后再提交噢！", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
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
                                viewModel.currentDraftName.value = submitWorkName
                                if (cleaned.isNotBlank()) {
                                    viewModel.currentDraftCode.value = cleaned
                                }
                                viewModel.submitWorkAndAiReport { msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                                showSubmitDialog = false
                            }
                        } else {
                            viewModel.currentDraftName.value = submitWorkName
                            viewModel.submitWorkAndAiReport { msg ->
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            }
                            showSubmitDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) {
                    Text("确认提交")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
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
                if (window.addBlockFromAndroid) {
                    return window.addBlockFromAndroid('$opcode', '$blockText');
                }
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
                if (window.loadProject) {
                    window.loadProject('$cleanJson');
                    return "Loaded via window.loadProject";
                }
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



class ScratchProjectLoaderInterface(private var projectData: String = "") {
    fun setProjectData(data: String) {
        projectData = data
    }
    
    @android.webkit.JavascriptInterface
    fun getProjectData(): String {
        return projectData
    }

    @android.webkit.JavascriptInterface
    fun getProjectJson(): String {
        return projectData
    }
}

