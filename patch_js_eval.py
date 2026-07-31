import re

with open("app/src/main/java/com/example/ui/ScratchEditorScreen.kt", "r") as f:
    content = f.read()

# Fix 1: Auto-save evaluation
target1 = """                "(function() { " +
                "  try { " +
                "    var vm = window.vm || (window.scratch && window.scratch.vm); " +"""
replacement1 = """                "(function() { " +
                "  try { " +
                "    if (typeof window.getProjectJson === 'function') { return window.getProjectJson(); } " +
                "    var vm = window.vm || (window.scratch && window.scratch.vm); " +"""

if target1 in content:
    content = content.replace(target1, replacement1)
    print("Fixed target 1")
else:
    print("Failed to fix target 1")

# Fix 2: Submit evaluation
target2 = """                                "(function() { " +
                                "  try { " +
                                "    if (window.vm) { return JSON.stringify(window.vm.toJSON()); } " +"""
replacement2 = """                                "(function() { " +
                                "  try { " +
                                "    if (typeof window.getProjectJson === 'function') { return window.getProjectJson(); } " +
                                "    if (window.vm) { return JSON.stringify(window.vm.toJSON()); } " +"""

if target2 in content:
    content = content.replace(target2, replacement2)
    print("Fixed target 2")
else:
    print("Failed to fix target 2")

with open("app/src/main/java/com/example/ui/ScratchEditorScreen.kt", "w") as f:
    f.write(content)
