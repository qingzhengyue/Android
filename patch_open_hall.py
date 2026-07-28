import re

with open("app/src/main/java/com/example/ui/OpenHallScreen.kt", "r") as f:
    content = f.read()

imports_to_add = """
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
"""

# add imports after the last import
last_import_idx = content.rfind("import ")
end_of_last_import = content.find("\n", last_import_idx)

content = content[:end_of_last_import] + imports_to_add + content[end_of_last_import:]

old_box = """            // 占位预览画廊
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = Color(0xFF0EA5E9),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scratch 3.0 逻辑舞台预览中",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }"""

new_image = """            // 真实的预览图
            AsyncImage(
                model = work.coverUrl ?: "https://picsum.photos/seed/${work.workId}/400/300", // Fallback if no coverUrl
                contentDescription = "作品预览",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.placeholder_image), // 加载中的占位图
                error = painterResource(R.drawable.error_image) // 加载失败显示的图
            )"""

if old_box in content:
    content = content.replace(old_box, new_image)
else:
    print("Failed to find old box")

with open("app/src/main/java/com/example/ui/OpenHallScreen.kt", "w") as f:
    f.write(content)
