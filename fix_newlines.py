with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "r") as f:
    content = f.read()

# Fix the specific broken lines
broken_append_n = 'pythonCode.append("\n")'
broken_append_title = 'pythonCode.append("# --- $displayName ---\n")'

content = content.replace(broken_append_n, 'pythonCode.append("\\n")')
content = content.replace(broken_append_title, 'pythonCode.append("# --- $displayName ---\\n")')

with open("app/src/main/java/com/example/data/ScratchToPythonConverter.kt", "w") as f:
    f.write(content)
