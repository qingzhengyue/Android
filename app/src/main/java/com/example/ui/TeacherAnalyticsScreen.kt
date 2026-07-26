package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppRepository
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAnalyticsScreen(
    viewModel: MainViewModel
) {
    val classesList by viewModel.classesList.collectAsStateWithLifecycle()
    val analyticsData by viewModel.classAnalyticsState.collectAsStateWithLifecycle()
    var selectedClassId by remember { mutableIntStateOf(-1) }

    LaunchedEffect(classesList) {
        if (classesList.isNotEmpty() && selectedClassId == -1) {
            selectedClassId = classesList.first().classId
            viewModel.loadClassAnalytics(selectedClassId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "📊 学情大屏",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC))
        ) {
            // 班级选择器
            if (classesList.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = classesList.indexOfFirst { it.classId == selectedClassId }.coerceAtLeast(0),
                    containerColor = Color.White,
                    edgePadding = 16.dp
                ) {
                    classesList.forEach { clazz ->
                        Tab(
                            selected = clazz.classId == selectedClassId,
                            onClick = {
                                selectedClassId = clazz.classId
                                viewModel.loadClassAnalytics(clazz.classId)
                            },
                            text = { Text(clazz.className, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            val data = analyticsData
            if (data == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. 班级核心指标卡片
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricCard(
                                title = "班级总人数",
                                value = "${data.totalStudents} 人",
                                icon = Icons.Default.People,
                                color = Color(0xFF3B82F6),
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "实际提交率",
                                value = if (data.totalStudents > 0) "${(data.submittedCount * 100 / data.totalStudents)}%" else "0%",
                                icon = Icons.Default.Task,
                                color = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "抄袭预警数",
                                value = "${data.plagiarismRiskCount} 件",
                                icon = Icons.Default.Warning,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 2. 五维能力雷达图 (Radar Chart)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🕸️ 班级 Scratch 核心能力五维雷达图",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                AbilityRadarChart(
                                    grammar = data.avgGrammar / 25f,
                                    logic = data.avgLogic / 30f,
                                    taskMatch = data.avgTaskMatch / 25f,
                                    creative = data.avgCreative / 20f,
                                    total = data.avgTotal / 100f
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "班级平均综合分：${String.format("%.1f", data.avgTotal)} / 100 分",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB)
                                )
                            }
                        }
                    }

                    // 3. 高频易错点与 AI 辅导指引
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = Color(0xFF8B5CF6)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "💡 高频易错知识点 & AI 教学建议",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                data.commonErrors.forEach { err ->
                                    Surface(
                                        color = Color(0xFFF3E8FF),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "• $err",
                                            fontSize = 13.sp,
                                            color = Color(0xFF6B21A8),
                                            modifier = Modifier.padding(10.dp)
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
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, fontSize = 11.sp, color = Color.Gray)
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }
    }
}

@Composable
fun AbilityRadarChart(
    grammar: Float, // 0..1
    logic: Float,
    taskMatch: Float,
    creative: Float,
    total: Float
) {
    val labels = listOf("语法表达", "逻辑完整", "任务契合", "创新思维", "综合表现")
    val values = listOf(
        grammar.coerceIn(0.1f, 1f),
        logic.coerceIn(0.1f, 1f),
        taskMatch.coerceIn(0.1f, 1f),
        creative.coerceIn(0.1f, 1f),
        total.coerceIn(0.1f, 1f)
    )

    Canvas(
        modifier = Modifier
            .size(220.dp)
            .padding(16.dp)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2.2f
        val angleStep = (2 * Math.PI / 5).toFloat()

        // 绘制背景 5 边形网格
        for (step in 1..4) {
            val r = radius * (step / 4f)
            val gridPath = Path()
            for (i in 0 until 5) {
                val angle = i * angleStep - (Math.PI / 2).toFloat()
                val x = center.x + r * cos(angle)
                val y = center.y + r * sin(angle)
                if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
            }
            gridPath.close()
            drawPath(path = gridPath, color = Color(0xFFE2E8F0), style = Stroke(width = 2f))
        }

        // 绘制数据多边形
        val dataPath = Path()
        for (i in 0 until 5) {
            val angle = i * angleStep - (Math.PI / 2).toFloat()
            val r = radius * values[i]
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()

        drawPath(path = dataPath, color = Color(0x663B82F6))
        drawPath(path = dataPath, color = Color(0xFF2563EB), style = Stroke(width = 5f))
    }
}
