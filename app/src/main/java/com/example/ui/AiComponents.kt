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

/**
 * 拟人化气泡对话组件 (ChatGPT 风格)
 * 区别“我”的提问（右侧蓝色/绿底气泡）与“精灵姐姐”的回答（左侧粉红/白底气泡 + 专属头像）
 */
@Composable
fun AiChatBubble(
    isFromUser: Boolean,
    title: String? = null,
    message: String,
    timestamp: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isFromUser) {
            // 精灵姐姐头像
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFC2185B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "精灵姐姐",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (isFromUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            // 角色名称与时间戳
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start
            ) {
                Text(
                    text = if (isFromUser) "我的提问 🙋‍♂️" else (title ?: "精灵姐姐 👩‍💻"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFromUser) Color(0xFF1565C0) else Color(0xFFC2185B)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timestamp,
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 气泡卡片
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isFromUser) Color(0xFFE3F2FD) else Color.White
                ),
                shape = if (isFromUser) RoundedCornerShape(14.dp, 2.dp, 14.dp, 14.dp) else RoundedCornerShape(2.dp, 14.dp, 14.dp, 14.dp),
                border = BorderStroke(1.dp, if (isFromUser) Color(0xFF90CAF9) else Color(0xFFF8BBD0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(modifier = Modifier.padding(10.dp)) {
                    if (isFromUser) {
                        Text(
                            text = message,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFF0D47A1)
                        )
                    } else {
                        StyledAiResult(message)
                    }
                }
            }
        }

        if (isFromUser) {
            Spacer(modifier = Modifier.width(6.dp))
            // 用户头像
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E88E5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "我",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


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
    val dialogueHistory by viewModel.dialogueHistoryList.collectAsState()

    // Capture and automatically append new AI replies into dialogue history
    LaunchedEffect(aiResult) {
        val res = aiResult
        val type = aiResultType
        if (!res.isNullOrBlank()) {
            val history = viewModel.dialogueHistoryList.value
            if (history.none { it.answer == res }) {
                val q = when (type) {
                    "创意引导" -> if (creativePromptInput.text.isNotBlank()) "主题: ${creativePromptInput.text}" else "自由扩展与创意优化"
                    "知识点讲解" -> if (kbPromptInput.text.isNotBlank()) "知识点: ${kbPromptInput.text}" else "知识考点"
                    "语法纠错" -> "语法与逻辑检测"
                    else -> "诊断检测"
                }
                val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                val newItem = DialogueHistoryItem(
                    title = "【$type】",
                    question = q,
                    answer = res,
                    timestamp = timeStr
                )
                viewModel.dialogueHistoryList.value = listOf(newItem) + history
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

            // Area 2: 中间内容展示区 (Modifier.weight(1f) 占满所有剩余空间、支持完整滚动及点击历史记录展开)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section: Specific Controls depending on selected tab
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                        
                                        // 【修复】用 Row + horizontalScroll 替代 FlowRow，
                                        // 避免 FlowRow 在 LazyColumn 内因无限高度约束导致布局测量死循环而卡死
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
                        val nowTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(text = "✨ 最新 AI 对话分析：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2185B))
                            Spacer(modifier = Modifier.height(4.dp))
                            AiChatBubble(
                                isFromUser = false,
                                title = "精灵姐姐 👩‍💻",
                                message = aiResult ?: "",
                                timestamp = nowTime
                            )
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
                                val updatedList = viewModel.dialogueHistoryList.value.map {
                                    if (it.id == item.id) it.copy(isExpanded = !it.isExpanded) else it
                                }
                                viewModel.dialogueHistoryList.value = updatedList
                            }
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFF8BBD0), RoundedCornerShape(12.dp))
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

                            // 渲染我的提问气泡
                            AiChatBubble(
                                isFromUser = true,
                                message = item.question,
                                timestamp = item.timestamp
                            )

                            // 渲染精灵姐姐解答气泡
                            AiChatBubble(
                                isFromUser = false,
                                title = "精灵姐姐 ${item.title}",
                                message = item.answer,
                                timestamp = item.timestamp
                            )
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

