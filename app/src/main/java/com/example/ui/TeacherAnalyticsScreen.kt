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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
    val tasksList by viewModel.tasksList.collectAsStateWithLifecycle()
    val analyticsData by viewModel.classAnalyticsState.collectAsStateWithLifecycle()

    var selectedClassId by remember { mutableIntStateOf(-1) } // -1 代表全部班级
    var selectedTaskId by remember { mutableIntStateOf(0) } // 0 代表全部任务

    LaunchedEffect(selectedClassId, selectedTaskId) {
        viewModel.loadClassAnalytics(selectedClassId, if (selectedTaskId == 0) null else selectedTaskId)
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC))
        ) {
            // 1. 班级切换 Selector
            ScrollableTabRow(
                selectedTabIndex = if (selectedClassId == -1) 0 else (classesList.indexOfFirst { it.classId == selectedClassId } + 1).coerceAtLeast(0),
                containerColor = Color.White,
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = selectedClassId == -1,
                    onClick = {
                        selectedClassId = -1
                        selectedTaskId = 0
                    },
                    text = { Text("📋 全部班级", fontWeight = FontWeight.Bold) }
                )
                classesList.forEach { clazz ->
                    Tab(
                        selected = clazz.classId == selectedClassId,
                        onClick = {
                            selectedClassId = clazz.classId
                            selectedTaskId = 0
                        },
                        text = { Text(clazz.className, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // 2. 任务筛选器 Filter Row
            val filteredTasks = remember(tasksList, selectedClassId) {
                if (selectedClassId == -1) tasksList else tasksList.filter { it.classId == selectedClassId }
            }

            ScrollableTabRow(
                selectedTabIndex = if (selectedTaskId == 0) 0 else (filteredTasks.indexOfFirst { it.taskId == selectedTaskId } + 1).coerceAtLeast(0),
                containerColor = Color(0xFFF1F5F9),
                edgePadding = 12.dp,
                divider = {}
            ) {
                Tab(
                    selected = selectedTaskId == 0,
                    onClick = { selectedTaskId = 0 },
                    text = { Text("🎯 全部任务", fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                )
                filteredTasks.forEach { task ->
                    Tab(
                        selected = task.taskId == selectedTaskId,
                        onClick = { selectedTaskId = task.taskId },
                        text = { Text(task.taskName, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                    )
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
                                title = "涉及学生",
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
                                title = "抄袭预警",
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
                                    text = "🕸️ Scratch 核心能力五维雷达图",
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
                                    text = "综合平均分：${String.format("%.1f", data.avgTotal)} / 100 分",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB)
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(12.dp))

                                // 五维得分对比条 (两层立体包裹布局：上层图标+名称与高对比得分，下层无断点渐变进度条)
                                DimensionScoreRow("语法表达", data.avgGrammar, 25f, Color(0xFF3B82F6), icon = Icons.Default.Code)
                                DimensionScoreRow("逻辑结构", data.avgLogic, 30f, Color(0xFF10B981), icon = Icons.Default.Psychology)
                                DimensionScoreRow("任务契合", data.avgTaskMatch, 25f, Color(0xFFF59E0B), icon = Icons.Default.AssignmentTurnedIn)
                                DimensionScoreRow("创新思维", data.avgCreative, 20f, Color(0xFF8B5CF6), icon = Icons.Default.AutoAwesome)
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
fun DimensionScoreRow(
    name: String,
    current: Float,
    max: Float,
    color: Color,
    icon: ImageVector? = null
) {
    val percent = (current / max).coerceIn(0f, 1f)
    val formattedCurrent = if (current % 1f == 0f) "${current.toInt()}" else String.format("%.1f", current)
    val formattedMax = "${max.toInt()}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        // 上层：左侧 [图标 + 维度名称]，右侧 [得分 / 满分] 高对比
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
            }

            // 右侧强化得分对比 (当前分突出放大，总分置灰缩小)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formattedCurrent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Text(
                    text = " / $formattedMax 分",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 下层：横跨整行的修长进度条（无任何游离小圆点），同色系柔和渐变填充 + 极浅底色轨道
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = percent)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.65f),
                                color
                            )
                        )
                    )
            )
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
