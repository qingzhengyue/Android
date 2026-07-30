with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

content = content.replace("androidx.compose.material.icons.Icons.Rounded.Analytics", "androidx.compose.material.icons.rounded.Analytics")
content = content.replace("androidx.compose.material.icons.Icons.Rounded.Extension", "androidx.compose.material.icons.rounded.Extension")

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)
