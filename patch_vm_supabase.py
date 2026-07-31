import os

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = """                    com.example.data.SupabaseManager.uploadScratchProject(localFile, localFile.name)
                    
                    android.util.Log.d("SupabaseDebug", "SupabaseManager.uploadScratchProject 调用完成")"""
replacement = """                    com.example.data.SupabaseManager.uploadScratchProject(localFile, localFile.name)
                    
                    val workToInsert = work.copy(workId = report.workId)
                    com.example.data.SupabaseManager.insertScratchWorkRecord(workToInsert)
                    
                    android.util.Log.d("SupabaseDebug", "SupabaseManager.uploadScratchProject and insertScratchWorkRecord 调用完成")"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched MainViewModel with Supabase data insert")
else:
    print("Target not found")
