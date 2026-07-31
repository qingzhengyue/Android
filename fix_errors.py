with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

content = content.replace("@Composable\\n@Composable", "@Composable")
content = content.replace('Regex("(.*?)\\s*\\((.*?)\\)")', 'Regex("(.*?)\\\\s*\\\\((.*?)\\\\)")')
content = content.replace('withStyle', 'androidx.compose.ui.text.withStyle')

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)
