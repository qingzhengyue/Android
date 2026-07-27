package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 通用空状态UI组件
 */
@Composable
fun EmptyStateView(
    modifier: Modifier = Modifier,
    title: String = "暂无数据",
    subtitle: String = "这里空空如也~"
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = "暂无数据",
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFD1D5DB) // 浅灰色
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 主标题
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4B5563) // 中灰色
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 副标题说明
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = Color(0xFF9CA3AF), // 浅灰色
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
    }
}
