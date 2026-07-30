with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

content = content.replace("androidx.compose.material.icons.Icons\\n", "")
content = content.replace("androidx.compose.material.icons.filled.Code\\n", "")
content = content.replace("androidx.compose.material.icons.filled.Analytics\\n", "")

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)
print("Cleaned up StudentScreens.kt")
