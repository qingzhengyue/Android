package com.example.ui

import androidx.compose.ui.graphics.Color

object BlockTranslator {
    // 定义 Opcode 到中文文案的映射表
    private val blockNameMap = mapOf(
        "event_whenflagclicked" to "当 绿旗 被点击",
        "control_forever" to "重复执行",
        "control_if" to "如果 <条件> 那么",
        "motion_movesteps" to "移动 步",
        "motion_changexby" to "将 x 坐标增加",
        "motion_ifonedgebounce" to "碰到边缘就反弹",
        "motion_setrotationstyle" to "将旋转方式设为",
        "sensing_keypressed" to "按下 <按键> 键?",
        "looks_say" to "说"
    )

    // 获取中文名称，如果找不到则降级显示原始 opcode 或友好提示
    fun getChineseName(opcode: String): String {
        return blockNameMap[opcode] ?: "未知积木: $opcode"
    }
    
    // 获取积木对应的颜色主题（适配 Scratch 官方分类色）
    fun getBlockColor(opcode: String): Color {
        return when {
            opcode.startsWith("event_") -> Color(0xFFFFBF00) // 事件块：黄橙色
            opcode.startsWith("control_") -> Color(0xFFFFAB19) // 控制块：橙色
            opcode.startsWith("motion_") -> Color(0xFF4C97FF) // 运动块：蓝色
            opcode.startsWith("sensing_") -> Color(0xFF5CB1D6) // 侦测块：青色
            opcode.startsWith("looks_") -> Color(0xFF9966FF)   // 外观块：紫色
            else -> Color(0xFF888888)
        }
    }
}
