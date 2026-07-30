with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r") as f:
    content = f.read()

target = """                try {
                    val localFile = java.io.File(context.filesDir, "student_${studentId}_project_${report.workId}.sb3")
                    com.example.data.Sb3Generator.writeSb3File(currentDraftCode.value, localFile)
                    
                    android.util.Log.d("SupabaseDebug", "本地 .sb3 文件生成成功，路径：${localFile.absolutePath}，大小：${localFile.length()}字节")"""

replacement = """                try {
                    val taskIdForMock = currentTaskId.value ?: 0
                    val matchedTaskForMock = tasksList.value.find { it.taskId == taskIdForMock }
                    val taskNameForMock = matchedTaskForMock?.taskName ?: ""
                    
                    val mockFile = com.example.data.MockWorkRepository.getMockSb3FileForTask(context, taskIdForMock.toLong(), taskNameForMock)
                    val localFile = java.io.File(context.filesDir, "student_${studentId}_project_${report.workId}.sb3")
                    
                    // Copy mock file to localFile for upload
                    mockFile.copyTo(localFile, overwrite = true)
                    
                    android.util.Log.d("SupabaseDebug", "本地 .sb3 文件生成成功，路径：${localFile.absolutePath}，大小：${localFile.length()}字节")"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w") as f:
        f.write(content)
    print("Patched MainViewModel.kt with MockWorkRepository")
else:
    print("Target block not found in MainViewModel.kt")
