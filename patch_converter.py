with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "r") as f:
    content = f.read()

target = """            "looks_say" -> {
                val message = getInputValue(inputs, "MESSAGE", "Hello!")
                sb.append(indent).append("print(\"$message\")\n")
            }"""

replacement = """            "looks_say" -> {
                val message = getInputValue(inputs, "MESSAGE", "Hello!")
                sb.append(indent).append("print(\"$message\")\n")
            }
            "motion_ifonedgebounce" -> {
                sb.append(indent).append("bounce_if_on_edge()\n")
            }
            "motion_changexby" -> {
                val dx = getInputValue(inputs, "DX", "10")
                sb.append(indent).append("change_x_by($dx)\n")
            }
            "motion_changeyby" -> {
                val dy = getInputValue(inputs, "DY", "10")
                sb.append(indent).append("change_y_by($dy)\n")
            }
            "control_if" -> {
                // Try to get condition block
                var conditionStr = "True"
                val conditionArr = inputs?.optJSONArray("CONDITION")
                if (conditionArr != null) {
                    val condBlockId = conditionArr.optString(1)
                    val condBlock = blocks.optJSONObject(condBlockId)
                    if (condBlock != null && condBlock.optString("opcode") == "sensing_keypressed") {
                        val fields = condBlock.optJSONObject("fields")
                        val keyOpt = fields?.optJSONArray("KEY_OPTION")?.optString(0) ?: "space"
                        conditionStr = "key_pressed('$keyOpt')"
                    }
                }
                
                sb.append(indent).append("if $conditionStr:\n")
                val substack = getSubstackId(inputs)
                if (substack != null) {
                    sb.append(parseBlock(substack, blocks, indentLevel + 1))
                } else {
                    sb.append(indent).append("    pass\n")
                }
            }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "w") as f:
    f.write(content)
