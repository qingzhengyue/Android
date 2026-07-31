import re

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

# Separate package
content = re.sub(r'(package com.example.ui)(import )', r'\1\n\n\2', content)

# Separate imports
content = re.sub(r'(import [^;]+?)(import )', r'\1\n\2', content)
# Run multiple times to catch overlapping matches
for _ in range(5):
    content = re.sub(r'(import [a-zA-Z0-9_.*]+)(import )', r'\1\n\2', content)

# Remove literal \n
content = content.replace("\\n", "\n")

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)
