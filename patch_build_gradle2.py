with open("app/build.gradle.kts", "r") as f:
    content = f.read()

content = content.replace("val localProperties = java.util.Properties()", "import java.util.Properties\\nval localProperties = Properties()")

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
