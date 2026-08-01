package com.example

import com.example.data.ScratchToPythonConverter

fun main() {
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
    println(ScratchToPythonConverter.convertJsonToPython(json))
}
