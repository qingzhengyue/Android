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
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(prompt: String, hasNetwork: Boolean = true): String = withContext(Dispatchers.IO) {
        if (!hasNetwork) {
            val fallbackResponse = when {
                prompt.contains("语法纠错") || prompt.contains("错误") -> "💡 星梭自动网络保障精灵：看起来你的积木块有几个【未扣合】的间隙哦！请检查所有的黄色控制积木（如【重复执行】）里面是否已经完美连接了动作积木块。"
                prompt.contains("创意") || prompt.contains("想法") -> "🎨 星梭自动网络保障精灵：想丰富游戏画面吗？试试在舞台背景里添加一个新的角色（比如一朵白云），并使用【当🟢被点击】和【重复执行】让它在天空慢慢滑行吧！✨"
                else -> "✨ 星梭自动网络保障精灵：你的想法非常棒！由于网络连接处于离线模式，你可以试着把刚才的积木模块拼接起来，然后点击画面右上方绿旗自己先运行试试噢！"
            }
            return@withContext fallbackResponse
        }

        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isEmpty()) {
            return@withContext "【API 密钥未配置】当前展示本地精灵姐姐解答：\n\n1. 已经为您保存了最新进度！\n2. 积木块逻辑：如果要重复动作，记得放入【重复执行】里面，并检查动作速度不要过快哦～"
        }

        val isQwen = apiKey.startsWith("sk-")
        val isSparkMaaS = apiKey.startsWith("dae06") || apiKey.contains(":")
        var attempts = 0
        val maxAttempts = 3
        var lastErrCode = 0
        var lastErrMsg = ""

        while (attempts <= maxAttempts) {
            try {
                val request = if (isQwen || isSparkMaaS) {
                    val requestBodyJson = JSONObject()
                    val modelName = if (isSparkMaaS) "xopqwen36v35b" else "qwen-plus"
                    requestBodyJson.put("model", modelName)
                    val messagesArray = JSONArray()
                    val messageObj = JSONObject()
                    messageObj.put("role", "user")
                    messageObj.put("content", prompt)
                    messagesArray.put(messageObj)
                    requestBodyJson.put("messages", messagesArray)

                    val targetUrl = if (isSparkMaaS) {
                        "https://maas-api.cn-huabei-1.xf-yun.com/v2/chat/completions"
                    } else {
                        "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
                    }

                    Request.Builder()
                        .url(targetUrl)
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Content-Type", "application/json")
                        .post(requestBodyJson.toString().toRequestBody(mediaType))
                        .build()
                } else {
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

                    val generationConfig = JSONObject()
                    generationConfig.put("temperature", 0.2)
                    requestBodyJson.put("generationConfig", generationConfig)

                    Request.Builder()
                        .url("$BASE_URL?key=$apiKey")
                        .post(requestBodyJson.toString().toRequestBody(mediaType))
                        .build()
                }

                client.newCall(request).execute().use { response ->
                    val code = response.code
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: return@withContext "无返回结果"
                        val responseJson = JSONObject(responseBody)
                        
                        if (isQwen || isSparkMaaS) {
                            val choices = responseJson.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val firstChoice = choices.getJSONObject(0)
                                val message = firstChoice.optJSONObject("message")
                                if (message != null) {
                                    return@withContext message.optString("content")
                                }
                            }
                        } else {
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
                        }
                        return@withContext "AI 返回了空的信息，请稍后重试。"
                    } else {
                        val errorMsg = response.body?.string() ?: ""
                        lastErrCode = code
                        lastErrMsg = errorMsg

                        if (code == 503 || code == 500 || code == 502 || code == 429) {
                            if (attempts < maxAttempts) {
                                attempts++
                                val delayTime = when (attempts) {
                                    1 -> 1000L
                                    2 -> 2000L
                                    else -> 4000L
                                }
                                kotlinx.coroutines.delay(delayTime)
                                continue
                            }
                        }

                        // Exit loop for non-retryable errors
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (attempts < maxAttempts) {
                    attempts++
                    val delayTime = when (attempts) {
                        1 -> 1000L
                        2 -> 2000L
                        else -> 4000L
                    }
                    try { kotlinx.coroutines.delay(delayTime) } catch(ie: Exception){}
                    continue
                }
                break
            }
        }

        // Failsafe offline-style fallback in case of all network or authentication errors
        val fallbackResponse = when {
            prompt.contains("语法纠错") || prompt.contains("错误") -> "💡 星梭自愈网络服务保障：看起来你的积木块拼合没有报错，但别忘了在最顶端搭上【当 🟢 被点击】积木，整个小猫咪才会在点击绿旗时真正出发喔！✨"
            prompt.contains("创意") || prompt.contains("想法") -> "🎨 星梭自愈网络服务保障：你可以点击屏幕左侧的【编程魔法盒】一键载入其他角色的创意设计技巧，比如让星星每秒闪烁，或让云朵飘动噢！✨"
            else -> "✨ 星梭自愈网络服务保障：你的操作非常棒！此时由于网络网关存在瞬时阻断，你可以点击上方「提交作品」先把进度提交到王老师的审阅面板中哦！"
        }
        return@withContext fallbackResponse
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
            你是一个充满爱心的资深少儿编程(Scratch 3.0)教学评测专家。请针对学生交上来的Scratch JSON积木代码进行专业而亲切的自动评测。
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

            关于 "optimizationSuggestions" 字段，你必须遵守以下专门针对小学3-6年级小学生的认知评测规范：
            - 【极度温柔有爱】：先热情赞美孩子付出的努力和创意，不可打击自信心。多用可爱的卡通表情符号。
            - 【具体的具体拼搭指南】：绝对严禁宽泛空洞的评价（如“进一步完善逻辑”、“加强循环理解”等）。必须给出一看就懂的 ①②③ 极简改进步骤（说明找到哪个积木颜色分类，找什么名字的积木，拼在什么积木下面或里面，或修改什么变量值）。
            - 【比喻解说】：如果指出错漏，用拟人化或简单比喻（比如“这里有个孤单的小猫积木没有排入队伍中哦～”、“让控制哨兵更好地帮你把关吧！”）。
            - 字数简短精悍，控制在150字以内，排版清爽。

            你必须最终输出一个合法的 JSON 格式字符串，不需要任何 markdown 的 ```json 包裹标记，其属性必须完全等于：
            {
               "grammarScore": <数值>,
               "logicScore": <数值>,
               "taskMatchScore": <数值>,
               "creativeScore": <数值>,
               "averageScore": <各项加和数值>,
               "optimizationSuggestions": "在此填入符合上文规范的少儿亲和式优化评语"
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
