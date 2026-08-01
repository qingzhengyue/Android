import os

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = """                    val workToInsert = work.copy(workId = report.workId)
                    com.example.data.SupabaseManager.insertScratchWorkRecord(workToInsert)"""

replacement = """                    val workToInsert = com.example.data.ScratchWorkInsertDto(
                        workName = work.workName,
                        workCode = work.workCode,
                        studentId = work.studentId,
                        classId = work.classId,
                        taskId = work.taskId,
                        submitCount = work.submitCount,
                        reviewStatus = work.reviewStatus
                    )
                    com.example.data.SupabaseManager.insertScratchWorkRecord(workToInsert)"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched MainViewModel.kt")
else:
    print("Target not found in MainViewModel.kt")
