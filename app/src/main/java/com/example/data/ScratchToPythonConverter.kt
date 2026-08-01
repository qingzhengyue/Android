package com.example.data

import org.json.JSONObject

object ScratchToPythonConverter {

    fun convertJsonToPython(scratchJson: String): String {
        return try {
            val jsonObject = JSONObject(scratchJson)
            val pythonCode = StringBuilder()
            
            if (jsonObject.has("targets")) {
                val targetsArray = jsonObject.getJSONArray("targets")
                for (i in 0 until targetsArray.length()) {
                    val targetObj = targetsArray.getJSONObject(i)
                    val targetName = targetObj.optString("name", "角色 ${i+1}")
                    val isStage = targetObj.optBoolean("isStage", false)
                    val displayName = if (isStage) "舞台 ($targetName)" else "角色 ($targetName)"
                    
                    if (targetObj.has("blocks")) {
                        val targetBlocks = targetObj.getJSONObject("blocks")
                        val topLevelBlocks = mutableListOf<String>()
                        
                        targetBlocks.keys().forEach { key ->
                            val block = targetBlocks.optJSONObject(key)
                            if (block != null && block.optBoolean("topLevel", false)) {
                                topLevelBlocks.add(key)
                            }
                        }
                        
                        if (topLevelBlocks.isEmpty()) {
                            var flagClickedBlockId: String? = null
                            targetBlocks.keys().forEach { key ->
                                val block = targetBlocks.optJSONObject(key)
                                if (block != null && block.optString("opcode") == "event_whenflagclicked") {
                                    flagClickedBlockId = key
                                }
                            }
                            if (flagClickedBlockId != null) {
                                topLevelBlocks.add(flagClickedBlockId!!)
                            } else if (targetBlocks.keys().hasNext()) {
                                topLevelBlocks.add(targetBlocks.keys().next())
                            }
                        }
                        
                        if (topLevelBlocks.isNotEmpty()) {
                            if (pythonCode.isNotEmpty()) pythonCode.append("\n")
                            pythonCode.append("# --- $displayName ---\n")
                            topLevelBlocks.forEach { blockId ->
                                pythonCode.append(parseBlock(blockId, targetBlocks, 0))
                                pythonCode.append("\n")
                            }
                        }
                    }
                }
            } else if (jsonObject.has("blocks")) {
                val blocks = jsonObject.getJSONObject("blocks")
                val topLevelBlocks = mutableListOf<String>()
                blocks.keys().forEach { key ->
                    val block = blocks.optJSONObject(key)
                    if (block != null && block.optBoolean("topLevel", false)) {
                        topLevelBlocks.add(key)
                    }
                }
                if (topLevelBlocks.isEmpty()) {
                    var flagClickedBlockId: String? = null
                    blocks.keys().forEach { key ->
                        val block = blocks.optJSONObject(key)
                        if (block != null && block.optString("opcode") == "event_whenflagclicked") {
                            flagClickedBlockId = key
                        }
                    }
                    if (flagClickedBlockId != null) {
                        topLevelBlocks.add(flagClickedBlockId!!)
                    } else if (blocks.keys().hasNext()) {
                        topLevelBlocks.add(blocks.keys().next())
                    }
                }
                topLevelBlocks.forEach { blockId ->
                    pythonCode.append(parseBlock(blockId, blocks, 0))
                    pythonCode.append("\n")
                }
            }
            
            val result = pythonCode.toString().trim()
            if (result.isEmpty()) {
                "# [空代码或未能识别出积木块]"
            } else {
                result
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "# [解析代码失败]: ${e.message}\n$scratchJson"
        }
    }

    private fun parseBlock(blockId: String?, blocks: JSONObject, indentLevel: Int): String {
        if (blockId == null || !blocks.has(blockId)) return ""
        val block = blocks.optJSONObject(blockId) ?: return ""
        val opcode = block.optString("opcode")
        
        val indent = "    ".repeat(indentLevel)
        val sb = StringBuilder()
        
        val inputs = block.optJSONObject("inputs")

        when (opcode) {
            "event_whenflagclicked" -> {
                sb.append(indent).append("def main():\n")
            }
            "control_forever" -> {
                sb.append(indent).append("while True:\n")
                val substack = getSubstackId(inputs)
                if (substack != null) {
                    val parsed = parseBlock(substack, blocks, indentLevel + 1)
                    if (parsed.isBlank()) {
                        sb.append(indent).append("    pass\n")
                    } else {
                        sb.append(parsed)
                    }
                } else {
                    sb.append(indent).append("    pass\n")
                }
            }
            "control_repeat" -> {
                val times = getInputValue(inputs, "TIMES", "10")
                sb.append(indent).append("for i in range($times):\n")
                val substack = getSubstackId(inputs)
                if (substack != null) {
                    sb.append(parseBlock(substack, blocks, indentLevel + 1))
                } else {
                    sb.append(indent).append("    pass\n")
                }
            }
            "motion_movesteps" -> {
                val steps = getInputValue(inputs, "STEPS", "10")
                sb.append(indent).append("move($steps)\n")
            }
            "motion_turnright" -> {
                val degrees = getInputValue(inputs, "DEGREES", "15")
                sb.append(indent).append("turn_right($degrees)\n")
            }
            "motion_turnleft" -> {
                val degrees = getInputValue(inputs, "DEGREES", "15")
                sb.append(indent).append("turn_left($degrees)\n")
            }
            "looks_say" -> {
                val message = getInputValue(inputs, "MESSAGE", "Hello!")
                sb.append(indent).append("print(\"$message\")\n")
            }
            "motion_setrotationstyle" -> {
                var style = "left-right"
                val fields = block.optJSONObject("fields")
                if (fields != null && fields.has("STYLE")) {
                    val f = fields.optJSONArray("STYLE")
                    if (f != null && f.length() > 0) style = f.optString(0)
                }
                sb.append(indent).append("sprite.set_rotation_style('$style')\n")
            }
            "motion_ifonedgebounce" -> {
                sb.append(indent).append("sprite.bounce_off_edge()\n")
            }            "motion_changexby" -> {
                val dx = getInputValue(inputs, "DX", "10")
                sb.append(indent).append("sprite.change_x($dx)\n")
            }            "motion_goto" -> {
                val toInput = inputs?.optJSONArray("TO")
                var toValue = "_random_"
                if (toInput != null) {
                    var toBlockId: String? = null
                    for (i in 1 until toInput.length()) {
                        val v = toInput.optString(i)
                        if (v.isNotEmpty() && blocks.has(v)) {
                            toBlockId = v
                            break
                        }
                    }
                    if (toBlockId != null) {
                        val toBlock = blocks.optJSONObject(toBlockId)
                        if (toBlock != null && toBlock.optString("opcode") == "motion_goto_menu") {
                            val fields = toBlock.optJSONObject("fields")
                            if (fields != null && fields.has("TO")) {
                                val f = fields.optJSONArray("TO")
                                if (f != null && f.length() > 0) toValue = f.optString(0)
                            }
                        }
                    }
                }
                sb.append(indent).append("sprite.go_to('$toValue')\n")
            }
            "motion_goto_menu" -> {
                // Ignore standalone menu blocks
            }
            "control_if" -> {
                var conditionStr = "True"
                val conditionArr = inputs?.optJSONArray("CONDITION")
                if (conditionArr != null) {
                    val condBlockId = conditionArr.optString(1)
                    val condBlock = blocks.optJSONObject(condBlockId)
                    if (condBlock != null && condBlock.optString("opcode") == "sensing_keypressed") {
                        val inputsOfCond = condBlock.optJSONObject("inputs")
                        var keyOpt = "space"
                        if (inputsOfCond != null && inputsOfCond.has("KEY_OPTION")) {
                            keyOpt = getInputValue(inputsOfCond, "KEY_OPTION", "space")
                        } else {
                            val fields = condBlock.optJSONObject("fields")
                            if (fields != null && fields.has("KEY_OPTION")) {
                                val f = fields.optJSONArray("KEY_OPTION")
                                if (f != null && f.length() > 0) keyOpt = f.optString(0)
                            }
                        }
                        conditionStr = "is_key_pressed('$keyOpt')"
                    }
                }
                
                sb.append(indent).append("if $conditionStr:\n")
                val substack = getSubstackId(inputs)
                if (substack != null) {
                    sb.append(parseBlock(substack, blocks, indentLevel + 1))
                } else {
                    sb.append(indent).append("    pass\n")
                }
            }
            else -> {
                sb.append(indent).append("# [未识别积木: $opcode]\n")
            }
        }

        // 处理顺序连接的下一个积木
        val nextBlock = block.optString("next", null)
        if (nextBlock != null && nextBlock != "null" && nextBlock.isNotEmpty()) {
            val nextIndent = if (opcode == "event_whenflagclicked") indentLevel + 1 else indentLevel
            sb.append(parseBlock(nextBlock, blocks, nextIndent))
        } else if (opcode == "event_whenflagclicked") {
            // 如果事件头后面没有跟着积木，需要一个 pass 防止语法错误
            sb.append(indent).append("    pass\n")
        }

        return sb.toString()
    }

    private fun getSubstackId(inputs: JSONObject?): String? {
        if (inputs == null) return null
        val substackArray = inputs.optJSONArray("SUBSTACK") ?: return null
        // Support both ["b3"] (from draft) and [2, "c"] (from valid JSON)
        val valObj = if (substackArray.length() == 1) substackArray.opt(0) else substackArray.opt(1)
        return if (valObj is String) valObj else null
    }

    private fun getInputValue(inputs: JSONObject?, inputName: String, defaultValue: String): String {
        if (inputs == null) return defaultValue
        val input = inputs.optJSONArray(inputName) ?: return defaultValue
        val valObj = input.opt(1)
        return if (valObj is org.json.JSONArray) {
            valObj.optString(1, defaultValue)
        } else if (valObj is String) {
            valObj
        } else {
            valObj?.toString() ?: defaultValue
        }
    }
}
