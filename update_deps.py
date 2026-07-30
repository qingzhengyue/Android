import re

# Update libs.versions.toml
with open('gradle/libs.versions.toml', 'r') as f:
    content = f.read()

if 'supabase-storage-kt' not in content:
    content = content.replace(
        'supabase-realtime-kt = { group = "io.github.jan-tennert.supabase", name = "realtime-kt", version.ref = "supabase" }',
        'supabase-realtime-kt = { group = "io.github.jan-tennert.supabase", name = "realtime-kt", version.ref = "supabase" }\nsupabase-storage-kt = { group = "io.github.jan-tennert.supabase", name = "storage-kt", version.ref = "supabase" }'
    )
    with open('gradle/libs.versions.toml', 'w') as f:
        f.write(content)

# Update build.gradle.kts
with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

if 'implementation(libs.supabase.storage.kt)' not in content:
    content = content.replace(
        'implementation(libs.supabase.realtime.kt)',
        'implementation(libs.supabase.realtime.kt)\n  implementation(libs.supabase.storage.kt)'
    )
    with open('app/build.gradle.kts', 'w') as f:
        f.write(content)
