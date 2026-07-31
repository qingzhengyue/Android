with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

import re
content = re.sub(r'Text\("作品已成功提交！\nAI 正在马不停蹄地为您生成专属评估报告\n通常需要 10-15 秒，请稍后重新点开查看哦！"', r'Text("作品已成功提交！\\nAI 正在马不停蹄地为您生成专属评估报告\\n通常需要 10-15 秒，请稍后重新点开查看哦！"', content)

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "r") as f:
    content2 = f.read()

content2 = re.sub(r'Text\(\n\s*text = "🤖 正在生成 AI 评测报告中...\n\(通常需要 10-15 秒\)"', r'Text(\n                                            text = "🤖 正在生成 AI 评测报告中...\\n(通常需要 10-15 秒)"', content2)

with open("app/src/main/java/com/example/ui/TeacherWorksScreens.kt", "w") as f:
    f.write(content2)

print("Fixed")
