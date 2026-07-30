import re

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r") as f:
    content = f.read()

target = """                val report = repository.submitWorkAndEvaluate(work)
                
                // ----- Supabase 上传逻辑 -----
                try {
                    val localFile = java.io.File(context.filesDir, "student_${studentId}_project_${report.workId}.sb3")
                    com.example.data.Sb3Generator.writeSb3File(currentDraftCode.value, localFile)
                    com.example.data.SupabaseManager.uploadScratchProject(localFile, localFile.name)
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseUpload", "Upload to Supabase failed", e)
                }
                // ---------------------------

                onResult("作品提报并评测成功！综合评分：${report.averageScore} 分，精细诊断细节已产生 ~")"""

replacement = """                val report = repository.submitWorkAndEvaluate(work)
                
                // ----- 加上这行强力调试代码 -----
                android.util.Log.d("SupabaseDebug", "开始执行 Supabase 上传逻辑，workId = ${report.workId}")
                // ------------------------------
                
                try {
                    val localFile = java.io.File(context.filesDir, "student_${studentId}_project_${report.workId}.sb3")
                    com.example.data.Sb3Generator.writeSb3File(currentDraftCode.value, localFile)
                    
                    android.util.Log.d("SupabaseDebug", "本地 .sb3 文件生成成功，路径：${localFile.absolutePath}，大小：${localFile.length()}字节")
                    
                    com.example.data.SupabaseManager.uploadScratchProject(localFile, localFile.name)
                    
                    android.util.Log.d("SupabaseDebug", "SupabaseManager.uploadScratchProject 调用完成")
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseDebug", "Upload to Supabase failed with exception", e)
                }

                onResult("作品提报并评测成功！综合评分：${report.averageScore} 分，精细诊断细节已产生 ~")"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w") as f:
        f.write(content)
    print("Patched MainViewModel.kt successfully.")
else:
    print("Target content not found. Let's find what is actually there.")
    # Try finding something else
