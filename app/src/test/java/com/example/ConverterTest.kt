package com.example

import com.example.data.ScratchToPythonConverter
import org.junit.Test
import org.junit.Assert.assertTrue

class ConverterTest {
    @Test
    fun testMotionGoto() {
        val json = """{
            "targets": [
                {
                    "name": "Sprite1",
                    "isStage": false,
                    "blocks": {
                        "block1": {
                            "opcode": "motion_goto",
                            "topLevel": true,
                            "inputs": {
                                "TO": [1, "menu1"]
                            }
                        },
                        "menu1": {
                            "opcode": "motion_goto_menu",
                            "fields": {
                                "TO": ["_mouse_"]
                            }
                        }
                    }
                }
            ]
        }"""
        val result = ScratchToPythonConverter.convertJsonToPython(json)
        println("RESULT:")
        println(result)
        assertTrue(result.contains("sprite.go_to"))
    }
}
