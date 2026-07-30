with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

content = content.replace("androidx.compose.material.icons.rounded.Analytics", "androidx.compose.material.icons.Icons.Rounded.Analytics")
content = content.replace("androidx.compose.material.icons.rounded.Extension", "androidx.compose.material.icons.Icons.Rounded.Extension")

# Add imports
if "import androidx.compose.material.icons.rounded.Analytics" not in content:
    content = content.replace("import androidx.compose.material.icons.Icons", "import androidx.compose.material.icons.Icons\\nimport androidx.compose.material.icons.rounded.Analytics\\nimport androidx.compose.material.icons.rounded.Extension")

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)
