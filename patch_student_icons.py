with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

content = content.replace("Icons.Default.Code", "androidx.compose.material.icons.filled.Code")
content = content.replace("Icons.Default.Analytics", "androidx.compose.material.icons.filled.Analytics")

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)
print("Fixed icons in StudentScreens.kt")
