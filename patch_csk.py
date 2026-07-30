with open("app/src/main/java/com/example/data/GeminiClient.kt", "r") as f:
    content = f.read()

content = content.replace('"gpt-3.5-turbo" // 默认使用通用模型名，如果您有特定模型请在此修改', '"llama3.1-8b" // 替换为 Cerebras Cloud 支持的模型')
content = content.replace('"https://api.openai.com/v1/chat/completions" // 若使用的是中转 API，请将此处改为中转地址', '"https://api.cerebras.ai/v1/chat/completions" // 替换为真实的 Cerebras 请求地址')

with open("app/src/main/java/com/example/data/GeminiClient.kt", "w") as f:
    f.write(content)
print("Patched GeminiClient.kt for Cerebras")
