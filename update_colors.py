with open("app/src/main/java/com/example/ui/StudentScreens.kt", "r") as f:
    content = f.read()

# Replace with premium gradient hex codes
content = content.replace("listOf(Color(0xFF81C784), Color(0xFF388E3C))", "listOf(Color(0xFF84DFB4), Color(0xFF28B48F))") # Fresh Teal Gradient
content = content.replace("listOf(Color(0xFF64B5F6), Color(0xFF1976D2))", "listOf(Color(0xFF8AB4F8), Color(0xFF4285F4))") # Soft Blue
content = content.replace("listOf(Color(0xFFFFB74D), Color(0xFFF57C00))", "listOf(Color(0xFFFFD180), Color(0xFFFF8F00))") # Warm Amber
content = content.replace("listOf(Color(0xFFBA68C8), Color(0xFF7B1FA2))", "listOf(Color(0xFFD7A1F9), Color(0xFF9333EA))") # Elegant Purple

with open("app/src/main/java/com/example/ui/StudentScreens.kt", "w") as f:
    f.write(content)
