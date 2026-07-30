import sys

with open("app/build.gradle.kts", "r") as f:
    lines = f.readlines()

new_lines = []
new_lines.append("import java.util.Properties\n")
new_lines.append("\n")
new_lines.append("val localProperties = Properties()\n")
new_lines.append("val localPropertiesFile = rootProject.file(\"local.properties\")\n")
new_lines.append("if (localPropertiesFile.exists()) {\n")
new_lines.append("    localPropertiesFile.inputStream().use { localProperties.load(it) }\n")
new_lines.append("}\n")
new_lines.append("\n")

found_plugins = False
for line in lines:
    if line.startswith("plugins {"):
        found_plugins = True
    if found_plugins:
        new_lines.append(line)

with open("app/build.gradle.kts", "w") as f:
    f.writelines(new_lines)

