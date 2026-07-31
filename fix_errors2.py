with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

content = content.replace("@Composable\n@Composable", "@Composable")
content = content.replace('androidx.compose.ui.text.withStyle', 'withStyle')

if "import androidx.compose.ui.text.withStyle" not in content:
    content = content.replace("package com.example.ui", "package com.example.ui\nimport androidx.compose.ui.text.withStyle\n")

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)
