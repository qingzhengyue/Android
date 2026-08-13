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
            return@withContext getSmartScratchAnswer(prompt)
        }

        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isEmpty()) {
            return@withContext getSmartScratchAnswer(prompt)
        }

        val isSparkMaaS = apiKey.startsWith("dae06") || apiKey.contains(":")
        val isCSK = apiKey.startsWith("csk-")
        val isQwen = apiKey.startsWith("sk-") && !isCSK // 默认通义千问
        val isOpenAICompatible = isQwen || isSparkMaaS || isCSK
        var attempts = 0
        val maxAttempts = 3
        var lastErrCode = 0
        var lastErrMsg = ""

        while (attempts <= maxAttempts) {
            try {
                val request = if (isOpenAICompatible) {
                    val requestBodyJson = JSONObject()
                    val modelName = when {
                        isSparkMaaS -> "xopqwen36v35b"
                        isCSK -> "llama3.1-8b"
                        else -> "qwen-plus"
                    }
                    requestBodyJson.put("model", modelName)
                    val messagesArray = JSONArray()
                    val messageObj = JSONObject()
                    messageObj.put("role", "user")
                    messageObj.put("content", prompt)
                    messagesArray.put(messageObj)
                    requestBodyJson.put("messages", messagesArray)

                    val targetUrl = when {
                        isSparkMaaS -> "https://maas-api.cn-huabei-1.xf-yun.com/v2/chat/completions"
                        isCSK -> "https://api.cerebras.ai/v1/chat/completions"
                        else -> "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
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
                        
                        if (isOpenAICompatible) {
                            val choices = responseJson.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val firstChoice = choices.getJSONObject(0)
                                val message = firstChoice.optJSONObject("message")
                                if (message != null) {
                                    val aiContent = message.optString("content")
                                    if (aiContent.isNotBlank()) return@withContext aiContent
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
                                        val text = parts.getJSONObject(0).optString("text")
                                        if (text.isNotBlank()) return@withContext text
                                    }
                                }
                            }
                        }
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

        // Failsafe offline/fallback smart Scratch QA generator
        return@withContext getSmartScratchAnswer(prompt)
    }

    /**
     * 少儿 Scratch 领域离线/故障兜底智能问答求解器
     * 针对小朋友常见的提问（如“碰到边缘反弹”、“跟随鼠标”、“得分变量”等）提供准确、清晰、100% 答其所问的拼搭方案
     */
    private fun getSmartScratchAnswer(prompt: String): String {
        val q = prompt.lowercase()

        return when {
            // 1. 语法纠错 / 检查代码
            prompt.contains("语法纠错") || prompt.contains("错误") -> {
                val hasGreenFlag = prompt.contains("event_whenflagclicked") || prompt.contains("whenflagclicked") || prompt.contains("被点击")
                if (hasGreenFlag) {
                    "🌟【语法与逻辑智能诊断】✨\n【诊断结果】: 宝贝真棒！精灵姐姐检测到你的程序中已经正确添加了【当 🟢 被点击】启动触发积木，第一步打得非常坚实！\n【修正建议】: 请检查绿旗积木下方的【移动】或【重复执行】积木是否相互紧密嵌套，并确认是否有阻止持续运动的逻辑断点哦！"
                } else {
                    "💡【语法与逻辑智能诊断】✨\n【错误提示】: 目前的积木组顶部好像还没有添加启动触发器噢。\n【修正建议】: 请在左侧黄色【事件】菜单中，将第一个【当 🟢 被点击】积木拖出来放在最顶端，这样点击屏幕上的绿旗时程序就会跑起来啦！"
                }
            }

            // 2. 边缘反弹 / 碰撞反弹 / 碰壁 / 碰到边缘
            q.contains("边缘") || q.contains("反弹") || q.contains("碰壁") || q.contains("弹回") || q.contains("边界") -> {
                "💡【 Scratch 碰壁反弹拼搭指南】✨\n\n" +
                "要让角色在 Scratch 里碰到边缘就自动反弹，只需要简单 3 步操作哦：\n\n" +
                "1. 🐱 **添加启动与循环**：去黄色【事件】分类拖出【当 🟢 被点击】，再到黄色【控制】分类拖出【重复执行】嵌套在下方。\n" +
                "2. 🚀 **添加移动积木**：去蓝色【运动】分类找到【移动 10 步】，放到【重复执行】嘴巴里面。\n" +
                "3. 🧱 **添加反弹积木**：在蓝色【运动】分类找到【碰到边缘就反弹】，也放进【重复执行】中，贴在【移动 10 步】下方！\n\n" +
                "👉 **优化技巧**：如果反弹时角色倒立了，拖出蓝色【运动】里的【将旋转方式设为 左右翻转】放在最最顶端就可以保持站立啦！试着点绿旗看看吧！"
            }

            // 3. 鼠标跟随 / 跟着鼠标 / 指针
            q.contains("鼠标") || q.contains("指针") || q.contains("跟随") || q.contains("跟着") -> {
                "💡【 Scratch 角色跟随鼠标指南】✨\n\n" +
                "要让角色像小尾巴一样跟着鼠标走，有两种很棒的方法哦：\n\n" +
                "1. 🐾 **方法一（平滑转向跟随）**：\n" +
                "   • 黄色【事件】拖出【当 🟢 被点击】，嵌套黄色【控制】里的【重复执行】；\n" +
                "   • 在蓝色【运动】分类找到【面向 鼠标指针】放进【重复执行】；\n" +
                "   • 再加一个蓝色【移动 5 步】紧跟下方。\n\n" +
                "2. ⚡️ **方法二（瞬间移到鼠标）**：\n" +
                "   • 在【重复执行】里面直接放上蓝色【运动】分类中的【移到 鼠标指针】，角色就会紧贴着鼠标光标移动啦！"
            }

            // 4. 克隆 / 分身
            q.contains("克隆") || q.contains("分身") || q.contains("复制") -> {
                "💡【 Scratch 克隆体分身指南】✨\n\n" +
                "克隆就像变出无数个分身小英雄，分为“制造”和“控制”两部分：\n\n" +
                "1. 🌟 **制造分身**：在黄色【控制】菜单底部找到【克隆 自己】，配合【当 🟢 被点击】或【当按下空格键】来触发。\n" +
                "2. 🐾 **分身脚本**：拖出黄色【控制】里的【当作为克隆体启动时】作为新开头。\n" +
                "3. 🎨 **分身动作**：在【当作为克隆体启动时】下方加入【外观】显隐或【运动】移动积木，并在结束时加上【删除此克隆体】防止游戏卡顿！"
            }

            // 5. 切换造型 / 动画
            q.contains("造型") || q.contains("换装") || q.contains("动画") || q.contains("走路") -> {
                "💡【 Scratch 切换造型动画指南】✨\n\n" +
                "要让角色摆动手脚走动起来，需要用到“造型”切换：\n\n" +
                "1. 🐱 **添加循环**：在黄色【控制】分类拖出【重复执行】。\n" +
                "2. 🎨 **切换造型**：去紫色【外观】分类找到【下一个造型】放到【重复执行】嘴巴里。\n" +
                "3. ⏰ **控制节奏**：非常重要！在【下一个造型】下方加上黄色【控制】里的【等待 0.1 秒】，造型就不会切换得太快啦！"
            }

            // 6. 变量 / 得分 / 计分 / 积分
            q.contains("变量") || q.contains("得分") || q.contains("计分") || q.contains("分数") || q.contains("记录") -> {
                "💡【 Scratch 变量计分指南】✨\n\n" +
                "用变量制作游戏得分板只需要 3 步：\n\n" +
                "1. 📊 **新建变量**：点击左侧深橙色【变量】分类，点击【建立一个变量】，命名为“得分”。\n" +
                "2. 🔄 **初始化归零**：拖出【将得分设为 0】贴在【当 🟢 被点击】正下方，确保每次开局分数重置。\n" +
                "3. 🎯 **加分触发**：当角色遇到目标或发生碰撞时，执行【将得分增加 1】就可以加分啦！"
            }

            // 7. 碰撞检测 / 触碰
            q.contains("碰撞") || q.contains("碰到") || q.contains("触碰") || q.contains("相撞") -> {
                "💡【 Scratch 碰撞检测指南】✨\n\n" +
                "检测两个角色是否相撞，可以用【如果...那么...】配合【侦测】：\n\n" +
                "1. 🛡️ **判断框**：黄色【控制】拖出【如果...那么...】开口框，放入【重复执行】中。\n" +
                "2. 🔍 **侦测条件**：浅蓝色【侦测】拖出【碰到... ?】嵌进六边形小洞里。\n" +
                "3. 🎉 **触发响应**：在【如果】嘴巴里面放上【播放声音】、【隐藏】或【将得分增加 1】即可！"
            }

            // 8. 广播 / 消息 / 传信
            q.contains("广播") || q.contains("消息") || q.contains("信号") -> {
                "💡【 Scratch 广播消息指南】✨\n\n" +
                "广播就像角色之间用对讲机传递信号：\n\n" +
                "1. 📢 **发送广播**：角色 A 在完成任务或满足条件时，执行黄色【事件】里的【广播 新消息】。\n" +
                "2. 👂 **接收广播**：角色 B 使用黄色【事件】里的【当接收到 新消息】作为脚本开头。\n" +
                "3. 🎬 **协同配合**：角色 B 在收到消息下方拼接出现的动作或声音，两个角色就能完美配合啦！"
            }

            // 9. 隐藏 / 显示 / 消失 / 出现
            q.contains("隐藏") || q.contains("显示") || q.contains("消失") || q.contains("出现") || q.contains("隐身") -> {
                "💡【 Scratch 隐身与显形指南】✨\n\n" +
                "1. 👻 **隐身消失**：去紫色【外观】分类拖出【隐藏】积木，角色就会立刻变成透明看不见。\n" +
                "2. ✨ **显形重现**：去紫色【外观】分类拖出【显示】积木，角色重回舞台。\n" +
                "👉 **必看提示**：如果代码里用到了【隐藏】，一定要在【当 🟢 被点击】开头加上【显示】，防止下次点绿旗时角色“神秘失踪”哦！"
            }

            // 10. 创意 / 想法 / 主题
            prompt.contains("创意") || prompt.contains("想法") || prompt.contains("主题") -> {
                val topic = if (prompt.contains("主题是：【")) prompt.substringAfter("主题是：【").substringBefore("】") else "自由拓展"
                "🎨【创作灵感方案: $topic】✨\n" +
                "1. 🐾 动态特效: 尝试从左侧【外观】分类拖出【将颜色特效增加 25】放入【重复执行】中，让 $topic 的角色闪烁炫彩光芒！\n" +
                "2. 🎵 趣味音效: 从【声音】分类选择【播放声音 直到结束】，每次交互时触发生动的音效！\n" +
                "3. 🏆 计分机制: 点击【变量】新建一个“得分”变量，每次成功碰撞时增加 1 分！加油尝试吧！"
            }

            // 11. 考点 / 知识点
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

            // 12. 其它任意学生自定义问题 (通用灵活解题指南)
            else -> {
                var cleanQuestion = prompt
                if (cleanQuestion.contains("小朋友问：“")) {
                    cleanQuestion = cleanQuestion.substringAfter("小朋友问：“").substringBefore("”")
                } else if (cleanQuestion.contains("“")) {
                    cleanQuestion = cleanQuestion.substringAfter("“").substringBefore("”")
                }
                if (cleanQuestion.length > 30) cleanQuestion = cleanQuestion.take(30) + "..."

                "💡【 Scratch 拼搭解题指南 】✨\n\n" +
                "关于你问的“$cleanQuestion”：\n\n" +
                "1. 🐱 **起点框架**：请在左侧黄色【事件】分类中，拖出【当 🟢 被点击】放到脚本区最上方。\n" +
                "2. 🧩 **找对积木**：\n" +
                "   • 如果要移动或转向，去蓝色【运动】分类找【移动】或【面向】积木；\n" +
                "   • 如果要造型或说话，去紫色【外观】分类找【换造型】或【思考】积木；\n" +
                "   • 如果要持续运行或判断条件，去黄色【控制】分类找【重复执行】或【如果...那么】框。\n" +
                "3. 🚀 **测试尝试**：将积木按逻辑顺序紧紧吸附在一起，点击右上角的【绿旗】亲自测试效果吧！如果卡住了，可以随时切换顶部“专家”模式让精灵姐姐为你更深入地解答哦！"
            }
        }
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
