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
fun StudentProfileScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val studentName by viewModel.currentUserName.collectAsState()
    val studentId by viewModel.currentIdentifier.collectAsState()
    val classId by viewModel.currentClassId.collectAsState()
    val studentClass by viewModel.currentClass.collectAsState()
    val works by viewModel.worksList.collectAsState()
    val tasks by viewModel.tasksList.collectAsState()

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("个人档案", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE8F5E9),
                    titleContentColor = Color(0xFF2E7D32)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Card(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9))
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("🧑‍💻", fontSize = 48.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(studentName.ifBlank { "未设置姓名" }, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            Spacer(modifier = Modifier.height(24.dp))

            // Info cards
            ProfileInfoRow(Icons.Default.School, "班级", studentClass?.className ?: "班级ID: $classId")
            ProfileInfoRow(Icons.Default.Badge, "学号", studentId.ifBlank { "未设置" })
            ProfileInfoRow(Icons.Default.CheckCircle, "完成任务数", "${tasks.size}")
            ProfileInfoRow(Icons.Default.ArtTrack, "提交作品数", "${works.size}")
        }
    }
}

@Composable
fun ProfileInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF388E3C), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, color = Color(0xFF616161), modifier = Modifier.weight(1f))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        }
    }
}
