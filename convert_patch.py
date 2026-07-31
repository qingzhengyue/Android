with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "r") as f:
    content = f.read()

import re
pattern = re.compile(r'fun convertJsonToPython\(.*?private fun parseBlock', re.DOTALL)

replacement = r"""fun convertJsonToPython(scratchJson: String): String {
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

    private fun parseBlock"""

if pattern.search(content):
    content = pattern.sub(replacement, content)
    with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "w") as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Target not found")
