package com.example.data

import kotlinx.serialization.Serializable
import androidx.room.Entity
import androidx.room.PrimaryKey

// 1. 提示词与来源标识
enum class PromptSource {
    LOCAL,   // 本地通用（系统级）
    CLOUD    // 云端下发（班级专属）
}

data class PromptChipModel(
    val id: String,
    val text: String,
    val icon: String,
    val source: PromptSource
)

// 2. 聊天消息多态结构
sealed class ChatMessage {
    abstract val id: String
    abstract val isFromStudent: Boolean

    // 普通文本消息
    data class TextMessage(
        override val id: String,
        override val isFromStudent: Boolean,
        val text: String
    ) : ChatMessage()

    // 积木介绍富文本卡片消息
    data class BlockIntroCardMessage(
        override val id: String,
        override val isFromStudent: Boolean = false,
        val blockName: String,     
        val blockImageUrl: String?, 
        val description: String,   
        val exampleGifUrl: String? 
    ) : ChatMessage()
}

// 3. 大模型 JSON 交互协议
@Serializable
data class AiResponse(
    val type: String,
    val data: String
)

@Serializable
data class BlockCardPayload(
    val blockId: String,
    val blockName: String,
    val dynamicDescription: String,
    val suggestion: String
)

// 4. Room Entity
@Entity(tableName = "cloud_prompts")
data class PromptEntity(
    @PrimaryKey val id: String,
    val classId: String,
    val text: String,
    val icon: String,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)
