import os

with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = """"control_forever" -> {
                sb.append(indent).append("while True:
")
                val substack = getSubstackId(inputs)
                if (substack != null) {
                    val parsed = parseBlock(substack, blocks, indentLevel + 1)
                    if (parsed.isBlank()) {
                        sb.append(indent).append("    pass
")
                    } else {
                        sb.append(parsed)
                    }
                } else {
                    sb.append(indent).append("    pass
")
                }
            }"""

replacement = """"control_forever" -> {
                sb.append(indent).append("while True:\\n")
                val substack = getSubstackId(inputs)
                if (substack != null) {
                    val parsed = parseBlock(substack, blocks, indentLevel + 1)
                    if (parsed.isBlank()) {
                        sb.append(indent).append("    pass\\n")
                    } else {
                        sb.append(parsed)
                    }
                } else {
                    sb.append(indent).append("    pass\\n")
                }
            }"""

content = content.replace(target, replacement)
with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "w", encoding="utf-8") as f:
    f.write(content)
print("Patched ScratchToPythonConverter syntax")
