package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 环形综合得分仪 (Canvas 绘制 + 平滑数值动画)
 * 在 AI 评测看板顶部呈现现代、具有质感的环形仪表盘
 */
@Composable
fun CircularAiScoreGauge(
    score: Int,
    maxScore: Int = 100,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(score) {
        animationPlayed = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) (score.toFloat() / maxScore).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "gaugeProgress"
    )

    val animatedDisplayScore = (animatedProgress * maxScore).toInt()

    val (badgeText, badgeColor) = when {
        score >= 90 -> "卓越五星 ⭐⭐⭐⭐⭐" to Color(0xFF10B981)
        score >= 80 -> "四星优秀 ⭐⭐⭐⭐" to Color(0xFF3B82F6)
        score >= 70 -> "三星良好 ⭐⭐⭐" to Color(0xFFF59E0B)
        else -> "持续加油 ⭐⭐" to Color(0xFF8B5CF6)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF))
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🤖 AI 综合评测得分看板",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3730A3)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(130.dp)
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val strokeWidth = 14.dp.toPx()
                val arcRadius = size.width / 2 - strokeWidth / 2

                // 绘制背景底环
                drawArc(
                    color = Color(0xFFC7D2FE),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 绘制动画进度弧线
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF6366F1), Color(0xFF3B82F6), Color(0xFF10B981))
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$animatedDisplayScore",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E1B4B)
                )
                Text(
                    text = "/ $maxScore 分",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 定性评级胶囊
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(badgeColor.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = badgeText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}

/**
 * 现代化量化评分条 (学生端/教师端通用复用组件)
 * 带有数值变动动画、图标辅助、渐变填充轨道与量化定性评级胶囊
 *
 * @param dimensionName 维度名称 (如："逻辑结构")
 * @param score 实际得分 (Float 或 Int)
 * @param maxScore 满分 (默认100)
 * @param themeColor 维度主题色 (用于渐变和高亮)
 * @param icon 维度专属图标
 */
@Composable
fun AnimatedQuantitativeScoreBar(
    dimensionName: String,
    score: Float,
    maxScore: Float = 100f,
    themeColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    // 1. 自动计算评级标签 (量化评价定性辅助)
    val evaluationTag = when {
        score >= maxScore * 0.9f -> "优秀"
        score >= maxScore * 0.8f -> "良好"
        score >= maxScore * 0.6f -> "及格"
        else -> "待改进"
    }

    // 2. 增长动画逻辑
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = score) {
        animationPlayed = true
    }

    val currentProgress by animateFloatAsState(
        targetValue = if (animationPlayed) (score / maxScore).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "progressAnimation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        // 顶部信息区：图标 + 维度名 + 评级胶囊 + 精确量化数字
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // 左侧：维度名称与评级标签
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = dimensionName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151) // 深灰黑
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 评级小胶囊
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(themeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = evaluationTag,
                        fontSize = 11.sp,
                        color = themeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 右侧：精确量化数字
            Row(verticalAlignment = Alignment.Bottom) {
                val formattedScore = if (score % 1f == 0f) "${score.toInt()}" else String.format("%.1f", score)
                Text(
                    text = formattedScore,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themeColor
                )
                Text(
                    text = " / ${maxScore.toInt()} 分",
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF), // 浅灰色
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 底部进度条区：底层轨道 + 渐变增长轨道
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp) // 稍粗的进度条显得更有分量
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFFF3F4F6)) // 轨道底色
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = currentProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                themeColor.copy(alpha = 0.5f),
                                themeColor // 右侧颜色更深，形成光泽感
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun AnimatedQuantitativeScoreBar(
    dimensionName: String,
    score: Int,
    maxScore: Int = 100,
    themeColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    AnimatedQuantitativeScoreBar(
        dimensionName = dimensionName,
        score = score.toFloat(),
        maxScore = maxScore.toFloat(),
        themeColor = themeColor,
        modifier = modifier,
        icon = icon
    )
}

