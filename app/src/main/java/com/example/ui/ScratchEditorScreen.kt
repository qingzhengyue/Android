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
import androidx.compose.ui.text.input.TextFieldValue
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.webkit.ValueCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun InteractiveScratchProgrammingScreen(viewModel: MainViewModel, onBackToHall: () -> Unit) {
    val context = LocalContext.current
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
    var showMagicBoxDrawer by remember { mutableStateOf(false) }
    var saveNameDialog by remember { mutableStateOf(false) }
    var showLoadDraftDialog by remember { mutableStateOf(false) }
    var showLoadWorkDialog by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }
    var submitWorkName by remember { mutableStateOf("") }
    
    val coroutineScope = rememberCoroutineScope()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var projectLoaderInterface by remember { mutableStateOf<ScratchProjectLoaderInterface?>(null) }
    val pendingTeacherSb3Path by viewModel.teacherPendingSb3Path.collectAsState()
    val realTimeCheckEnabled by viewModel.realTimeStateEnabled.collectAsState()
    var scratchChangeCounter by remember { mutableStateOf(0) }

    // 自动保存
    LaunchedEffect(draftCode) {
        if (draftCode.isNotBlank() && draftCode != "{}") {
            delay(2000L)
            viewModel.triggerDebouncedAutoSave(viewModel.currentTaskId.value, draftCode)
        }
    }

    fun getLiveCodeAndCall(funcType: String, param: String = "") {
        if (viewModel.aiLoading.value) return
        val webView = webViewInstance ?: return
        val callbackFired = java.util.concurrent.atomic.AtomicBoolean(false)
        coroutineScope.launch {
            delay(6000L)
            if (!callbackFired.getAndSet(true)) {
                viewModel.callAiAssistant(funcType, param = param)
            }
        }
        webView.evaluateJavascript(
            "(function(){ try { var vm = window.vm || (window.scratch && window.scratch.vm); if(vm){ return JSON.stringify(vm.runtime.targets.map(t=>({name:t.getName(), blocks:Object.keys(t.blocks._blocks).map(id=>t.blocks._blocks[id].opcode)}))); } }catch(e){} return ''; })()"
        ) { result ->
            if (!callbackFired.getAndSet(true)) {
                val cleaned = result?.trim('\"')?.replace("\\\"", "\"") ?: ""
                viewModel.callAiAssistant(funcType, if (cleaned.isNotBlank()) cleaned else null, param = param)
            }
        }
    }

    val workspaceLoadEvent by viewModel.workspaceLoadEvent.collectAsState()
    LaunchedEffect(workspaceLoadEvent, webViewInstance) {
        val code = workspaceLoadEvent
        val webView = webViewInstance
        if (code != null && webView != null) {
            projectLoaderInterface?.setProjectData(code)
            loadProjectIntoWebView(webView, code, context)
            viewModel.workspaceLoadEvent.value = null
        }
    }

    LaunchedEffect(draftCode) {
        if (draftCode.isNotBlank()) projectLoaderInterface?.setProjectData(draftCode)
    }

    // 换源逻辑
    val mirrors = remember {
        listOf(
            "file:///android_asset/scratch_blocks_viewer.html",
            "https://editor.scratch-cn.cn/",
            "https://scratch3.fun/",
            "https://turbowarp.org/"
        )
    }
    var currentMirrorIndex by rememberSaveable { mutableStateOf(0) }
    var scratchUrl by remember { mutableStateOf(mirrors[0]) }
    var isPageLoading by rememberSaveable { mutableStateOf(true) }
    var isAllFailed by remember { mutableStateOf(false) }

    LaunchedEffect(scratchUrl, webViewInstance) {
        val webView = webViewInstance ?: return@LaunchedEffect
        isPageLoading = true
        isAllFailed = false
        webView.loadUrl(scratchUrl)
        
        if (scratchUrl.startsWith("http")) {
            delay(10000L)
            if (isPageLoading && !isAllFailed) {
                if (currentMirrorIndex < mirrors.size - 1) {
                    currentMirrorIndex++
                    scratchUrl = mirrors[currentMirrorIndex]
                } else {
                    isAllFailed = true
                    isPageLoading = false
                }
            }
        } else {
            delay(3000L) // 本地源保底
            if (isPageLoading) isPageLoading = false
        }
    }

    // 修复文件上传无法重复触发的问题
    var uploadMessageCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNullOrEmpty()) {
            uploadMessageCallback?.onReceiveValue(null)
        } else {
            uploadMessageCallback?.onReceiveValue(uris.toTypedArray())
        }
        uploadMessageCallback = null
    }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { activity?.requestedOrientation = originalOrientation }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp).background(Color(0xFF1A237E)).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToHall) { Icon(Icons.Default.ArrowBack, "返回", tint = Color.White) }
                Text("📦 $draftName", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TopBarActionButton(onClick = { showMagicBoxDrawer = !showMagicBoxDrawer }, Icons.Default.Widgets, "魔法盒", Color(0xFFF57C00))
                TopBarActionButton(onClick = { showAiAssistSheet = !showAiAssistSheet }, Icons.Default.AutoAwesome, "AI精灵", Color(0xFFC2185B))
                if (viewModel.currentUserRole.collectAsState().value == "student") {
                    TopBarActionButton(onClick = { submitWorkName = draftName; showSubmitDialog = true }, Icons.Default.Send, "提交", Color(0xFF1E88E5))
                }
                var showMore by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMore = true }) { Icon(Icons.Default.MoreVert, "更多", tint = Color.White) }
                    DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                        DropdownMenuItem(
                            text = { Text("手动换源 (当前源: ${currentMirrorIndex + 1})") },
                            onClick = { 
                                showMore = false
                                currentMirrorIndex = (currentMirrorIndex + 1) % mirrors.size
                                scratchUrl = mirrors[currentMirrorIndex]
                            }
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            AnimatedVisibility(visible = showMagicBoxDrawer) {
                MagicBoxDrawerPanel(webViewInstance, viewModel, { showMagicBoxDrawer = false }, {}, Modifier.width(drawerWidth))
            }
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    isPageLoading = true
                                }
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isPageLoading = false
                                    // 核心修复：自动恢复数据的轮询脚本
                                    val script = """
                                        (function() {
                                            function doLoad() {
                                                var loader = window.AndroidProjectLoader || window.AndroidBlockViewer;
                                                if (!loader) return false;
                                                var data = loader.getProjectData();
                                                if (!data || data.length < 20 || data === '{}') return false;
                                                if (window.loadProject) { window.loadProject(data); return true; }
                                                var vm = window.vm || (window.scratch && window.scratch.vm);
                                                if (!vm) {
                                                    var fs = document.querySelectorAll('iframe');
                                                    for(var i=0; i<fs.length; i++) {
                                                        try { if(fs[i].contentWindow && fs[i].contentWindow.vm) { vm = fs[i].contentWindow.vm; break; } } catch(e){}
                                                    }
                                                }
                                                if (vm && vm.loadProject) {
                                                    try { vm.loadProject(JSON.parse(data)); return true; } catch(e) {}
                                                }
                                                return false;
                                            }
                                            var count = 0;
                                            var itv = setInterval(function() {
                                                count++;
                                                if (doLoad() || count > 15) clearInterval(itv);
                                            }, 1000);
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(script, null)
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onShowFileChooser(wv: WebView?, cb: ValueCallback<Array<Uri>>?, p: FileChooserParams?): Boolean {
                                    if (uploadMessageCallback != null) {
                                        uploadMessageCallback?.onReceiveValue(null)
                                    }
                                    uploadMessageCallback = cb
                                    try { 
                                        fileChooserLauncher.launch("*/*") 
                                    } catch(e:Exception){ 
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
                                allowFileAccess = true
                                allowContentAccess = true
                                allowFileAccessFromFileURLs = true
                                allowUniversalAccessFromFileURLs = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }
                            val loader = ScratchProjectLoaderInterface(draftCode)
                            projectLoaderInterface = loader
                            addJavascriptInterface(loader, "AndroidProjectLoader")
                            addJavascriptInterface(loader, "AndroidBlockViewer")
                            addJavascriptInterface(ScratchJsInterface { scratchChangeCounter++ }, "AndroidWorkspace")
                            webViewInstance = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (isPageLoading) {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xCCFFFFFF)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF1E88E5))
                    }
                }
            }
            AnimatedVisibility(visible = showAiAssistSheet) {
                AiAssistPanel(webViewInstance, viewModel, realTimeCheckEnabled, { viewModel.setRealTimeStateEnabled(it) }, { t, p -> getLiveCodeAndCall(t, p) }, { showAiAssistSheet = false }, Modifier.width(drawerWidth))
            }
        }
    }

    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("提交作品 🚀") },
            text = { OutlinedTextField(value = submitWorkName, onValueChange = { submitWorkName = it }, label = { Text("作品名称") }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { Button(onClick = {
                viewModel.currentDraftName.value = submitWorkName
                viewModel.submitWorkAndAiReport { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                showSubmitDialog = false
            }) { Text("确认提交") } }
        )
    }
}

fun loadProjectIntoWebView(webView: WebView?, pJson: String, context: android.content.Context) {
    if (webView == null || pJson.isBlank() || pJson == "{}") return
    val cleanJson = pJson.replace("'", "\\'").replace("\n", " ").replace("\r", " ")
    val js = """
        (function() {
            var count = 0;
            function tryLoad() {
                count++;
                if (window.loadProject) { window.loadProject('$cleanJson'); return true; }
                var vm = window.vm || (window.scratch && window.scratch.vm);
                if (vm && vm.loadProject) {
                    try { vm.loadProject(JSON.parse('$cleanJson')); return true; } catch(e) {}
                }
                return false;
            }
            var itv = setInterval(function() { if (tryLoad() || count > 20) clearInterval(itv); }, 1000);
        })();
    """.trimIndent()
    webView.evaluateJavascript(js, null)
}

class ScratchJsInterface(private val onChanged: () -> Unit) {
    @android.webkit.JavascriptInterface
    fun onCodeChanged() { onChanged() }
}

class ScratchProjectLoaderInterface(private var projectData: String = "") {
    fun setProjectData(data: String) { projectData = data }
    @android.webkit.JavascriptInterface
    fun getProjectData(): String = projectData
    @android.webkit.JavascriptInterface
    fun getProjectJson(): String = projectData
}
