import os
import re

with open("app/src/main/java/com/example/data/Database.kt", "r", encoding="utf-8") as f:
    content = f.read()

dto_class = """
@Serializable
data class ScratchWorkInsertDto(
    @SerialName("work_name")
    val workName: String,
    @SerialName("work_code")
    val workCode: String,
    @SerialName("student_id")
    val studentId: Int,
    @SerialName("class_id")
    val classId: Int,
    @SerialName("task_id")
    val taskId: Int,
    @SerialName("submit_count")
    val submitCount: Int,
    @SerialName("review_status")
    val reviewStatus: String
)
"""

if "ScratchWorkInsertDto" not in content:
    content += dto_class
    with open("app/src/main/java/com/example/data/Database.kt", "w", encoding="utf-8") as f:
        f.write(content)
    print("Added ScratchWorkInsertDto")
else:
    print("ScratchWorkInsertDto already exists")
