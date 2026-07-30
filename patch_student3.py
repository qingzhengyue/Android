with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

content = content.replace("androidx.compose.material.icons.filled.Code", "androidx.compose.material.icons.filled.PlayArrow")
content = content.replace("androidx.compose.material.icons.filled.Analytics", "androidx.compose.material.icons.filled.Info")

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)
print("Fixed icons in StudentScreens.kt")
