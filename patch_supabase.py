import os

with open("app/src/main/java/com/example/data/SupabaseManager.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = """    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Storage)
    }"""
replacement = """    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(io.github.jan.supabase.postgrest.Postgrest)
        install(Storage)
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/data/SupabaseManager.kt", "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched SupabaseManager")
else:
    print("Target not found")
