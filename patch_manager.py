import os

with open("app/src/main/java/com/example/data/SupabaseManager.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = "suspend fun insertScratchWorkRecord(work: ScratchWork): Boolean {"
replacement = "suspend fun insertScratchWorkRecord(work: ScratchWorkInsertDto): Boolean {"

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/data/SupabaseManager.kt", "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched SupabaseManager.kt")
else:
    print("Target not found in SupabaseManager.kt")
