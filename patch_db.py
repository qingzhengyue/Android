with open("app/src/main/java/com/example/data/Database.kt", "r") as f:
    content = f.read()

content = content.replace(
    'val workCode: String, // Scratch作品代码 (JSON)',
    'val workCode: String, // Scratch作品代码 (JSON)\n    @ColumnInfo(name = "coverUrl")\n    @SerialName("cover_url")\n    val coverUrl: String? = null, // 作品封面'
)
content = content.replace('version = 8', 'version = 9')

with open("app/src/main/java/com/example/data/Database.kt", "w") as f:
    f.write(content)
