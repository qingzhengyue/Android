import os

with open("app/src/main/java/com/example/data/DatabasePrepopulator.kt", "r", encoding="utf-8") as f:
    content = f.read()

target = "val dao = db.appDao"
replacement = """val dao = db.appDao
        
        // --- 数据清理：修复早期测试数据导致学号带有 S 的问题 ---
        try {
            db.openHelper.writableDatabase.execSQL("UPDATE student SET studentNumber = REPLACE(studentNumber, 'S', '') WHERE studentNumber LIKE '%S%'")
        } catch (e: Exception) {
            e.printStackTrace()
        }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/data/DatabasePrepopulator.kt", "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched DatabasePrepopulator")
else:
    print("Target not found")
