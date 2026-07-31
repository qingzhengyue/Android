package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

@Composable
fun BlockItemView(
    opcode: String,
    blockJson: JSONObject?,
    blocksMap: JSONObject?,
    modifier: Modifier = Modifier
) {
    val color = BlockTranslator.getBlockColor(opcode)
    val segments = BlockTextFormatter.formatBlock(opcode, blockJson, blocksMap)

    Box(
        modifier = modifier
            .background(color = color, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            segments.forEach { segment ->
                when (segment) {
                    is BlockSegment.Text -> {
                        Text(
                            text = segment.content,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    is BlockSegment.Parameter -> {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = segment.value,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
