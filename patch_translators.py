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

patch_file("app/src/main/java/com/example/ui/BlockTranslator.kt",
           '"motion_ifonedgebounce" to "碰到边缘就反弹",',
           '"motion_ifonedgebounce" to "碰到边缘就反弹",\n        "motion_setrotationstyle" to "将旋转方式设为",')

patch_file("app/src/main/java/com/example/ui/ScratchBlockTranslator.kt",
           '"motion_ifonedgebounce" to "碰到边缘就反弹",',
           '"motion_ifonedgebounce" to "碰到边缘就反弹",\n        "motion_setrotationstyle" to "将旋转方式设为",')

patch_file("app/src/main/java/com/example/data/ScratchToPythonConverter.kt",
           '"motion_ifonedgebounce" -> {',
           '"motion_setrotationstyle" -> {\n                val style = getInputValue(inputs, "STYLE", "left-right")\n                sb.append(indent).append("sprite.set_rotation_style(\'$style\')\\n")\n            }\n            "motion_ifonedgebounce" -> {')
