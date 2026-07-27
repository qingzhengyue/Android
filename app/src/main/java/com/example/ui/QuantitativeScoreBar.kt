package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 现代化量化评分条 (学生端/教师端通用复用组件)
 * 带有数值变动动画、渐变填充轨道与量化定性评级胶囊
 *
 * @param dimensionName 维度名称 (如："逻辑结构")
 * @param score 实际得分 (Float 或 Int)
 * @param maxScore 满分 (默认100)
 * @param themeColor 维度主题色 (用于渐变和高亮)
 */
@Composable
fun AnimatedQuantitativeScoreBar(
    dimensionName: String,
    score: Float,
    maxScore: Float = 100f,
    themeColor: Color,
    modifier: Modifier = Modifier
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
        // 顶部信息区：维度名 + 评级胶囊 + 精确量化数字
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // 左侧：维度名称与评级标签
            Row(verticalAlignment = Alignment.CenterVertically) {
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
    modifier: Modifier = Modifier
) {
    AnimatedQuantitativeScoreBar(
        dimensionName = dimensionName,
        score = score.toFloat(),
        maxScore = maxScore.toFloat(),
        themeColor = themeColor,
        modifier = modifier
    )
}
