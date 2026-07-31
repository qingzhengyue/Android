with open("app/src/main/java/com/example/data/AppRepository.kt", "r") as f:
    content = f.read()

target1 = """        // 同步作品到云端
        try {
            supabase?.from("scratch_work")?.upsert(finalWorkWithId)
        } catch (e: Exception) {
            Log.e("Supabase", "Work sync failed: ${e.message}")
            throw e
        }"""

replacement1 = """        // 同步作品到云端
        try {
            supabase?.from("scratch_work")?.upsert(finalWorkWithId)
        } catch (e: Exception) {
            Log.e("SupabaseSync", "同步上传失败，拦截到的底层错误是: ${e.message}", e)
            throw Exception("作品云端同步失败: ${e.message}", e)
        }"""

target2 = """        try {
            supabase?.from("work_ai_report")?.upsert(reportWithId)
        } catch (e: Exception) {
            Log.e("Supabase", "Report sync failed: ${e.message}")
        }"""

replacement2 = """        try {
            supabase?.from("work_ai_report")?.upsert(reportWithId)
        } catch (e: Exception) {
            Log.e("SupabaseSync", "评测报告同步失败，拦截到的底层错误是: ${e.message}", e)
            throw Exception("评测报告云端同步失败: ${e.message}", e)
        }"""

if target1 in content:
    content = content.replace(target1, replacement1)
else:
    print("Could not find target1")
    
if target2 in content:
    content = content.replace(target2, replacement2)
else:
    print("Could not find target2")

with open("app/src/main/java/com/example/data/AppRepository.kt", "w") as f:
    f.write(content)

