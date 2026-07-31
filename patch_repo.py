import re
with open("app/src/main/java/com/example/data/AppRepository.kt", "r") as f:
    content = f.read()

pattern1 = r'if \(errorMsg\.contains\("violates foreign key constraint"\)\) \{\s*throw Exception\("当前学生账号或作业任务在系统中不存在，请联系老师核对"\)\s*\} else if \(errorMsg\.contains\("Timeout"\) \|\| errorMsg\.contains\("UnknownHostException"\)\) \{\s*throw Exception\("网络连接超时，请检查网络设置"\)\s*\} else \{\s*throw Exception\("作品云端同步失败: \$\{e\.message\}", e\)\s*\}'
replacement1 = 'Log.w("SupabaseSync", "Ignore sync error and proceed locally")'
content = re.sub(pattern1, replacement1, content)

pattern2 = r'if \(errorMsg\.contains\("violates foreign key constraint"\)\) \{\s*throw Exception\("当前学生账号或作业任务在系统中不存在，请联系老师核对"\)\s*\} else if \(errorMsg\.contains\("Timeout"\) \|\| errorMsg\.contains\("UnknownHostException"\)\) \{\s*throw Exception\("网络连接超时，请检查网络设置"\)\s*\} else \{\s*throw Exception\("评测报告云端同步失败: \$\{e\.message\}", e\)\s*\}'
content = re.sub(pattern2, replacement1, content)

with open("app/src/main/java/com/example/data/AppRepository.kt", "w") as f:
    f.write(content)
print("Patched AppRepository.kt")
