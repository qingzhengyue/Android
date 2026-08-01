import sys
import os
import re

if len(sys.argv) < 2:
    print("Usage: python update_ip.py <new_ip>")
    sys.exit(1)

new_ip = sys.argv[1]

# Define files to search and replace IP with regex
files_to_update = [
    ".env.example",
    "app/src/test/java/com/example/TestKtor.kt",
    "app/src/test/java/com/example/SupabaseUrlTest.kt",
    "app/src/test/java/com/example/SupabaseUploadTest.kt"
]

ip_pattern = re.compile(r'\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b')

for file_path in files_to_update:
    if os.path.exists(file_path):
        with open(file_path, "r") as f:
            content = f.read()
        
        updated_content = ip_pattern.sub(new_ip, content)
        
        with open(file_path, "w") as f:
            f.write(updated_content)
        print(f"Updated {file_path}")

# Update local.properties to override env var
local_properties_path = "local.properties"
if os.path.exists(local_properties_path):
    with open(local_properties_path, "r") as f:
        lines = f.readlines()
else:
    lines = []

supabase_url_found = False
for i, line in enumerate(lines):
    if line.startswith("SUPABASE_URL="):
        lines[i] = f"SUPABASE_URL=http://{new_ip}:8000\n"
        supabase_url_found = True
        break

if not supabase_url_found:
    lines.append(f"\nSUPABASE_URL=http://{new_ip}:8000\n")

with open(local_properties_path, "w") as f:
    f.writelines(lines)
print(f"Updated {local_properties_path}")

print(f"\nSuccessfully updated IP address to {new_ip}.")
print("Remember to recompile the app for changes to take effect.")
