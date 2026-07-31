import os

file_path = "app/src/main/java/com/example/data/SupabaseManager.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

target = """            bucket.upload(remoteFileName, fileBytes) {
                upsert = true
            }"""

replacement = """            // Avoid upsert which requires UPDATE permissions
            bucket.upload(remoteFileName, fileBytes) {
                upsert = false
            }"""

if target in content:
    content = content.replace(target, replacement)
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched SupabaseManager.kt")
else:
    print("Target not found in SupabaseManager.kt")

