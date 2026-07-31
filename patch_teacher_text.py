with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "r") as f:
    content = f.read()

import re
pattern = r'Text\(\s*text = "🤖 正在检索该作品的 AI 评测报告\.\.\.",\s*fontSize = 13\.sp,\s*color = Color\.Gray,\s*fontWeight = FontWeight\.Medium\s*\)'
replacement = """Text(
                                            text = "🤖 正在生成 AI 评测报告中...\\n(通常需要 10-15 秒)",
                                            fontSize = 13.sp,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )"""
if re.search(pattern, content):
    content = re.sub(pattern, replacement, content)
    with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "w") as f:
        f.write(content)
    print("Patched teacher text")
else:
    print("Teacher text not found")
