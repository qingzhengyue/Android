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
    
    // Draggable and foldable floating console state
    var isExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val animX = remember { androidx.compose.animation.core.Animatable(0f) }
    val animY = remember { androidx.compose.animation.core.Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isSnapped by remember { mutableStateOf(false) }
    var snappedSide by remember { mutableStateOf("right") }
    var hasInitializedPosition by remember { mutableStateOf(false) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    // 【终极重构：增强型 Bridge 接口】
    var projectLoaderInterface by remember { mutableStateOf<ScratchProjectLoaderInterface?>(null) }
    
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
        if (viewModel.aiLoading.value) {
            return
        }
        val webView = webViewInstance
        if (webView != null) {
            val callbackFired = java.util.concurrent.atomic.AtomicBoolean(false)
            
            coroutineScope.launch {
                kotlinx.coroutines.delay(5000L)
                if (!callbackFired.getAndSet(true)) {
                    viewModel.callAiAssistant(funcType, param = param)
                }
            }
            
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
                if (!callbackFired.getAndSet(true)) {
                    val cleaned = if (result != null && result != "null" && result != "\"\"") {
                        var s = result.trim()
                        if (s.startsWith("\"") && s.endsWith("\"") && s.length >= 2) {
                            s = s.substring(1, s.length - 1)
                            s = s.replace("\\\"", "\"").replace("\\\\", "\\")
                        }
                        s
                    } else ""
                    val maxCodeLen = 8000
                    val finalCode = if (cleaned.length > maxCodeLen) {
                        cleaned.take(maxCodeLen) + "\n...[代码已截断]"
                    } else {
                        cleaned
                    }
                    viewModel.callAiAssistant(funcType, if (finalCode.isNotBlank()) finalCode else null, param = param)
                }
            }
        } else {
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

    // 统一计算核心代码，防止无限死循环
    val activeProjectCode: String = remember(pendingTeacherSb3Path, workspaceLoadEvent, draftCode) {
        var code = ""
        if (!pendingTeacherSb3Path.isNullOrBlank()) {
            try {
                val file = java.io.File(pendingTeacherSb3Path!!)
                if (file.exists()) {
                    code = file.readText(Charsets.UTF_8)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (code.isBlank() && !workspaceLoadEvent.isNullOrBlank()) {
            code = workspaceLoadEvent!!
        }
        if (code.isBlank()) {
            code = draftCode
        }
        code
    }

    // ★ 关键重构：将生成 Sb3 放到 Android 协程安全区，直接赋给接口内存，彻底绕开 JS 字符串拦截！
    LaunchedEffect(activeProjectCode, projectLoaderInterface, webViewInstance) {
        if (activeProjectCode.isNotBlank()) {
            viewModel.currentDraftCode.value = activeProjectCode
            val sb3Base64 = try {
                com.example.data.Sb3Generator.createSb3Base64(activeProjectCode)
            } catch (e: Exception) { "" }
            
            projectLoaderInterface?.setProjectData(activeProjectCode, sb3Base64)
            val webView = webViewInstance
            if (webView != null) {
                loadProjectIntoWebView(webView)
            }
        }
    }

    var showMagicBoxDrawer by remember { mutableStateOf(false) }
    var localInputName by remember { mutableStateOf(draftName) }

    val coerceInSafe = remember {
        { value: Float, min: Float, max: Float ->
            if (max < min) min else value.coerceIn(min, max)
        }
    }

    LaunchedEffect(Unit) {
        android.widget.Toast.makeText(context, "双指捏合可缩放画布 🔍", android.widget.Toast.LENGTH_LONG).show()
    }

    val mirrors = remember {
        listOf(
            "https://editor.scratch-cn.cn/editor",                // 源1
            "https://scratch3.fun/editor",                        // 源2
            "https://turbowarp.org/editor",                       // 源3
            "file:///android_asset/scratch_blocks_viewer.html"    // 源4
        )
    }
    var currentMirrorIndex by rememberSaveable { mutableStateOf(0) }
    var scratchUrl by remember { mutableStateOf(mirrors[0]) }
    
    var isPageLoading by rememberSaveable { mutableStateOf(true) }
    var isAllFailed by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("正在加载 Scratch 编辑器 (1/${mirrors.size})...") }

    LaunchedEffect(scratchUrl, webViewInstance) {
        val webView = webViewInstance ?: return@LaunchedEffect
        isPageLoading = true
        isAllFailed = false
        loadingMessage = "正在加载 Scratch 编辑器 (${currentMirrorIndex + 1}/${mirrors.size})..."
        webView.loadUrl(scratchUrl)
        
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

    var uploadMessageCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uploadMessageCallback?.onReceiveValue(uris.toTypedArray())
        uploadMessageCallback = null
    }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF1A237E))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TopBarActionButton(
                    onClick = {
                        showMagicBoxDrawer = !showMagicBoxDrawer
                        if (!isTablet && showMagicBoxDrawer) {
                            showAiAssistSheet = false
                        }
                    },
                    icon = Icons.Default.Widgets,
                    text = "编程魔法盒 🎒",
                    containerColor = Color(0xFFF57C00) 
                )

                TopBarActionButton(
                    onClick = {
                        showAiAssistSheet = !showAiAssistSheet
                        if (!isTablet && showAiAssistSheet) {
                            showMagicBoxDrawer = false
                        }
                    },
                    icon = Icons.Default.AutoAwesome,
                    text = "智能精灵姐姐 👩‍💻",
                    containerColor = Color(0xFFC2185B) 
                )

                val userRole by viewModel.currentUserRole.collectAsState()
                if (userRole == "student") {
                    TopBarActionButton(
                        onClick = {
                            submitWorkName = draftName
                            showSubmitDialog = true
                        },
                        icon = Icons.Default.Send,
                        text = "提交作品 🚀",
                        containerColor = Color(0xFF1E88E5) 
                    )
                }

                var showMoreTopMenu by remember { mutableStateOf(false) }

                Box {
                    TopBarActionButton(
                        onClick = { showMoreTopMenu = true },
                        icon = Icons.Default.MoreVert,
                        text = "更多 🛠️",
                        containerColor = Color(0xFF455A64) 
                    )

                    DropdownMenu(
                        expanded = showMoreTopMenu,
                        onDismissRequest = { showMoreTopMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("手动换源 ⚡ (当前源: ${currentMirrorIndex + 1})", fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                showMoreTopMenu = false
                                if (currentMirrorIndex < mirrors.size - 1) {
                                    currentMirrorIndex++
                                } else {
                                    currentMirrorIndex = 0
                                }
                                scratchUrl = mirrors[currentMirrorIndex]
                                android.widget.Toast.makeText(context, "已手动切换到第 ${currentMirrorIndex + 1} 个极速镜像 ⚡", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("载入当前作品积木 🧩", fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                showMoreTopMenu = false
                                val webView = webViewInstance
                                val codeToLoad = draftCode 
                                if (webView != null && codeToLoad.isNotBlank()) {
                                    val sb3 = try { com.example.data.Sb3Generator.createSb3Base64(codeToLoad) } catch (e: Exception) { "" }
                                    projectLoaderInterface?.setProjectData(codeToLoad, sb3)
                                    loadProjectIntoWebView(webView)
                                    android.widget.Toast.makeText(context, "正在载入作品积木到编辑器，请稍候... ✨", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, "当前工作区暂无可载入的代码", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Collections, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("选择已提交作品导入 📁", fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                showMoreTopMenu = false
                                showLoadWorkDialog = true
                            }
                        )
                    }
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

        LaunchedEffect(screenWidthPx, screenHeightPx) {
            if (!hasInitializedPosition && screenWidthPx > 0) {
                val initX = screenWidthPx - buttonWidthPx - with(density) { 20.dp.toPx() }
                val initY = screenHeightPx - buttonHeightPx - with(density) { 80.dp.toPx() }
                animX.snapTo(initX)
                animY.snapTo(initY)
                hasInitializedPosition = true
            }
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (url.contains("login", ignoreCase = true) || 
                                url.contains("signin", ignoreCase = true) ||
                                url.contains("/community", ignoreCase = true) ||
                                url.contains("/register", ignoreCase = true) ||
                                url.contains("/signup", ignoreCase = true)) {
                                return true 
                            }
                            return false 
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isPageLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isPageLoading = false
                            
                            val codeToLoad = activeProjectCode
                            if (codeToLoad.isNotBlank()) {
                                val sb3 = try { com.example.data.Sb3Generator.createSb3Base64(codeToLoad) } catch(e:Exception){""}
                                projectLoaderInterface?.setProjectData(codeToLoad, sb3)
                                loadProjectIntoWebView(view)
                            }
                            
                            if (url != null && !url.startsWith("file:///")) {
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
                            }
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
                        override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                            request?.grant(request.resources)
                        }

                        override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                            if (message != null) {
                                android.util.Log.d("WebViewJS", "[${'$'}{message.messageLevel()}] ${'$'}{message.message()} (line ${'$'}{message.lineNumber()})")
                            }
                            return true
                        }

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
                        
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false

                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        mediaPlaybackRequiresUserGesture = false
                    }
                    
                    requestFocus()
                    
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
                    
                    // 【修复】保存 JavascriptInterface 实例引用，包含强壮的 Base64 桥接功能！
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
        } 
    } 


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
                                    val webView = webViewInstance
                                    if (webView != null && draft.blockCode.isNotBlank()) {
                                        val sb3 = try { com.example.data.Sb3Generator.createSb3Base64(draft.blockCode) } catch(e:Exception){""}
                                        projectLoaderInterface?.setProjectData(draft.blockCode, sb3)
                                        loadProjectIntoWebView(webView)
                                    }
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
                                    val webView = webViewInstance
                                    if (webView != null && work.workCode.isNotBlank()) {
                                        val sb3 = try { com.example.data.Sb3Generator.createSb3Base64(work.workCode) } catch(e:Exception){""}
                                        projectLoaderInterface?.setProjectData(work.workCode, sb3)
                                        loadProjectIntoWebView(webView)
                                    }
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

// 【终极重构：不再向 JS 拼接超长参数，改为无参数调用，让 JS 从原生接口拉取】
fun loadProjectIntoWebView(webView: WebView?) {
    if (webView == null) return

    val js = """
        (function() {
            try {
                window.__scratch_job_id = (window.__scratch_job_id || 0) + 1;
                var currentJobId = window.__scratch_job_id;

                // ★ 从原生接口安全拉取超长数据，避开 WebView 截断 Bug ★
                var rawData = window.AndroidProjectLoader ? window.AndroidProjectLoader.getProjectJson() : null;
                var base64Data = window.AndroidProjectLoader ? window.AndroidProjectLoader.getProjectBase64() : null;
                
                if ((!base64Data || base64Data.length === 0) && (!rawData || rawData.length === 0)) return "Empty data";
                
                function base64ToArrayBuffer(b64) {
                    var binaryString = window.atob(b64);
                    var len = binaryString.length;
                    var bytes = new Uint8Array(len);
                    for (var i = 0; i < len; i++) {
                        bytes[i] = binaryString.charCodeAt(i);
                    }
                    return bytes.buffer;
                }

                var buffer = null;
                if (base64Data && base64Data.length > 0) {
                    try { buffer = base64ToArrayBuffer(base64Data); } catch(e) {}
                }

                var attempts = 0;
                var maxAttempts = 120; // 最大轮询 60 秒
                var readyCount = 0;

                function getVm() {
                    if (window.vm) return window.vm;
                    if (window.scratch && window.scratch.vm) return window.scratch.vm;
                    if (window.__turboWarp__ && window.__turboWarp__.vm) return window.__turboWarp__.vm;
                    var frames = document.querySelectorAll('iframe');
                    for (var i = 0; i < frames.length; i++) {
                        try { if (frames[i].contentWindow && frames[i].contentWindow.vm) return frames[i].contentWindow.vm; } catch(e) {}
                    }
                    
                    // ★ 终极 React Fiber 深入搜索，就算被官网藏起来也能扒出来 ★
                    try {
                        var el = document.getElementById('scratch') || document.querySelector('[class^="gui_stage-wrapper_"]') || document.querySelector('[class*="gui_page-wrapper_"]');
                        if (el) {
                            var keys = Object.keys(el);
                            var reactKey = keys.find(function(k) { return k.startsWith('__reactInternalInstance') || k.startsWith('__reactFiber'); });
                            if (reactKey) {
                                var fiber = el[reactKey];
                                while (fiber) {
                                    if (fiber.stateNode && fiber.stateNode.props && fiber.stateNode.props.vm) return fiber.stateNode.props.vm;
                                    if (fiber.memoizedProps && fiber.memoizedProps.vm) return fiber.memoizedProps.vm;
                                    fiber = fiber.return;
                                }
                            }
                        }
                    } catch(e) {}
                    
                    return null;
                }

                function tryInject() {
                    if (window.__scratch_job_id !== currentJobId) return true; 
                    attempts++;

                    // 1. 🚀 TurboWarp 专属通道
                    if (window.loadProject && typeof window.loadProject === 'function') {
                        var twData = buffer ? buffer : JSON.parse(rawData);
                        window.loadProject(twData);
                        console.log("Success: Injected via TurboWarp API.");
                        return true; 
                    }

                    // 2. 🐢 标准版底层直接强插通道
                    var targetVm = getVm();
                    
                    if (!targetVm || !targetVm.editingTarget || !targetVm.runtime || targetVm.runtime.targets.length === 0) {
                        readyCount = 0; return false; 
                    }
                    
                    var loaders = document.querySelectorAll('[class*="loader_fullscreen"], [class*="loader_background"]');
                    var loaderVisible = false;
                    for (var i = 0; i < loaders.length; i++) {
                        if (window.getComputedStyle(loaders[i]).display !== 'none') {
                            loaderVisible = true; break;
                        }
                    }
                    if (loaderVisible) {
                        readyCount = 0; return false;
                    }

                    // 稳定期等待，防止官方网络延迟加载导致覆盖
                    readyCount++;
                    if (readyCount < 4) {
                        console.log("Waiting for React UI to settle... (" + readyCount + "/4)");
                        return false; 
                    }

                    console.log("Target VM locked. Executing safe payload.");

                    try {
                        var dataToLoad = buffer ? buffer : JSON.parse(rawData);
                        var loadPromise = targetVm.loadProject(dataToLoad);
                        
                        loadPromise.then(function() {
                            setTimeout(function() {
                                if (window.__scratch_job_id !== currentJobId) return;
                                
                                if (targetVm.emitWorkspaceUpdate) targetVm.emitWorkspaceUpdate();
                                if (targetVm.emitTargetsUpdate) targetVm.emitTargetsUpdate();
                                
                                var targets = targetVm.runtime.targets;
                                if (targets && targets.length > 0 && targetVm.setEditingTarget) {
                                    var stage = targets.find(function(t) { return t.isStage; });
                                    var sprite = targets.find(function(t) { return !t.isStage; }) || targets[0];
                                    
                                    if (stage) targetVm.setEditingTarget(stage.id);
                                    
                                    setTimeout(function() {
                                        if (window.__scratch_job_id !== currentJobId) return;
                                        if (sprite) targetVm.setEditingTarget(sprite.id);
                                        window.dispatchEvent(new Event('resize'));
                                        console.log("Standard Scratch load complete!");
                                    }, 50);
                                } else {
                                    window.dispatchEvent(new Event('resize'));
                                }
                            }, 150);
                        }).catch(function(e) { console.error("VM load error:", e); });
                        
                        return true;
                    } catch(e) {
                        console.error("Injection error:", e);
                        readyCount = 0;
                        return false;
                    }
                }

                if (!tryInject()) {
                    var timer = setInterval(function() {
                        if (tryInject() || attempts >= maxAttempts || window.__scratch_job_id !== currentJobId) {
                            clearInterval(timer);
                        }
                    }, 500);
                }
                return "Started deep-search injection for Job ID: " + currentJobId;
            } catch(e) {
                return "Fatal JS Error: " + e.message;
            }
        })();
    """.trimIndent()
    webView.evaluateJavascript(js) { res ->
        android.util.Log.d("ScratchLoadProject", "loadProject result: $res")
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

// ★ 终极数据隔离桥梁：直接在 Android 内存里存好大文件，让 JS 来这里“进货”
class ScratchProjectLoaderInterface(
    private var projectData: String = "", 
    private var base64Data: String = ""
) {
    fun setProjectData(data: String, base64: String = "") {
        projectData = data
        base64Data = base64
    }
    
    @android.webkit.JavascriptInterface
    fun getProjectData(): String {
        return projectData
    }

    @android.webkit.JavascriptInterface
    fun getProjectJson(): String {
        return projectData
    }
    
    @android.webkit.JavascriptInterface
    fun getProjectBase64(): String {
        return base64Data
    }
}
