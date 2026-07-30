import re

with open("app/src/main/java/com/example/data/GeminiClient.kt", "r") as f:
    content = f.read()

# Remove 'val isQwen = true // 强制统一使用通义千问 API'
# Replace it with logic that detects csk- or sk-
replacement = """
        val isSparkMaaS = apiKey.startsWith("dae06") || apiKey.contains(":")
        val isCSK = apiKey.startsWith("csk-")
        val isQwen = apiKey.startsWith("sk-") && !isCSK // 默认通义千问
        val isOpenAICompatible = isQwen || isSparkMaaS || isCSK
"""

content = re.sub(r'val isSparkMaaS = apiKey\.startsWith\("dae06"\) \|\| apiKey\.contains\(":"\)\s*val isQwen = true // 强制统一使用通义千问 API', replacement.strip(), content)

# Replace 'if (isQwen || isSparkMaaS)' with 'if (isOpenAICompatible)'
content = content.replace("if (isQwen || isSparkMaaS)", "if (isOpenAICompatible)")

# Find the modelName logic
target_model = """val modelName = if (isSparkMaaS) "xopqwen36v35b" else "qwen-plus\""""
replacement_model = """val modelName = when {
                        isSparkMaaS -> "xopqwen36v35b"
                        isCSK -> "gpt-3.5-turbo" // 默认使用通用模型名，如果您有特定模型请在此修改
                        else -> "qwen-plus"
                    }"""
content = content.replace(target_model, replacement_model)

# Find the targetUrl logic
target_url = """val targetUrl = if (isSparkMaaS) {
                        "https://maas-api.cn-huabei-1.xf-yun.com/v2/chat/completions"
                    } else {
                        "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
                    }"""
replacement_url = """val targetUrl = when {
                        isSparkMaaS -> "https://maas-api.cn-huabei-1.xf-yun.com/v2/chat/completions"
                        isCSK -> "https://api.openai.com/v1/chat/completions" // 若使用的是中转 API，请将此处改为中转地址
                        else -> "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
                    }"""
content = content.replace(target_url, replacement_url)

with open("app/src/main/java/com/example/data/GeminiClient.kt", "w") as f:
    f.write(content)
print("Patched GeminiClient.kt successfully.")
