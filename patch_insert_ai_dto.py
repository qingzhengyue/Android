import os

with open("app/src/main/java/com/example/data/Database.kt", "r", encoding="utf-8") as f:
    content = f.read()

dto_class = """
@Serializable
data class WorkAiReportInsertDto(
    @SerialName("work_id") val workId: Int,
    @SerialName("student_id") val studentId: Int,
    @SerialName("grammar_score") val grammarScore: Int,
    @SerialName("logic_score") val logicScore: Int,
    @SerialName("task_match_score") val taskMatchScore: Int,
    @SerialName("creative_score") val creativeScore: Int,
    @SerialName("average_score") val averageScore: Int,
    @SerialName("optimization_suggestions") val optimizationSuggestions: String
)
"""

if "WorkAiReportInsertDto" not in content:
    content += dto_class
    with open("app/src/main/java/com/example/data/Database.kt", "w", encoding="utf-8") as f:
        f.write(content)
    print("Added WorkAiReportInsertDto")
else:
    print("WorkAiReportInsertDto already exists")
