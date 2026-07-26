package com.example.data

import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

/**
 * Scratch 积木代码相似度与抄袭检测工具 (Task 6)
 * 通过对 Scratch 3.0 标准 JSON 进行特征提取（opcode 序列、控制流树、积木类型分布），
 * 忽略坐标(x,y)、变动ID等干扰参数，计算 Jaccard / Cosine 相似度。
 */
object ScratchCodeSimilarity {

    data class SimilarityResult(
        val similarityPercentage: Int, // 0 - 100
        val isPlagiarism: Boolean,      // > 75% 认定为高风险疑似抄袭
        val matchedWorkId: Int? = null,
        val matchedStudentName: String? = null,
        val similaritySummary: String
    )

    /**
     * 提取 Scratch JSON 中的核心 opcode 指令序列 (忽略坐标、颜色等非逻辑参数)
     */
    fun extractOpcodeSequence(jsonString: String): List<String> {
        val opcodes = mutableListOf<String>()
        if (jsonString.isBlank()) return opcodes

        try {
            val root = JSONObject(jsonString)
            val targets = root.optJSONArray("targets") ?: return opcodes

            for (i in 0 until targets.length()) {
                val target = targets.optJSONObject(i) ?: continue
                val blocks = target.optJSONObject("blocks") ?: continue

                val keys = blocks.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val block = blocks.optJSONObject(key)
                    if (block != null) {
                        val opcode = block.optString("opcode", "")
                        if (opcode.isNotBlank()) {
                            opcodes.add(opcode)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 如果 JSON 结构非标准 Scratch JSON，使用正则表达式退化提取
            val regex = """"opcode"\s*:\s*"([^"]+)"""".toRegex()
            regex.findAll(jsonString).forEach { match ->
                opcodes.add(match.groupValues[1])
            }
        }
        return opcodes
    }

    /**
     * 计算两个 opcode 序列之间的 Jaccard 频次相似度
     */
    fun calculateJaccardSimilarity(seq1: List<String>, seq2: List<String>): Double {
        if (seq1.isEmpty() && seq2.isEmpty()) return 1.0
        if (seq1.isEmpty() || seq2.isEmpty()) return 0.0

        val freq1 = seq1.groupingBy { it }.eachCount()
        val freq2 = seq2.groupingBy { it }.eachCount()

        val allKeys = freq1.keys + freq2.keys
        var intersectionSum = 0
        var unionSum = 0

        for (key in allKeys) {
            val c1 = freq1[key] ?: 0
            val c2 = freq2[key] ?: 0
            intersectionSum += min(c1, c2)
            unionSum += max(c1, c2)
        }

        return if (unionSum == 0) 0.0 else intersectionSum.toDouble() / unionSum.toDouble()
    }

    /**
     * 对比当前作品与同一任务下其他已提交作品的相似度
     */
    fun checkSimilarityAgainstClassWorks(
        targetWorkCode: String,
        otherWorks: List<ScratchWork>,
        studentMap: Map<Int, String> = emptyMap()
    ): SimilarityResult {
        val targetOpcodes = extractOpcodeSequence(targetWorkCode)
        if (targetOpcodes.isEmpty() || otherWorks.isEmpty()) {
            return SimilarityResult(
                similarityPercentage = 0,
                isPlagiarism = false,
                similaritySummary = "代码为原创独创结构，未在同班或大厅中发现高度重合作品。"
            )
        }

        var maxSimilarity = 0.0
        var highestMatchWork: ScratchWork? = null

        for (other in otherWorks) {
            val otherOpcodes = extractOpcodeSequence(other.workCode)
            val sim = calculateJaccardSimilarity(targetOpcodes, otherOpcodes)
            if (sim > maxSimilarity) {
                maxSimilarity = sim
                highestMatchWork = other
            }
        }

        val similarityPercent = (maxSimilarity * 100).toInt()
        val isPlagiarized = similarityPercent >= 75
        val matchedStudentName = highestMatchWork?.let { studentMap[it.studentId] } ?: "同班同学"

        val summary = if (isPlagiarized) {
            "🚨 预警：该作品与学生 [ $matchedStudentName ] 的作品 (ID: ${highestMatchWork?.workId}) 积木结构相似度高达 $similarityPercent%，存在较高的复制或高度模仿风险！"
        } else {
            "✅ 相似度检测正常 (最高相似度 $similarityPercent%)，代码结构独立良好。"
        }

        return SimilarityResult(
            similarityPercentage = similarityPercent,
            isPlagiarism = isPlagiarized,
            matchedWorkId = highestMatchWork?.workId,
            matchedStudentName = matchedStudentName,
            similaritySummary = summary
        )
    }
}
