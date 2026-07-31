import re

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

# Fix Activityimport -> Activity\nimport
content = re.sub(r'([a-zA-Z0-9_*])import ', r'\1\nimport ', content)

# Sometimes there's no space? No, import must have a space after it: `import `
# Let's check `([a-zA-Z0-9_*])import `

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)
