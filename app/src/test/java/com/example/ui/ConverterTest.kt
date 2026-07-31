package com.example.ui

import org.json.JSONObject
import org.junit.Test
import java.io.File
import com.example.data.ScratchToPythonConverter

class ConverterTest {
    @Test
    fun testConverter() {
        val json = """{"targets":[{"isStage":false,"name":"猫咪漫步 (Sprite1)","blocks":{"b1":{"opcode":"event_whenflagclicked","next":"b2"},"b2":{"opcode":"control_forever","inputs":{"SUBSTACK":["b3"]}},"b3":{"opcode":"motion_movesteps","inputs":{"STEPS":[4,"10"]},"next":"b4"},"b4":{"opcode":"motion_ifonedgebounce","next":"b5"},"b5":{"opcode":"motion_setrotationstyle","fields":{"STYLE":["左右翻转"]}}}}]}"""
        val py = ScratchToPythonConverter.convertJsonToPython(json)
        File("test_output.txt").writeText(py)
    }
}
