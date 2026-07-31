import os

with open("app/src/main/java/com/example/data/SupabaseManager.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = """import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage"""
replacement = """import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.postgrest"""

content = content.replace(target, replacement)

target2 = """            println("上传成功：$remoteFileName")
            true
        }
    }
}"""
replacement2 = """            println("上传成功：$remoteFileName")
            true
        }
    }
    
    suspend fun insertScratchWorkRecord(work: ScratchWork): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Ensure Postgrest is imported and used to insert data
                client.postgrest["scratch_work"].insert(work)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}"""

content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/data/SupabaseManager.kt", "w", encoding="utf-8") as f:
    f.write(content)
print("Patched SupabaseManager")
