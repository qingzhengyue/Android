import re

with open('app/src/main/java/com/example/ui/AITutoringScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# We need to insert the EmptyState logic into AITutoringScreen
# Inside AITutoringScreen:
#         } else {
#             ChatFlowContent(
#                 modifier = Modifier.padding(innerPadding),
#                 chatHistory = chatHistory,
#                 isLoading = isLoading
#             )
#         }

replacement = """
        ) { innerPadding ->
            if (chatHistory.isEmpty()) {
                EmptyState(modifier = Modifier.padding(innerPadding))
            } else {
                ChatFlowContent(
                    modifier = Modifier.padding(innerPadding),
                    chatHistory = chatHistory,
                    isLoading = isLoading
                )
            }
        }
"""

content = re.sub(
    r'\) \{\s*innerPadding ->\s*ChatFlowContent\(\s*modifier = Modifier\.padding\(innerPadding\),\s*chatHistory = chatHistory,\s*isLoading = isLoading\s*\)\s*\}',
    replacement.strip(),
    content
)

empty_state_composable = """
@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🤖", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "遇到 Scratch 难题了？拍个照或者直接问我吧！",
            color = Color(0xFF9CA3AF),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
"""

if "fun EmptyState(" not in content:
    content += "\n" + empty_state_composable

with open('app/src/main/java/com/example/ui/AITutoringScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
