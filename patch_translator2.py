import re

with open("app/src/main/java/com/example/ui/ScratchBlockTranslator.kt", "r", encoding="utf-8") as f:
    content = f.read()

replacement = """        "motion_goto" to "移到 随机位置",
        "motion_goto_menu" to "随机位置",
        "motion_glideto" to "在 1 秒内滑行到 随机位置",
        "motion_glideto_menu" to "随机位置","""

content = content.replace("""        "motion_goto" to "移到 随机位置",
        "motion_goto_menu" to "随机位置",
        "motion_glideto" to "在 1 秒内滑行到 随机位置",""", replacement)

with open("app/src/main/java/com/example/ui/ScratchBlockTranslator.kt", "w", encoding="utf-8") as f:
    f.write(content)
