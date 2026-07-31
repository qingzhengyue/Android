with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "r") as f:
    content = f.read()

target = """            "looks_say" -> {
                val message = getInputValue(inputs, "MESSAGE", "Hello!")
                sb.append(indent).append("print(\\"$message\\")\\n")
            }"""

replacement = """            "looks_say" -> {
                val message = getInputValue(inputs, "MESSAGE", "Hello!")
                sb.append(indent).append("print(\\"$message\\")\\n")
            }
            "motion_ifonedgebounce" -> {
                sb.append(indent).append("sprite.bounce_off_edge()\\n")
            }
            "motion_changexby" -> {
                val dx = getInputValue(inputs, "DX", "10")
                sb.append(indent).append("sprite.change_x($dx)\\n")
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
                
                sb.append(indent).append("if $conditionStr:\\n")
                val substack = getSubstackId(inputs)
                if (substack != null) {
                    sb.append(parseBlock(substack, blocks, indentLevel + 1))
                } else {
                    sb.append(indent).append("    pass\\n")
                }
            }"""

content = content.replace(target, replacement)
with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "w") as f:
    f.write(content)
