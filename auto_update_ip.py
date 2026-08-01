import socket
import os
import re

def get_local_ip():
    # 获取本机的局域网 IP 地址
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # 尝试连接一个外部 IP，不需要真正连通
        s.connect(('10.255.255.255', 1))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip

new_ip = get_local_ip()
print(f"🚀 自动检测到本机局域网 IP: {new_ip}")

files_to_update = [
    ".env.example",
    "app/src/test/java/com/example/TestKtor.kt",
    "app/src/test/java/com/example/SupabaseUrlTest.kt",
    "app/src/test/java/com/example/SupabaseUploadTest.kt"
]

# 匹配 IPv4 的正则表达式
ip_pattern = re.compile(r'\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b')

for file_path in files_to_update:
    # 兼容 Windows 路径
    normalized_path = os.path.normpath(file_path)
    if os.path.exists(normalized_path):
        with open(normalized_path, "r", encoding="utf-8") as f:
            content = f.read()
        
        # 将文件中所有的 IP 地址替换为新的 IP
        updated_content = ip_pattern.sub(new_ip, content)
        
        with open(normalized_path, "w", encoding="utf-8") as f:
            f.write(updated_content)
        print(f"✅ 已更新: {normalized_path}")

# 更新 local.properties (覆盖环境变量配置)
local_properties_path = "local.properties"
if os.path.exists(local_properties_path):
    with open(local_properties_path, "r", encoding="utf-8") as f:
        lines = f.readlines()
else:
    lines = []

supabase_url_found = False
for i, line in enumerate(lines):
    if line.startswith("SUPABASE_URL="):
        lines[i] = f"SUPABASE_URL=http://{new_ip}:8000\n"
        supabase_url_found = True
        break

if not supabase_url_found:
    # 如果没找到，追加到文件末尾
    lines.append(f"\nSUPABASE_URL=http://{new_ip}:8000\n")

with open(local_properties_path, "w", encoding="utf-8") as f:
    f.writelines(lines)
print(f"✅ 已更新: {local_properties_path}")

print(f"\n🎉 成功将项目的 API 地址替换为新的局域网 IP: {new_ip}")
print("💡 请在 Android Studio 中点击 'Sync Project with Gradle Files'，然后重新运行 App。")
