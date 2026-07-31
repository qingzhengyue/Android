import os
import re

with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "r", encoding="utf-8") as f:
    content = f.read()

pattern = r'"control_forever" -> \{\s*sb\.append\(indent\)\.append\("while True:\\n"\)\s*val substack = getSubstackId\(inputs\)\s*if \(substack != null\) \{\s*sb\.append\(parseBlock\(substack, blocks, indentLevel \+ 1\)\)\s*\} else \{\s*sb\.append\(indent\)\.append\("    pass\\n"\)\s*\}\s*\}'

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

if re.search(pattern, content):
    content = re.sub(pattern, replacement, content)
    with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched control_forever")
else:
    print("Target not found")
