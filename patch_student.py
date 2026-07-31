with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

import re

# Patch the null activeReport text
pattern1 = r'Text\("抱歉，未找到该作品的评测报告数据（可能因网络问题同步失败或为旧数据）。请尝试重新提交作品。", color = Color.Gray, textAlign = TextAlign.Center\)'
replacement1 = """Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF3F51B5), modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("作品已成功提交！\nAI 正在马不停蹄地为您生成专属评估报告\n通常需要 10-15 秒，请稍后重新点开查看哦！", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 13.sp, lineHeight = 18.sp)
                        }"""

if re.search(pattern1, content):
    content = re.sub(pattern1, replacement1, content)
    with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Not found")

