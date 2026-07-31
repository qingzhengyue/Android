with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

import re
# Insert a newline before every 'import ' that is immediately preceded by a letter or digit or star
content = re.sub(r'([a-zA-Z0-9_*])import ', r'\1\nimport ', content)

# ensure package is on its own line
content = re.sub(r'(package [a-zA-Z0-9_.]+)(import)', r'\1\n\n\2', content)

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)
