import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = """                    com.example.data.SupabaseManager.insertWorkAiReportRecord(reportToInsert)"""

replacement = """                    try {
                        if (!com.example.BuildConfig.SUPABASE_URL.contains("169.254")) {
                            com.example.data.SupabaseManager.insertWorkAiReportRecord(reportToInsert)
                        }
                    } catch(e: Exception) {
                        android.util.Log.e("SupabaseDebug", "Insert report failed: ${e.message}")
                    }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Patched insertWorkAiReportRecord in MainViewModel")
else:
    print("Target not found")
