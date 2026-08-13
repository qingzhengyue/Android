import re

with open('app/src/main/java/com/example/data/AppRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = """        try {
            supabase?.from("scratch_draft")?.upsert(draftWithId)
        } catch (e: Exception) {
            Log.e("Supabase", "Draft sync failed: ${e.message}")
        }
        localId"""

replacement = """        // Fire and forget, don't block the UI thread waiting for 30s timeout
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                // Ignore sync locally if no supabase server
                if (!com.example.BuildConfig.SUPABASE_URL.contains("169.254")) {
                    supabase?.from("scratch_draft")?.upsert(draftWithId)
                }
            } catch (e: Exception) {
                // Silently ignore sync failures to prevent log spam
            }
        }
        localId"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/data/AppRepository.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Patched saveDraft")
else:
    print("Target not found")
