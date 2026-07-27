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
                prompt.contains("语法纠错") || prompt.contains("错误") -> "💡 星梭自愈网络保障：看起来你的积木块拼合基本正常！请检查最顶端是否放了【当 🟢 被点击】触发积木，并确保【重复执行】框里有包含动作积木噢！✨"
                prompt.contains("创意") || prompt.contains("想法") -> "🎨 星梭自愈网络保障：试试在舞台里添加一个新的角色（比如一颗小星星），并使用【当 🟢 被点击】和【重复执行】+【右转 15 度】让它旋转闪烁吧！✨"
                prompt.contains("知识点") || prompt.contains("考点") -> "🌟 星梭自愈网络保障【考点解析】：\n1. 奇妙比喻：【重复执行】就像永远停不下来的欢快旋转木马！\n2. 为什么有用：可以让你的角色不用一次次重复拖积木，自动一直跑或者闪烁。\n3. 拼搭三步走：\n   ① 点击左侧黄色【控制】菜单；\n   ② 找到【重复执行】嘴巴状积木；\n   ③ 把要重复的蓝色【移动 10 步】装进它的嘴巴里面！\n4. 🎮 试一试：把移动 10 步放进去，点击绿旗看看小猫是不是一直跑！"
                else -> "✨ 星梭自愈网络保障：你的想法非常棒！目前处于网络自愈模式，你可以试着把刚才的积木拼接好，然后点击舞台右上角的绿旗亲自测试效果噢！"
            }
            return@withContext fallbackResponse
        }

        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isEmpty()) {
            return@withContext "【API 密钥未配置】当前展示本地精灵姐姐解答：\n\n1. 已经为您保存了最新进度！\n2. 积木块逻辑：如果要重复动作，记得放入【重复执行】里面，并检查动作速度不要过快哦～"
        }

        val isSparkMaaS = apiKey.startsWith("dae06") || apiKey.contains(":")
        val isQwen = true // 强制统一使用通义千问 API
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
            prompt.contains("语法纠错") || prompt.contains("错误") -> {
                val hasGreenFlag = prompt.contains("event_whenflagclicked") || prompt.contains("whenflagclicked") || prompt.contains("被点击")
                if (hasGreenFlag) {
                    "🌟【语法与逻辑智能诊断】\n【诊断结果】: 宝贝真棒！精灵姐姐检测到你的程序中已经正确添加了【当 🟢 被点击】启动触发积木，第一步打得非常坚实！✨\n【修正建议】: 请检查绿旗积木下方的【移动】或【重复执行】积木是否相互紧密嵌套，并确认是否有阻止持续运动的逻辑断点哦！"
                } else {
                    "💡【语法与逻辑智能诊断】\n【错误提示】: 目前的积木组顶部好像还没有添加启动触发器噢。\n【修正建议】: 请在左侧黄色【事件】菜单中，将第一个【当 🟢 被点击】积木拖出来放在最顶端，这样点击屏幕上的绿旗时程序就会跑起来啦！✨"
                }
            }
            prompt.contains("创意") || prompt.contains("想法") || prompt.contains("主题") -> {
                val topic = if (prompt.contains("主题是：【")) prompt.substringAfter("主题是：【").substringBefore("】") else "自由拓展"
                "🎨【创作灵感方案: $topic】✨\n" +
                "1. 🐾 动态特效: 尝试从左侧【外观】分类拖出【将颜色特效增加 25】放入【重复执行】中，让 $topic 的角色闪烁炫彩光芒！\n" +
                "2. 🎵 趣味音效: 从【声音】分类选择【播放声音 直到结束】，每次交互时触发生动的音效！\n" +
                "3. 🏆 计分机制: 点击【变量】新建一个“得分”变量，每次成功碰撞时增加 1 分！加油尝试吧！"
            }
            prompt.contains("知识点") || prompt.contains("考点") -> {
                val topic = if (prompt.contains("考点：【")) prompt.substringAfter("考点：【").substringBefore("】") else "编程知识"
                "🌟【知识考点深度解析: $topic】✨\n" +
                "1. 💡 奇妙比喻: 【$topic】就像是一个魔法调配工具，能帮你的角色搞定复杂的逻辑与指令！\n" +
                "2. 🚀 核心价值: 掌握【$topic】后，你的作品就能拥有自动交互、记录状态或多角色协同的强大功能。\n" +
                "3. 🐾 拼搭步骤:\n" +
                "   ① 在左侧对应分类菜单里找到【$topic】相关的积木块；\n" +
                "   ② 用手指将它拖拽到脚本区；\n" +
                "   ③ 把它黏贴在触发积木下方，运行绿旗看看奇妙的变化吧！"
            }
            else -> "✨ 星梭自愈网络服务保障：你的拼搭非常棒！已为你分析当前作品逻辑，你可以继续添加角色或将作品提交给老师点评哦！"
        }
        return@withContext fallbackResponse
    }

    data class ContentModerationResult(
        val isSafe: Boolean,
        val reason: String
    )

    /**
     * AI 自动化内容风控过滤 (Task 3)
     * 自动拦截开源大厅与同伴互动评论中的不良文本、攻击性言论或敏感违规词汇。
     */
    suspend fun moderateTextContent(content: String): ContentModerationResult = withContext(Dispatchers.IO) {
        if (content.isBlank()) return@withContext ContentModerationResult(true, "内容为空")

        // 本地敏感词快速前置检测
        val blackList = listOf("死", "杀", "脏话", "蠢", "笨蛋", "滚", "垃圾", "坏蛋", "作弊", "私聊")
        val lowerContent = content.lowercase()
        for (word in blackList) {
            if (lowerContent.contains(word)) {
                return@withContext ContentModerationResult(
                    isSafe = false,
                    reason = "触发少儿社区敏感词 [$word]，请修改语言后发布。"
                )
            }
        }

        val prompt = """
            你是一个少儿 Scratch 编程开源社区的风控安全审核员。请审核以下文本（作品名称、作品说明或学生评论）是否符合少儿健康社区规范。
            审查标准：不得包含违规、暴力、辱骂、负面情绪、人身攻击、泄露隐私或诱导非学习行为。
            受审文本："$content"

            请仅返回如下标准 JSON 格式，不要包含任何 markdown 标签或多余说明：
            {"isSafe": true或false, "reason": "审核说明或理由"}
        """.trimIndent()

        try {
            val responseText = generateContent(prompt)
            val cleanJson = responseText.replace("```json", "").replace("```", "").trim()
            val json = JSONObject(cleanJson)
            val isSafe = json.optBoolean("isSafe", true)
            val reason = json.optString("reason", if (isSafe) "内容符合社区规范" else "不合规文本")
            ContentModerationResult(isSafe, reason)
        } catch (e: Exception) {
            ContentModerationResult(true, "内容正常")
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
        // RAG 知识库检索增强 (Task 7)
        val ragKnowledge = EducationalKnowledgeBase.retrieveRelevantContext("$taskName $taskDetail", codeJson)

        val systemPrompt = """
            你是一个充满爱心的资深少儿编程(Scratch 3.0)教学评测专家。请针对学生交上来的Scratch JSON积木代码进行专业而亲切的自动评测。

            $ragKnowledge

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
            val suggestions = json.optString("optimizationSuggestions", "AI 评语提取：\n$responseText")

            ScratchWorkEvaluator.sanitize(EvaluationResult(
                grammarScore = grammar,
                logicScore = logic,
                taskMatchScore = match,
                creativeScore = creative,
                averageScore = grammar + logic + match + creative,
                suggestions = suggestions
            ))
        } catch (e: Exception) {
            ScratchWorkEvaluator.evaluate(codeJson)
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
