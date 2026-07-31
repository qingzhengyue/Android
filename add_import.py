with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

if "import androidx.compose.animation.core.*" not in content:
    content = content.replace("import androidx.compose.animation.*", "import androidx.compose.animation.*\\nimport androidx.compose.animation.core.*")

if "import androidx.compose.runtime.getValue" not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\\nimport androidx.compose.runtime.getValue")

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)
