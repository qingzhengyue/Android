import re

with open("/app/applet/app/src/main/java/com/example/data/SupabaseManager.kt", "r") as f:
    content = f.read()

replacement = """    suspend fun uploadScratchProject(localFile: File, remoteFileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val bucket = client.storage.from("student-works")
            val fileBytes = localFile.readBytes()
            
            bucket.upload(remoteFileName, fileBytes) {
                upsert = true
            }
            
            println("上传成功：$remoteFileName")
            true
        }
    }"""

# Find the start of the method
start_idx = content.find("suspend fun uploadScratchProject")
# Find the end of the method (two closing braces matching the two opening braces)
if start_idx != -1:
    end_idx = content.find("}", content.find("}", content.find("}", content.find("}", start_idx) + 1) + 1) + 1) + 1
    content = content[:start_idx] + replacement + content[end_idx:]
    with open("/app/applet/app/src/main/java/com/example/data/SupabaseManager.kt", "w") as f:
        f.write(content)
    print("Patched SupabaseManager.kt successfully.")
else:
    print("Method not found")
