with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

content = content.replace("import androidx.compose.material.icons.Icons\\nimport androidx.compose.material.icons.rounded.Analytics\\nimport androidx.compose.material.icons.rounded.Extension", "import androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.rounded.Analytics\nimport androidx.compose.material.icons.rounded.Extension\n")

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)
