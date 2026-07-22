package com.example

import com.example.data.GeminiClient
import com.example.data.ScratchWorkEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScratchWorkEvaluatorTest {
    @Test
    fun completeProjectScoresHigherThanInvalidData() {
        val complete = ScratchWorkEvaluator.evaluate(
            """{"blocks":{"a":{"opcode":"event_whenflagclicked"},"b":{"opcode":"control_forever"},"c":{"opcode":"motion_movesteps"},"d":{"opcode":"sound_play"}}}"""
        )
        val invalid = ScratchWorkEvaluator.evaluate("not-json")

        assertTrue(complete.averageScore > invalid.averageScore)
        assertTrue(complete.suggestions.isNotBlank())
    }

    @Test
    fun remoteScoresAreClampedAndRecalculated() {
        val sanitized = ScratchWorkEvaluator.sanitize(
            GeminiClient.EvaluationResult(90, -5, 40, 21, 999, "  可执行建议  ")
        )

        assertEquals(25, sanitized.grammarScore)
        assertEquals(0, sanitized.logicScore)
        assertEquals(25, sanitized.taskMatchScore)
        assertEquals(20, sanitized.creativeScore)
        assertEquals(70, sanitized.averageScore)
        assertEquals("可执行建议", sanitized.suggestions)
    }
}
