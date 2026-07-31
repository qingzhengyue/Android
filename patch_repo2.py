import re
with open("app/src/main/java/com/example/data/AppRepository.kt", "r") as f:
    content = f.read()

pattern = r'supabaseUrl\s*=\s*"[^"]+",\s*supabaseKey\s*=\s*"[^"]+"'
replacement = 'supabaseUrl = com.example.BuildConfig.SUPABASE_URL,\n                supabaseKey = com.example.BuildConfig.SUPABASE_ANON_KEY'
content = re.sub(pattern, replacement, content)

with open("app/src/main/java/com/example/data/AppRepository.kt", "w") as f:
    f.write(content)
print("Patched AppRepository.kt to use BuildConfig")
