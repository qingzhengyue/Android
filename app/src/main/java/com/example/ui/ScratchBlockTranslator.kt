package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Scratch 积木中文化映射工具类 (Block Translator)
 * 负责将 Scratch 底层 Opcode 映射为高颜值的中文描述文本，并提供对应的 Jetpack Compose 渲染组件。
 */
object ScratchBlockTranslator {

    // 中文字典 Map：将英文 Opcode 映射为中文描述
    private val blockDictionary = mapOf(
        "event_whenflagclicked" to "当 绿旗 被点击",
        "event_whenkeypressed" to "当按下 空格 键",
        "event_whenthisspriteclicked" to "当角色被点击",
        "control_wait" to "等待 1 秒",
        "control_repeat" to "重复执行 10 次",
        "control_forever" to "重复执行",
        "control_if" to "如果 < > 那么",
        "motion_movesteps" to "移动 10 步",
        "motion_turnright" to "右转 15 度",
        "motion_turnleft" to "左转 15 度",
        "motion_changexby" to "将 x 坐标增加 10",
        "motion_ifonedgebounce" to "碰到边缘就反弹",
        "motion_setrotationstyle" to "将旋转方式设为",
        "sensing_touchingobject" to "碰到鼠标指针?",
        "sensing_askandwait" to "询问 你叫什么名字? 并等待",
        "sensing_keypressed" to "按下 <空格> 键?",
        "looks_say" to "说 Hello!"
    )

    // 色彩映射：根据积木类别返回对应的官方色彩风格
    private val colorMap = mapOf(
        "event" to Color(0xFFFFBF00),      // 事件黄
        "control" to Color(0xFFFFAB19),    // 控制橙
        "motion" to Color(0xFF4C97FF),     // 运动蓝
        "sensing" to Color(0xFF5CB1D6),    // 侦测青
        "looks" to Color(0xFF9966FF),      // 外观紫
        "sound" to Color(0xFFCF63CF),      // 声音粉
        "operators" to Color(0xFF59C059),  // 运算绿
        "data" to Color(0xFFFF8C1A)        // 变量橙
    )

    /**
     * 将 Opcode 转换为中文描述，如果没有匹配项则返回原 Opcode
     */
    fun translateOpcode(opcode: String): String {
        return blockDictionary[opcode] ?: opcode
    }

    /**
     * 解析 Opcode 前缀（如 motion, control）并返回对应的背景色
     */
    fun getColorForOpcode(opcode: String): Color {
        val category = opcode.split("_").firstOrNull() ?: ""
        return colorMap[category] ?: Color.LightGray
    }
}

/**
 * 适配 Jetpack Compose 的高颜值积木卡片式渲染组件 (Chip/Card)
 * 保持积木原有的色彩风格，提供圆角和合适的内边距。
 */
@Composable
fun ScratchBlockCard(opcode: String, modifier: Modifier = Modifier) {
    val text = ScratchBlockTranslator.translateOpcode(opcode)
    val bgColor = ScratchBlockTranslator.getColorForOpcode(opcode)

    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .background(color = bgColor, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}
