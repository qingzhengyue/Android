import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = """                    com.example.data.SupabaseManager.insertScratchWorkRecord(workToInsert)"""

replacement = """                    try {
                        if (!com.example.BuildConfig.SUPABASE_URL.contains("169.254")) {
                            com.example.data.SupabaseManager.insertScratchWorkRecord(workToInsert)
                        }
                    } catch(e: Exception) {
                        android.util.Log.e("SupabaseDebug", "Insert failed: ${e.message}")
                    }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Patched insertScratchWorkRecord in MainViewModel")
else:
    print("Target not found")
