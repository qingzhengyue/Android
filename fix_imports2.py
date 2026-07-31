with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

# Fix the merged imports
content = content.replace("import ", "\nimport ")
content = content.replace("package com.example.ui\n", "package com.example.ui")
content = content.replace("package com.example.ui", "package com.example.ui\n\n")
# Clean up multiple newlines
import re
content = re.sub(r'\n{3,}', '\n\n', content)

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)
