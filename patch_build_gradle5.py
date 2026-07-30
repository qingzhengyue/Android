with open("app/build.gradle.kts", "r") as f:
    content = f.read()

content = content.replace("import java.util.Propertiesval localProperties = Properties()", "import java.util.Properties\\n\\nval localProperties = Properties()")

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
