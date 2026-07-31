package com.example.ui

import org.json.JSONArray
import org.json.JSONObject

sealed class BlockSegment {
    data class Text(val content: String) : BlockSegment()
    data class Parameter(val value: String) : BlockSegment()
}

object BlockTextFormatter {
    
    fun formatBlock(opcode: String, blockJson: JSONObject?, blocksMap: JSONObject?): List<BlockSegment> {
        val inputs = blockJson?.optJSONObject("inputs")
        val fields = blockJson?.optJSONObject("fields")
        
        return when (opcode) {
            "event_whenflagclicked" -> listOf(BlockSegment.Text("当 绿旗 被点击"))
            "control_forever" -> listOf(BlockSegment.Text("重复执行"))
            "control_if" -> {
                val conditionStr = extractCondition(inputs, blocksMap)
                listOf(
                    BlockSegment.Text("如果 "),
                    BlockSegment.Parameter(conditionStr),
                    BlockSegment.Text(" 那么")
                )
            }
            "motion_movesteps" -> {
                val steps = getInputValue(inputs, "STEPS", "10", blocksMap)
                listOf(
                    BlockSegment.Text("移动 "),
                    BlockSegment.Parameter(steps),
                    BlockSegment.Text(" 步")
                )
            }
            "motion_changexby" -> {
                val dx = getInputValue(inputs, "DX", "10", blocksMap)
                listOf(
                    BlockSegment.Text("将 x 坐标增加 "),
                    BlockSegment.Parameter(dx)
                )
            }
            "motion_ifonedgebounce" -> listOf(BlockSegment.Text("碰到边缘就反弹"))
            "sensing_keypressed" -> {
                val key = getInputValue(inputs, "KEY_OPTION", "", blocksMap).ifEmpty {
                    getFieldValue(fields, "KEY_OPTION", "space")
                }
                listOf(
                    BlockSegment.Text("按下 "),
                    BlockSegment.Parameter(key),
                    BlockSegment.Text(" 键?")
                )
            }
            "looks_say" -> {
                val msg = getInputValue(inputs, "MESSAGE", "Hello!", blocksMap)
                listOf(
                    BlockSegment.Text("说 "),
                    BlockSegment.Parameter(msg)
                )
            }
            "control_repeat" -> {
                val times = getInputValue(inputs, "TIMES", "10", blocksMap)
                listOf(
                    BlockSegment.Text("重复执行 "),
                    BlockSegment.Parameter(times),
                    BlockSegment.Text(" 次")
                )
            }
            "motion_turnright" -> {
                val degrees = getInputValue(inputs, "DEGREES", "15", blocksMap)
                listOf(
                    BlockSegment.Text("右转 "),
                    BlockSegment.Parameter(degrees),
                    BlockSegment.Text(" 度")
                )
            }
            "motion_turnleft" -> {
                val degrees = getInputValue(inputs, "DEGREES", "15", blocksMap)
                listOf(
                    BlockSegment.Text("左转 "),
                    BlockSegment.Parameter(degrees),
                    BlockSegment.Text(" 度")
                )
            }
            "motion_setrotationstyle" -> {
                val style = getFieldValue(fields, "STYLE", "left-right")
                val displayStyle = when(style) {
                    "left-right" -> "左右翻转"
                    "don't rotate" -> "不可旋转"
                    "all around" -> "任意旋转"
                    "左右翻转" -> "左右翻转"
                    "不可旋转" -> "不可旋转"
                    "任意旋转" -> "任意旋转"
                    else -> style
                }
                listOf(
                    BlockSegment.Text("将旋转方式设为 "),
                    BlockSegment.Parameter(displayStyle)
                )
            }
            else -> listOf(BlockSegment.Text(BlockTranslator.getChineseName(opcode)))
        }
    }

    private fun extractCondition(inputs: JSONObject?, blocksMap: JSONObject?): String {
        if (inputs == null || blocksMap == null) return "<条件>"
        val conditionArr = inputs.optJSONArray("CONDITION") ?: return "<条件>"
        val condBlockId = conditionArr.optString(1)
        val condBlock = blocksMap.optJSONObject(condBlockId) ?: return "<条件>"
        
        return if (condBlock.optString("opcode") == "sensing_keypressed") {
            val condInputs = condBlock.optJSONObject("inputs")
            val condFields = condBlock.optJSONObject("fields")
            val key = getInputValue(condInputs, "KEY_OPTION", "").ifEmpty {
                getFieldValue(condFields, "KEY_OPTION", "space")
            }
            "按下 $key 键"
        } else {
            "<条件>"
        }
    }

    private fun getInputValue(inputs: JSONObject?, inputName: String, defaultValue: String, blocksMap: JSONObject? = null): String {
        if (inputs == null) return defaultValue
        val input = inputs.optJSONArray(inputName) ?: return defaultValue
        val valObj = input.opt(1)
        
        if (valObj is JSONArray) {
            return valObj.optString(1, defaultValue)
        } else if (valObj is String && blocksMap != null) {
            val childBlock = blocksMap.optJSONObject(valObj)
            if (childBlock != null) {
                val opcode = childBlock.optString("opcode")
                val childFields = childBlock.optJSONObject("fields")
                if (childFields != null) {
                    if (childFields.has("NUM")) return getFieldValue(childFields, "NUM", defaultValue)
                    if (childFields.has("TEXT")) return getFieldValue(childFields, "TEXT", defaultValue)
                    if (childFields.has("VALUE")) return getFieldValue(childFields, "VALUE", defaultValue)
                    if (childFields.has("VARIABLE")) return getFieldValue(childFields, "VARIABLE", defaultValue)
                }
            }
        }
        return defaultValue
    }
    
    private fun getFieldValue(fields: JSONObject?, fieldName: String, defaultValue: String): String {
        if (fields == null) return defaultValue
        val field = fields.optJSONArray(fieldName) ?: return defaultValue
        return field.optString(0, defaultValue)
    }
}
