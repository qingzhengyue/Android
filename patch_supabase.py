import os

with open("app/src/main/java/com/example/data/SupabaseManager.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = """    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {"""

# If URL is 10.42.101.36:8000, we prepend http://
replacement = """    val client = createSupabaseClient(
        supabaseUrl = if (BuildConfig.SUPABASE_URL.startsWith("http")) BuildConfig.SUPABASE_URL else "http://${BuildConfig.SUPABASE_URL}",
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/data/SupabaseManager.kt", "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched Supabase URL prefix")
else:
    print("Target not found")
