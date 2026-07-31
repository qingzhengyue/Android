with open("app/src/main/java/com/example/ui/ScratchEditorScreen.kt", "r") as f:
    content = f.read()

target1 = """                "(function() { " +
                "  try { " +
                "    if (typeof window.getProjectJson === 'function') { return window.getProjectJson(); } " +
                "    var vm = window.vm || (window.scratch && window.scratch.vm); " +
                "    if (vm) { " +"""
                
replacement1 = """                "(function() { " +
                "  try { " +
                "    if (typeof window.getProjectJson === 'function') { return window.getProjectJson(); } " +
                "    var vm = window.vm || (window.scratch && window.scratch.vm); " +
                "    if (vm) { " +
                "      if (typeof vm.toJSON === 'function') { var res = vm.toJSON(); if (typeof res === 'string') return res; return JSON.stringify(res); } " +"""

content = content.replace(target1, replacement1)

target2 = """                                "(function() { " +
                                "  try { " +
                                "    if (typeof window.getProjectJson === 'function') { return window.getProjectJson(); } " +
                                "    if (window.vm) { return JSON.stringify(window.vm.toJSON()); } " +
                                "    else if (window.scratch && window.scratch.vm) { return JSON.stringify(window.scratch.vm.toJSON()); } " +"""
                                
replacement2 = """                                "(function() { " +
                                "  try { " +
                                "    if (typeof window.getProjectJson === 'function') { return window.getProjectJson(); } " +
                                "    var vm = window.vm || (window.scratch && window.scratch.vm); " +
                                "    if (vm && typeof vm.toJSON === 'function') { var res = vm.toJSON(); return (typeof res === 'string') ? res : JSON.stringify(res); } " +"""

content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/ui/ScratchEditorScreen.kt", "w") as f:
    f.write(content)
