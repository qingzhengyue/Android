import os

with open("app/src/main/java/com/example/ui/BlockTextFormatter.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = """            "motion_turnleft" -> {
                val degrees = getInputValue(inputs, "DEGREES", "15", blocksMap)
                listOf(
                    BlockSegment.Text("左转 "),
                    BlockSegment.Parameter(degrees),
                    BlockSegment.Text(" 度")
                )
            }"""
replacement = """            "motion_turnleft" -> {
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
            }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/BlockTextFormatter.kt", "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched BlockTextFormatter")
else:
    print("Target not found")
