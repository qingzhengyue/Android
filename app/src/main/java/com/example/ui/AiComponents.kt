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
fun PromptChipsRow(
    prompts: List<PromptChipModel>,
    onChipClick: (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF0F5))
    ) {
        item {
            Text(
                text = "快捷引导:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC2185B),
                modifier = Modifier.padding(top = 8.dp, end = 4.dp)
            )
        }
        items(prompts) { prompt ->
            val isCloud = prompt.source == PromptSource.CLOUD
            val containerColor = if (isCloud) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface
            val borderColor = if (isCloud) Color(0xFF3B82F6) else Color(0xFFF8BBD0)

            Surface(
                onClick = { onChipClick(prompt.text) },
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(text = prompt.icon, modifier = Modifier.padding(end = 4.dp))
                    Text(
                        text = prompt.text,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCloud) Color(0xFF1D4ED8) else Color(0xFFC2185B)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageList(messages: List<ChatMessage>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        reverseLayout = true,
        contentPadding = PaddingValues(16.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            when (message) {
                is ChatMessage.TextMessage -> {
                    NormalTextBubble(message)
                }
                is ChatMessage.BlockIntroCardMessage -> {
                    BlockIntroCard(message)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun NormalTextBubble(message: ChatMessage.TextMessage) {
    val alignment = if (message.isFromStudent) Alignment.End else Alignment.Start
    val bgColor = if (message.isFromStudent) Color(0xFFE3F2FD) else Color.White
    val textColor = if (message.isFromStudent) Color(0xFF1565C0) else Color(0xFFC2185B)
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            border = BorderStroke(1.dp, Color(0xFFF8BBD0)),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                StyledAiResult(message.text)
            }
        }
    }
}

@Composable
fun BlockIntroCard(message: ChatMessage.BlockIntroCardMessage) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(0.85f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 头部：积木名称与图标
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info, 
                    contentDescription = null,
                    tint = Color(0xFF3B82F6)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message.blockName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 中间：积木图示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.White, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🧩 这里显示积木的 UI 截图", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 底部：文字说明
            Text(
                text = message.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569)
            )

            // 交互按钮
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { /* 触发一键应用到工程，或跳转到相关练习 */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("查看运行案例")
            }
        }
    }
}

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
    getLiveCodeAndCall: (String, String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val panelWidth = (configuration.screenWidthDp / 3).dp
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiResult by viewModel.aiResult.collectAsState()
    val aiResultType by viewModel.aiResultType.collectAsState()
    
    var creativePromptInput by remember { mutableStateOf(TextFieldValue("")) }
    var kbPromptInput by remember { mutableStateOf(TextFieldValue("")) }
    var customQuestionInput by remember { mutableStateOf(TextFieldValue("")) }
    
    // Lifted active tab and dialogue history list states (修复1-3)
    val activeTab by viewModel.aiActiveTab.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val mergedPrompts by viewModel.mergedPromptsFlow.collectAsState()

    // Capture and automatically process new AI replies via JSON protocol
    LaunchedEffect(aiResult) {
        val res = aiResult
        if (!res.isNullOrBlank()) {
            val historyTextMsg = viewModel.chatMessages.value.filterIsInstance<ChatMessage.TextMessage>().map { it.text }
            if (historyTextMsg.none { it == res }) {
                // If the result isn't already a card, try to parse it
                viewModel.processAiResponse(res)
            }
        }
    }

    Card(
        modifier = modifier,
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
                    .padding(horizontal = 12.dp, vertical = 10.dp),
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

            // Area 1: 顶部功能标签区 (Height 48dp, 12dp spacing, underline indicator)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color.White)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("语法纠错", "创意引导", "考点讲解").forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { viewModel.aiActiveTab.value = tab }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(
                                text = tab,
                                color = if (isSelected) Color(0xFFC2185B) else Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(2.dp))
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

            // Area 2: 中间内容展示区 (Modifier.weight(1f) 占满所有剩余空间)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section: Specific Controls depending on selected tab
                when (activeTab) {
                    "语法纠错" -> {
                        // Switch for Real-time detection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFF8BBD0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
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
                            onClick = { getLiveCodeAndCall("语法纠错", "") },
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
                            Column(modifier = Modifier.padding(10.dp)) {
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
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("循环", "变量", "广播", "坐标").forEach { chip ->
                                        Box(
                                            modifier = Modifier
                                                .height(32.dp)
                                                .background(Color(0xFFFFEEF0), RoundedCornerShape(12.dp))
                                                .border(1.dp, Color(0xFFF8BBD0), RoundedCornerShape(12.dp))
                                                .clickable { 
                                                    kbPromptInput = TextFieldValue(chip)
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

                // Current diagnostics AI result loader
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
                }

                // Area 2.5: 快捷引导芯片 (取代了原来的 Area 2.5)
                PromptChipsRow(
                    prompts = mergedPrompts,
                    onChipClick = { chipText ->
                        val cleanPrompt = chipText.replace(Regex("^[🧩💡🎓❓⚡📦]\\s*"), "")
                        when (activeTab) {
                            "语法纠错" -> customQuestionInput = TextFieldValue(cleanPrompt)
                            "创意引导" -> creativePromptInput = TextFieldValue(cleanPrompt)
                            else -> kbPromptInput = TextFieldValue(cleanPrompt)
                        }
                    }
                )

                // 聊天消息列表 (占用剩余可用空间)
                Box(modifier = Modifier.weight(1f)) {
                    ChatMessageList(messages = chatMessages)
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
                        if (currentInputValue.text.isNotBlank()) {
                            when (activeTab) {
                                "语法纠错" -> {
                                    val userQ = customQuestionInput.text
                                    customQuestionInput = TextFieldValue("")
                                    val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                    
                                    val newItem = DialogueHistoryItem(
                                        title = "【自定义提问】",
                                        question = userQ,
                                        answer = "正在全力思索中...",
                                        timestamp = timeStr
                                    )
                                    viewModel.dialogueHistoryList.value = listOf(newItem) + viewModel.dialogueHistoryList.value
                                    
                                    viewModel.callAiCustomQuestion(userQ) { response ->
                                        val updatedHistory = viewModel.dialogueHistoryList.value.map {
                                            if (it.question == userQ && it.answer == "正在全力思索中...") {
                                                it.copy(answer = response)
                                            } else {
                                                it
                                            }
                                        }
                                        viewModel.dialogueHistoryList.value = updatedHistory
                                    }
                                }
                                "创意引导" -> {
                                    val liveTheme = creativePromptInput.text
                                    if (liveTheme.isNotBlank()) {
                                        // viewModel.currentDraftName.value = liveTheme
                                    }
                                    getLiveCodeAndCall("创意引导", liveTheme)
                                    creativePromptInput = TextFieldValue("")
                                }
                                "考点讲解" -> {
                                    getLiveCodeAndCall("知识点讲解", kbPromptInput.text.ifBlank { "变量" })
                                    kbPromptInput = TextFieldValue("")
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

