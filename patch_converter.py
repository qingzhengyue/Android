import os

with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = """    private fun getSubstackId(inputs: JSONObject?): String? {
        if (inputs == null) return null
        val substackArray = inputs.optJSONArray("SUBSTACK") ?: return null
        val valObj = substackArray.opt(1)
        return if (valObj is String) valObj else null
    }"""
replacement = """    private fun getSubstackId(inputs: JSONObject?): String? {
        if (inputs == null) return null
        val substackArray = inputs.optJSONArray("SUBSTACK") ?: return null
        // Support both ["b3"] (from draft) and [2, "c"] (from valid JSON)
        val valObj = if (substackArray.length() == 1) substackArray.opt(0) else substackArray.opt(1)
        return if (valObj is String) valObj else null
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched getSubstackId")
else:
    print("Target not found")
