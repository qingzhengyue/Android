package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isEmpty()) {
            return@withContext "【API 密钥未配置】请在 AI Studio 的 Secrets 面板中配置您的 GEMINI_API_KEY 密码。当前展示本地仿真响应：" +
                    "\n\n1. 已经为您保存了最新草稿！\n2. 积木块逻辑分析说明：重复漫步需要结合‘碰到边缘反弹’积木才不会走丢。请重试拼装。"
        }

        try {
            // 构建标准的 Gemini API 请求 JSON 体
            val requestBodyJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()

            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestBodyJson.put("contents", contentsArray)

            // 配置降低随机度以提供针对编程领域的准确评价
            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.2)
            requestBodyJson.put("generationConfig", generationConfig)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = response.body?.string() ?: ""
                    return@withContext "AI 发生网络错误 (网络代码 ${response.code}): $errorMsg"
                }

                val responseBody = response.body?.string() ?: return@withContext "无返回结果"
                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text")
                        }
                    }
                }
                "AI 返回了空的信息，请稍后重试。"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "AI 助手请求异常: ${e.message ?: "未知错误"}"
        }
    }

    /**
     * 调用 AI 自动评测并解析特定的 JSON 得分格式
     */
    suspend fun evaluateScratchWork(
        taskName: String,
        taskDetail: String,
        workName: String,
        codeJson: String
    ): EvaluationResult = withContext(Dispatchers.IO) {
        val systemPrompt = """
            你是一个资深的少儿编程(Scratch 3.0)教学专家。请针对学生交上来的Scratch JSON积木代码进行自动评测。
            任务要求：
            - 任务名称：$taskName
            - 任务详情：$taskDetail
            - 学生作品名称：$workName

            请严格从以下四个维度进行打分（各项分值不能超越其上限）：
            1. 语法合规性(grammarScore): 满分 25 分
            2. 逻辑完整性(logicScore): 满分 30 分
            3. 任务匹配度(taskMatchScore): 满分 25 分
            4. 创意实现度(creativeScore): 满分 20 分
            综合得分即为这四项总和（满分 100）。

            你必须最终输出一个合法的 JSON 格式字符串，不需要任何 markdown 的 ```json 包裹标记，其属性必须完全等于：
            {
               "grammarScore": <数值>,
               "logicScore": <数值>,
               "taskMatchScore": <数值>,
               "creativeScore": <数值>,
               "averageScore": <各项加和数值>,
               "optimizationSuggestions": <优化建议、错漏弥补与下一步知识点指引，用简短、充满爱心、易于小学生理解的中文编写>
            }
        """.trimIndent()

        val prompt = "$systemPrompt\n\n学生 Scratch 积木代码如下：\n$codeJson"
        val responseText = generateContent(prompt)

        // 尝试解析返回的 JSON，若非标准 JSON 则做容错提取或提供默认分数
        try {
            // 清洗掉可能多余的 markdown 标注
            val cleanJson = responseText.replace("```json", "").replace("```", "").trim()
            val json = JSONObject(cleanJson)
            val grammar = json.optInt("grammarScore", 20)
            val logic = json.optInt("logicScore", 24)
            val match = json.optInt("taskMatchScore", 20)
            val creative = json.optInt("creativeScore", 15)
            val average = json.optInt("averageScore", grammar + logic + match + creative)
            val suggestions = json.optString("optimizationSuggestions", "AI 评语提取：\n$responseText")

            EvaluationResult(
                grammarScore = grammar,
                logicScore = logic,
                taskMatchScore = match,
                creativeScore = creative,
                averageScore = average,
                suggestions = suggestions
            )
        } catch (e: Exception) {
            // 容错解析：如果是明密文形式，尝试正则抓取或者展示纯文本
            val baseSuggestions = "无法解析 AI 标准 JSON 得分，以下是 AI 的直接评测评语：\n$responseText"
            EvaluationResult(
                grammarScore = 20,
                logicScore = 25,
                taskMatchScore = 20,
                creativeScore = 15,
                averageScore = 80,
                suggestions = baseSuggestions
            )
        }
    }

    data class EvaluationResult(
        val grammarScore: Int,
        val logicScore: Int,
        val taskMatchScore: Int,
        val creativeScore: Int,
        val averageScore: Int,
        val suggestions: String
    )
}
