with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "r") as f:
    content = f.read()

target = """            val blocks = if (jsonObject.has("targets")) {
                jsonObject.getJSONArray("targets").getJSONObject(0).getJSONObject("blocks")
            } else if (jsonObject.has("blocks")) {"""

replacement = """            val blocks = org.json.JSONObject()
            if (jsonObject.has("targets")) {
                val targetsArray = jsonObject.getJSONArray("targets")
                for (i in 0 until targetsArray.length()) {
                    val targetObj = targetsArray.getJSONObject(i)
                    if (targetObj.has("blocks")) {
                        val targetBlocks = targetObj.getJSONObject("blocks")
                        targetBlocks.keys().forEach { key ->
                            blocks.put(key, targetBlocks.get(key))
                        }
                    }
                }
            } else if (jsonObject.has("blocks")) {"""

if target in content:
    content = content.replace(target, replacement)
    print("Patched blocks extraction")
else:
    print("Failed to patch blocks extraction")

with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "w") as f:
    f.write(content)
