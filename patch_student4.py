with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

content = content.replace("androidx.compose.material.icons.filled.PlayArrow", "androidx.compose.material.icons.Icons.Default.PlayArrow")
content = content.replace("androidx.compose.material.icons.filled.Info", "androidx.compose.material.icons.Icons.Default.Info")

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)
print("Fixed icons 4 in StudentScreens.kt")
