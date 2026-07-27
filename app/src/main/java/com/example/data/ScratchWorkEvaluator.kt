package com.example.data

import org.json.JSONArray
import org.json.JSONObject

/** Deterministic evaluator used when the remote model is unavailable or malformed. */
object ScratchWorkEvaluator {
    fun evaluate(codeJson: String): GeminiClient.EvaluationResult {
        val opcodes = mutableListOf<String>()
        val validJson = runCatching {
            collectOpcodes(JSONObject(codeJson), opcodes)
            true
        }.getOrDefault(false)

        if (!validJson) {
            return GeminiClient.EvaluationResult(
                grammarScore = 5,
                logicScore = 5,
                taskMatchScore = 5,
                creativeScore = 5,
                averageScore = 20,
                suggestions = "作品数据暂时无法解析。请先在 Scratch 中确认项目可以正常打开，再重新提交。"
            )
        }

        val hasStart = opcodes.any { it.startsWith("event_") }
        val hasControl = opcodes.any { it.startsWith("control_") }
        val hasAction = opcodes.any { it.startsWith("motion_") || it.startsWith("looks_") || it.startsWith("sound_") }
        val categoryCount = opcodes.mapNotNull { it.substringBefore('_').takeIf(String::isNotBlank) }.distinct().size
        val uniqueOpcodeCount = opcodes.distinct().size

        val grammar = (10 + (if (hasStart) 8 else 0) + if (opcodes.size >= 3) 7 else opcodes.size * 2).coerceIn(0, 25)
        val logic = (8 + (if (hasControl) 10 else 0) + (if (hasStart && hasAction) 8 else 0) + uniqueOpcodeCount / 3).coerceIn(0, 30)
        val taskMatch = (8 + (if (hasAction) 10 else 0) + opcodes.size / 2).coerceIn(0, 25)
        val creative = (5 + categoryCount * 2 + uniqueOpcodeCount / 2).coerceIn(0, 20)
        val total = grammar + logic + taskMatch + creative

        val suggestion = when {
            !hasStart -> "已经识别到作品内容。请补充“当绿旗被点击”等事件积木，让程序拥有明确的启动入口。"
            !hasControl -> "作品可以启动。建议加入重复执行、条件判断或等待积木，让交互逻辑更完整。"
            !hasAction -> "控制结构已经具备。请加入运动、外观或声音积木，让运行效果更直观。"
            categoryCount < 4 -> "核心逻辑已经成立。可以再组合侦测、变量或声音积木，增强互动性和原创表达。"
            else -> "作品结构完整且积木类型丰富。下一步可增加异常场景测试，并说明关键参数的设计依据。"
        }

        return GeminiClient.EvaluationResult(grammar, logic, taskMatch, creative, total, suggestion)
    }

    fun sanitize(result: GeminiClient.EvaluationResult): GeminiClient.EvaluationResult {
        val grammar = result.grammarScore.coerceIn(0, 25)
        val logic = result.logicScore.coerceIn(0, 30)
        val match = result.taskMatchScore.coerceIn(0, 25)
        val creative = result.creativeScore.coerceIn(0, 20)
        return result.copy(
            grammarScore = grammar,
            logicScore = logic,
            taskMatchScore = match,
            creativeScore = creative,
            averageScore = grammar + logic + match + creative,
            suggestions = result.suggestions.trim().ifBlank { "评测完成，请结合任务要求继续完善作品。" }
        )
    }

    private fun collectOpcodes(value: Any?, output: MutableList<String>) {
        when (value) {
            is JSONObject -> value.keys().forEach { key ->
                val child = value.opt(key)
                if (key == "opcode" && child is String) output += child
                collectOpcodes(child, output)
            }
            is JSONArray -> for (index in 0 until value.length()) collectOpcodes(value.opt(index), output)
        }
    }
}
