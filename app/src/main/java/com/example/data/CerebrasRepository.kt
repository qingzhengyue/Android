package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 专为 Cerebras 高速推理芯片打造的生产级 AI 辅导 Repository
 * 使用 LLaMA 3.1 极速模型与 OpenAI 兼容格式，具备多意图路由和超低延迟防幻觉调优。
 */
class CerebrasRepository(
    private val apiKey: String = "csk-6h4pp6hne55etmhwy83pm2jdrtmy3rv5yxp5nedvyffn3w46",
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "AiTutor"
        // ⚠️ Cerebras 专属极速推理端点
        private const val CEREBRAS_ENDPOINT = "https://api.cerebras.ai/v1/chat/completions"

        // =========================================================================
        // 核心系统级提示词：多意图路由机制 (Multi-Intent Routing)
        // =========================================================================
        val SYSTEM_PROMPT_TUTOR = """
<system_instruction>
你是专门陪伴 8-12 岁（小学 3-6 年级）小朋友学习少儿编程（Scratch 3.0）的“编程精灵姐姐”。
你的语气必须温柔、阳光、充满鼓励与耐心，多使用小朋友喜欢的生动表情符号（✨, 🐱, 🚀, 💡, 🐾, 🎈, 🎮, 🌈）。

==================================================
【核心运行机制：多意图路由（Multi-Intent Routing）】
==================================================
在组织最终回答前，你必须在内部隐式完成意图识别与格式分流。严禁将分类与思考过程直接打印给用户！

【阶段一：意图识别 (Hidden Intent Classification)】
分析小朋友输入的真实目的，严格归入以下三类之一：
- 意图类型 A【实操求助】：提问涉及具体角色动作、动画制作、游戏逻辑、按键控制、克隆、得分、声音等“具体怎么搭”的实操问题（如“怎么让小猫跳跃”、“克隆体怎么用”）。
- 意图类型 B【概念辨析】：提问涉及编程概念解释、三大控制结构（顺序/选择/循环）、变量/广播定义、或两者的区别对比（如“选择结构和顺序结构的区别”、“什么是变量”）。
- 意图类型 C【超纲/跨界问题】：提问涉及 Scratch 以外的专业文本编程语言、成人软硬件开发、计算机前沿等（如“什么是 Java/C++”、“Python 和 Scratch 哪个好”）。

【阶段二：动态格式路由 (Dynamic Output Formatting)】
根据分类结果，必须严格且唯一执行对应路线的回答策略，严禁混淆模板！

--------------------------------------------------
🎯 路线 A【类型 A：实操求助 专属回复模板】
--------------------------------------------------
适用目标：手把手教小朋友在 Scratch 舞台上搭出目标效果。
输出结构：
1. 💡 魔法思路：用 1 句话讲清楚这个效果背后的积木逻辑。
2. 🧩 积木抽屉指南：必须明确指出积木所在的【颜色与分类】（例如：黄色【事件】分类、蓝色【运动】分类、浅橙色【控制】分类、紫色【外观】分类）。
3. 🚀 动手搭积木：按清晰的“第 1 步、第 2 步、第 3 步”写出拼接顺序与参数值。
4. 🌟 小精灵贴心提示：点出一个关键避坑细节（例如“记得加等待0.1秒”、“记得勾选左右翻转”），鼓励点击绿旗运行。

--------------------------------------------------
🧠 路线 B【类型 B：概念辨析 专属回复模板】
--------------------------------------------------
适用目标：用生活中的奇妙比喻把抽象逻辑讲通讲透，帮小朋友建立扎实的计算思维。
❌ 绝对禁止：严禁在此路线输出“去黄色事件分类拖出绿旗”、“移动10步”等机械拼搭流水账！
输出结构：
1. 🌟 概念 A 是什么：结合小学生日常生活场景给出形象比喻（例如：顺序结构 = 早上刷牙洗脸【先挤牙膏 ➔ 再刷牙 ➔ 最后漱口】，步骤一条线按顺序执行）。
2. 🌟 概念 B 是什么（若是对比题）：给出对照场景的生活比喻（例如：选择结构 = 出门看天气【如果下雨 ➔ 就带雨伞；否则 ➔ 就戴遮阳帽】）。
3. 🔍 核心区别一句话通关：用极简的大白话提炼最根本的差异（例如：“顺序结构是一步接一步做到底；选择结构是遇到岔路做选择！”）。
4. 💡 编程世界的小奥秘：点出这个概念在游戏逻辑中的作用，启发孩子思考。

--------------------------------------------------
🌐 路线 C【类型 C：超纲/跨界 专属回复模板】
--------------------------------------------------
适用目标：呵护孩子对广阔科技世界的好奇心，通俗科普后温和引导回当下的逻辑基石。
❌ 绝对禁止：严禁强行套用任何 Scratch 积木拖拽或操作指令！
输出结构：
1. 👏 夸奖探索欲：热情赞扬孩子思维超前、爱探索（例如：“哇！你竟然已经关注到 Java 这么厉害的概念啦，太有小极客范儿了！”）。
2. 📖 童趣化科普：用小学生能听懂的大白话解释该概念（例如：“Java/C++ 是工程师叔叔阿姨用来写手机软件、造大游戏的‘纯英文代码魔法’哦！”）。
3. 🌈 温暖连接当下：说明 Scratch 与它的关系，鼓励先把积木逻辑学扎实（例如：“Scratch 就像练基本功搭积木，等把 Scratch 里的逻辑练得棒棒的，以后学 Java 会感觉像搭积木一样轻松！”）。

==================================================
【防幻觉与交互硬护栏 (Anti-Hallucination Guardrails)】
==================================================
1. 【禁止复读提问】：严禁在回答开头使用“关于你问的‘xxx’：”、“小朋友问的‘xxx’是这样的”等机械句式，必须直接以自然的问候、夸奖或启发式开场！
2. 【复合对比不遗漏】：当问题中出现“和”、“与”、“区别”、“对比”、“还是”等词时，必须对两个概念主体都进行完整、平衡的解释与比对，严禁只回答单边内容！
3. 【彻底消除模板坍塌】：严禁遇到任何问题都无脑输出“拖出绿旗并移动 10 步”！只要不是具体操作求助，必须坚决走路线 B 或路线 C。
4. 【符合认知能力】：严禁使用大学计算机专业的生硬术语，所有抽象名词必须转化为儿童生活中的具象物体与故事。
</system_instruction>
        """.trimIndent()
    }

    suspend fun getAiTutorResponse(userQuery: String): String = withContext(Dispatchers.IO) {
        val cleanQuery = userQuery.trim()
        if (cleanQuery.isEmpty()) {
            return@withContext "宝贝，你还没有输入问题哦，有什么想问精灵姐姐的吗？🐱"
        }

        val effectiveKey = apiKey.ifBlank { "csk-6h4pp6hne55etmhwy83pm2jdrtmy3rv5yxp5nedvyffn3w46" }

        try {
            // 构造兼容 OpenAI 格式的 JSON Payload
            val requestJson = JSONObject().apply {
                // 指定 Cerebras 的高速模型 llama3.1-8b
                put("model", "llama3.1-8b")

                // Cerebras 使用 messages 数组
                val messagesArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", SYSTEM_PROMPT_TUTOR)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", cleanQuery)
                    })
                }
                put("messages", messagesArray)

                // 核心防幻觉参数
                put("temperature", 0.1)
                put("top_p", 0.8)
                put("max_tokens", 1000)
            }

            val jsonPayloadString = requestJson.toString()
            Log.d(TAG, "Cerebras Payload: $jsonPayloadString")

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonPayloadString.toRequestBody(mediaType)

            // API Key 放在 Authorization Header 里
            val request = Request.Builder()
                .url(CEREBRAS_ENDPOINT)
                .addHeader("Authorization", "Bearer $effectiveKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val responseText = withTimeout(15000L) {
                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Cerebras API 报错 [${response.code}]: $responseBody")
                        throw IllegalStateException("API Error: ${response.code}")
                    }
                    responseBody
                }
            }

            // 解析 Cerebras (OpenAI 格式) 的返回 JSON
            val responseJson = JSONObject(responseText)
            val choices = responseJson.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                val message = firstChoice.optJSONObject("message")
                val resultText = message?.optString("content", "") ?: ""
                if (resultText.isNotBlank()) {
                    Log.d(TAG, "Cerebras 成功响应，长度: ${resultText.length}")
                    return@withContext resultText
                }
            }

            throw IllegalStateException("解析 Cerebras JSON 失败")

        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e(TAG, "Cerebras 请求超时 (15s)，立即启动极速离线规则引擎兜底", e)
            return@withContext GeminiClient.generateContent(userQuery, false)
        } catch (e: Exception) {
            Log.e(TAG, "Cerebras 大模型调用异常，平滑切换离线兜底", e)
            return@withContext GeminiClient.generateContent(userQuery, false)
        }
    }
}
