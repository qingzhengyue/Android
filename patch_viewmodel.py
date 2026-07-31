import os

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r", encoding="utf-8") as f:
    content = f.read()

target1 = "fun batchImportStudentsByTeacher(namesStr: String, classId: Int, onResult: (String) -> Unit) {"
replacement1 = """fun batchImportStudentsByTeacher(namesStr: String, classEntity: com.example.data.ClassEntity, onResult: (String) -> Unit) {
        val classId = classEntity.classId"""

content = content.replace(target1, replacement1)

target2 = """            val prefix = "S${classId}"
            val randSuffix = (1000..9999).random()"""
replacement2 = """            val gradeMatch = Regex("([一二三四五六七八九十0-9]+)年级").find(classEntity.grade) ?: Regex("([高初][一二三])").find(classEntity.grade)
            val classMatch = Regex("([一二三四五六七八九十0-9]+)[班(]").find(classEntity.className)
            val numMap = mapOf("一" to "1", "二" to "2", "三" to "3", "四" to "4", "五" to "5", "六" to "6", "七" to "7", "八" to "8", "九" to "9", "十" to "10", "初一" to "7", "初二" to "8", "初三" to "9", "高一" to "10", "高二" to "11", "高三" to "12")
            var gStr = classId.toString()
            if (gradeMatch != null) {
                val g = gradeMatch.groupValues[1]
                gStr = numMap[g] ?: g
            }
            var cStr = ""
            if (classMatch != null) {
                val c = classMatch.groupValues[1]
                cStr = numMap[c] ?: c
            } else {
                cStr = "1"
            }
            val prefix = "S${gStr}${cStr}"
            val randSuffix = 1"""
content = content.replace(target2, replacement2)

target3 = 'val num = "$prefix${randSuffix + index}"'
replacement3 = 'val currentSuffix = (randSuffix + index).toString().padStart(2, \'0\')\n                val num = "$prefix${currentSuffix}"'
content = content.replace(target3, replacement3)

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w", encoding="utf-8") as f:
    f.write(content)
print("Done")
