fun main() {
    val grade = "三年级"
    val className = "三年级二班"
    val fallbackId = 3

    val gradeMatch = Regex("([一二三四五六七八九十0-9]+)年级").find(grade) ?: Regex("([高初][一二三])").find(grade)
    val classMatch = Regex("([一二三四五六七八九十0-9]+)\\s*班").find(className) ?: Regex("(?<=[(（])[一二三四五六七八九十0-9]+(?=[)）])").find(className) ?: Regex("([一二三四五六七八九十0-9]+)").findAll(className).lastOrNull()
    val numMap = mapOf("一" to "1", "二" to "2", "三" to "3", "四" to "4", "五" to "5", "六" to "6", "七" to "7", "八" to "8", "九" to "9", "十" to "10", "初一" to "7", "初二" to "8", "初三" to "9", "高一" to "10", "高二" to "11", "高三" to "12")
    var gStr = fallbackId.toString()
    if (gradeMatch != null) {
        val g = gradeMatch.groupValues[1]
        gStr = numMap[g] ?: g
    }
    var cStr = ""
    if (classMatch != null) {
        val c = classMatch.groupValues.getOrElse(1) { classMatch.value }
        cStr = numMap[c] ?: c
    } else {
        cStr = "1"
    }
    println("${gStr}${cStr}")
}
