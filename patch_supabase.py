with open("/app/applet/app/src/main/java/com/example/data/SupabaseManager.kt", "r") as f:
    content = f.read()

target = """    suspend fun uploadScratchProject(localFile: File, remoteFileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val bucket = client.storage.from("student-works")
                val fileBytes = localFile.readBytes()
                                
                bucket.upload(remoteFileName, fileBytes) {
                    upsert = true
                }
                                
                println("上传成功：$remoteFileName")
                true
            } catch (e: Exception) {
                println("上传失败：${e.message}")
                e.printStackTrace()
                false
            }
        }
    }"""

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

if target in content:
    content = content.replace(target, replacement)
    with open("/app/applet/app/src/main/java/com/example/data/SupabaseManager.kt", "w") as f:
        f.write(content)
    print("Patched SupabaseManager.kt successfully.")
else:
    print("Target content not found. Let's find what is actually there.")
