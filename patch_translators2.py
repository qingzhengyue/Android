import os

def patch_file(path, target, replacement):
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    if target in content:
        content = content.replace(target, replacement)
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"Patched {path}")
    else:
        print(f"Target not found in {path}")

patch_file("app/src/main/java/com/example/data/ScratchToPythonConverter.kt",
           """            "motion_setrotationstyle" -> {
                val style = getInputValue(inputs, "STYLE", "left-right")
                sb.append(indent).append("sprite.set_rotation_style('$style')\\n")
            }""",
           """            "motion_setrotationstyle" -> {
                var style = "left-right"
                val fields = block.optJSONObject("fields")
                if (fields != null && fields.has("STYLE")) {
                    val f = fields.optJSONArray("STYLE")
                    if (f != null && f.length() > 0) style = f.optString(0)
                }
                sb.append(indent).append("sprite.set_rotation_style('$style')\\n")
            }""")
