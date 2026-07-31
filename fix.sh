sed -i 's/pythonCode.append("/pythonCode.append("\\n"/g' app/src/main/java/com/example/data/ScratchToPythonConverter.kt
sed -i 's/pythonCode.append("# --- $displayName ---/pythonCode.append("# --- $displayName ---\\n"/g' app/src/main/java/com/example/data/ScratchToPythonConverter.kt
# let's be more precise
