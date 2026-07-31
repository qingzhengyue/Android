import re

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

# Replace invocations
target_invocations = """                        EvaluationProgressRow(label = "1. 语法合规性 (检测积木完整拼接)", score = rep.grammarScore, maxScore = 25, color = Color(0xFF4CAF50))
                        EvaluationProgressRow(label = "2. 逻辑完整性 (检测逻辑环嵌套等)", score = rep.logicScore, maxScore = 30, color = Color(0xFF2196F3))
                        EvaluationProgressRow(label = "3. 任务匹配度 (检测任务目标要素)", score = rep.taskMatchScore, maxScore = 25, color = Color(0xFFFF9800))
                        EvaluationProgressRow(label = "4. 创意实现度 (分析交互及原创想法)", score = rep.creativeScore, maxScore = 20, color = Color(0xFF9C27B0))"""

replacement_invocations = """                        StatProgressBar(label = "1. 语法合规性 (检测积木完整拼接)", score = rep.grammarScore, maxScore = 25, gradientColors = listOf(Color(0xFF81C784), Color(0xFF388E3C)))
                        StatProgressBar(label = "2. 逻辑完整性 (检测逻辑环嵌套等)", score = rep.logicScore, maxScore = 30, gradientColors = listOf(Color(0xFF64B5F6), Color(0xFF1976D2)))
                        StatProgressBar(label = "3. 任务匹配度 (检测任务目标要素)", score = rep.taskMatchScore, maxScore = 25, gradientColors = listOf(Color(0xFFFFB74D), Color(0xFFF57C00)))
                        StatProgressBar(label = "4. 创意实现度 (分析交互及原创想法)", score = rep.creativeScore, maxScore = 20, gradientColors = listOf(Color(0xFFBA68C8), Color(0xFF7B1FA2)))"""

content = content.replace(target_invocations, replacement_invocations)

# Replace definition
target_def_regex = re.compile(r'fun EvaluationProgressRow\(label: String, score: Int, maxScore: Int, color: Color\) \{.*?\n\}', re.DOTALL)

replacement_def = """@Composable
fun StatProgressBar(
    label: String,
    score: Int,
    maxScore: Int,
    gradientColors: List<Color>
) {
    val progressRatio = (score.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f)
    
    // 使用 Spring 动画实现丝滑填充效果
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress_animation"
    )

    // 解析主标题和副标题，增加排版层次感
    val titleMatch = Regex("(.*?)\\\\s*\\\\((.*?)\\\\)").find(label)
    val mainTitle = titleMatch?.groupValues?.get(1) ?: label
    val subTitle = titleMatch?.groupValues?.get(2)

    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    withStyle(style = androidx.compose.ui.text.SpanStyle(
                        color = Color(0xFF2C3E50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )) {
                        append(mainTitle)
                    }
                    if (subTitle != null) {
                        append(" ")
                        withStyle(style = androidx.compose.ui.text.SpanStyle(
                            color = Color(0xFF95A5A6),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )) {
                            append("($subTitle)")
                        }
                    }
                }
            )
            
            Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    withStyle(style = androidx.compose.ui.text.SpanStyle(
                        color = gradientColors.last(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )) {
                        append(score.toString())
                    }
                    withStyle(style = androidx.compose.ui.text.SpanStyle(
                        color = Color(0xFFBDC3C7),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )) {
                        append(" / $maxScore 分")
                    }
                }
            )
        }

        // 现代感进度条轨道
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color(0xFFF0F3F4)) // 极浅的高级灰作为底色
        ) {
            // 渐变填充层
            Box(
                modifier = Modifier
                    .width(maxWidth * animatedProgress)
                    .fillMaxHeight()
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(gradientColors))
            )
        }
    }
}"""

content = target_def_regex.sub(replacement_def, content)

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)
