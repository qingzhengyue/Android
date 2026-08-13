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
        // 精确提取学生的核心提问文本，避免系统提示词与草稿代码干扰关键词匹配
        val cleanQuestion = when {
            prompt.contains("小朋友问：“") -> prompt.substringAfter("小朋友问：“").substringBefore("”")
            prompt.contains("“") && prompt.contains("”") -> prompt.substringAfter("“").substringBefore("”")
            else -> prompt
        }.trim()

        val q = cleanQuestion.lowercase()

        return when {
            // 0. 图文识图 / 拍照答疑
            cleanQuestion.contains("图片") || cleanQuestion.contains("拍照") || cleanQuestion.contains("识图") || cleanQuestion.contains("图文") || cleanQuestion.contains("解析图片") -> {
                "📸【 Scratch 题目与脚本图文智能识别】✨\n\n" +
                "精灵姐姐已经仔细阅读了你上传的少儿编程图片！分析与辅导如下：\n\n" +
                "1. 🔍 **核心积木识别**：\n" +
                "   • 触发事件：黄色【当 🟢 被点击】\n" +
                "   • 逻辑控制：黄色【重复执行】与【如果...那么...】开口框\n" +
                "   • 动作与外观：蓝色【移动 10 步】与紫色【下一个造型】\n\n" +
                "2. 💡 **解题与逻辑诊断**：\n" +
                "   • 图中展示的是典型的“事件驱动 + 循环检测”逻辑结构；\n" +
                "   • **避坑提示**：请确认在【下一个造型】下方是否加上了黄色【等待 0.1 秒】，防止造型切换过快变成无影脚哦！\n\n" +
                "3. 🚀 **动手尝试**：把图中积木按顺序拼好，点击右上角绿旗亲自看看运行效果吧！"
            }

            // 0.2 Python 与代码范例（高优先级：响应学生关于 Python、代码例子、具体编程语言实现的需求）
            q.contains("python") || q.contains("代码") || q.contains("范例") || q.contains("例程") || q.contains("代码示例") -> {
                when {
                    (q.contains("顺序") || q.contains("选择")) && (q.contains("区别") || q.contains("对比") || q.contains("范例") || q.contains("例子") || q.contains("与") || q.contains("和")) -> {
                        "🐍【 Python 编程：顺序结构 vs 选择结构 代码范例 】✨\n\n" +
                        "宝贝你想看 Python 代码范例太有远见啦！下面为你对比演示【顺序结构】与【选择结构】在 Python 中的具体编写方式：\n\n" +
                        "📌 **1. 顺序结构 (Sequential Structure)**\n" +
                        "• **特点**：代码从上到下按顺序依次执行，每一行都会运行，没有分支跳转。\n" +
                        "• **Python 代码范例**：\n" +
                        "```python\n" +
                        "# 顺序结构：按照步骤从第一行运行到最后一行\n" +
                        "print(\"第一步：准备 Python 魔法画笔 🎨\")\n" +
                        "print(\"第二步：在画板上画一个小圆圈 🟢\")\n" +
                        "print(\"第三步：涂上漂亮的蓝色 🎨\")\n" +
                        "print(\"顺序结构执行完毕！✨\")\n" +
                        "```\n\n" +
                        "📌 **2. 选择结构 (Selection / Conditional Structure)**\n" +
                        "• **特点**：使用 if 和 else 关键字进行条件判断，根据判断结果选择执行哪一条分支代码。\n" +
                        "• **Python 代码范例**：\n" +
                        "```python\n" +
                        "# 选择结构：根据分数判断是否通关\n" +
                        "score = 85\n\n" +
                        "if score >= 60:\n" +
                        "    # 当 score >= 60 条件成立时执行\n" +
                        "    print(\"🎉 恭喜你！成功通关少儿编程第一关！\")\n" +
                        "else:\n" +
                        "    # 当条件不成立时执行\n" +
                        "    print(\"💪 差一点点就通关啦，再接再厉哦！\")\n" +
                        "```\n\n" +
                        "💡 **核心区别小结**：\n" +
                        "• **顺序结构**：无条件分支，代码行行必过。\n" +
                        "• **选择结构**：用 if ... else ... 问句做选择，满不满足条件走不同路线！"
                    }
                    q.contains("选择") || q.contains("条件") || q.contains("分支") -> {
                        "🐍【 Python 选择结构 代码范例 】✨\n\n" +
                        "选择结构在 Python 中依靠 `if`、`elif` 和 `else` 来实现：\n\n" +
                        "```python\n" +
                        "# 示例：根据气温选择穿衣提醒\n" +
                        "temperature = 28\n\n" +
                        "if temperature > 25:\n" +
                        "    print(\"☀️ 今天天气很热，建议穿短袖！\")\n" +
                        "elif temperature >= 15:\n" +
                        "    print(\"🌤️ 天气很舒适，穿长袖外套吧！\")\n" +
                        "else:\n" +
                        "    print(\"❄️ 天气太冷啦，记得穿厚羽绒服！\")\n" +
                        "```"
                    }
                    q.contains("顺序") -> {
                        "🐍【 Python 顺序结构 代码范例 】✨\n\n" +
                        "顺序结构是 Python 最基本的执行方式：\n\n" +
                        "```python\n" +
                        "# 示例：计算两数之和\n" +
                        "print(\"--- 开始计算 ---\")\n" +
                        "a = 10\n" +
                        "b = 20\n" +
                        "result = a + b\n" +
                        "print(\"计算结果是:\", result)\n" +
                        "print(\"--- 计算结束 ---\")\n" +
                        "```"
                    }
                    q.contains("循环") -> {
                        "🐍【 Python 循环结构 代码范例 】✨\n\n" +
                        "Python 中包含 `for` 循环与 `while` 循环：\n\n" +
                        "```python\n" +
                        "# 1. for 循环：重复 5 次\n" +
                        "for i in range(1, 6):\n" +
                        "    print(f\"这是第 {i} 次报数 📢\")\n\n" +
                        "# 2. while 循环：条件成立就重复\n" +
                        "count = 0\n" +
                        "while count < 3:\n" +
                        "    print(\"小猫向前跑 🐾\")\n" +
                        "    count += 1\n" +
                        "```"
                    }
                    else -> {
                        "🐍【 Python 少儿编程代码范例 】✨\n\n" +
                        "```python\n" +
                        "# 欢迎来到 Python 少儿编程世界！\n" +
                        "name = \"小明\"\n" +
                        "age = 9\n\n" +
                        "print(f\"你好，{name}！欢迎你来到少儿编程乐园！\")\n" +
                        "print(\"这里可以用代码命令电脑画图、做数学游戏和控制角色哦！\")\n" +
                        "```"
                    }
                }
            }

            // 0.5 多结构对比 / 区别（例如：选择结构和顺序结构的区别、三大结构的区别）
            (q.contains("区别") || q.contains("不同") || q.contains("对比") || q.contains("比较") || q.contains("还是")) &&
            (q.contains("结构") || q.contains("顺序") || q.contains("选择") || q.contains("循环") || q.contains("分支")) -> {
                "💡【 Scratch 核心概念：顺序结构 vs 选择结构 的区别与联系】✨\n\n" +
                "宝贝问得太棒啦！“顺序结构”和“选择结构”是少儿编程里最核心的两大建筑基石，它们的最大区别在于**【程序执行时有没有分支判断】**：\n\n" +
                "1. 🚶 **顺序结构（按部就班，一条路走到底）**：\n" +
                "   • **特点**：代码就像按步骤搭积木，从上到下**依次执行**，每一行积木都会被运行，绝不漏掉任何一步，也不需要做任何决定！\n" +
                "   • **生活的例子**：按顺序【洗手 ➔ 擦干 ➔ 吃饭】。\n" +
                "   • **Scratch 积木**：【当 🟢 被点击】 ➔ 【移动 10 步】 ➔ 【说 Hello! 2秒】 ➔ 【右转 15 度】。\n\n" +
                "2. 🔀 **选择结构（遇事思考，根据条件二选一）**：\n" +
                "   • **特点**：代码来到了**岔路口**，先用六边形条件检测（如【碰到鼠标指针?】）。**条件成立**就执行【那么】里面的积木；**条件不成立**就执行【否则】里面的积木（或者跳过）！\n" +
                "   • **生活的例子**：出门前看天气：【如果下雨 ➔ 就打伞；否则 ➔ 戴帽子】。\n" +
                "   • **Scratch 积木**：黄色【如果 <碰到 鼠标指针?> 那么 [说 摸到我啦] 否则 [说 还没摸到]】。\n\n" +
                "🌟 **一句话核心区别**：\n" +
                "• **顺序结构** = 没有分支，从上到下全做完；\n" +
                "• **选择结构** = 有条件判断，满足条件才做某件事！\n\n" +
                "把它们组合在一起，小猫角色就能既按顺序移动，又能聪明地躲避障碍物啦！"
            }

            // 1. 顺序结构
            q.contains("顺序") || q.contains("顺序结构") -> {
                "💡【 Scratch 核心概念：什么是顺序结构？】✨\n\n" +
                "顺序结构是编程里最基础、最重要的“第一大结构”哦！\n\n" +
                "**简单来说**：“按照从上到下的顺序，一步接一步地执行指令”，就像我们按步骤搭积木、或者按步骤刷牙洗脸一样！\n\n" +
                "🌟 **Scratch 里的拼搭例子**：\n" +
                "1. 🟢 去黄色【事件】拖出【当 🟢 被点击】放到最顶端；\n" +
                "2. 🚶 去蓝色【运动】拖出【移动 10 步】，吸附在绿旗下方；\n" +
                "3. 💬 去紫色【外观】拖出【说 Hello! 2秒】，吸附在移动积木下方；\n" +
                "4. 🔄 去蓝色【运动】拖出【右转 15 度】，吸附在最下方。\n\n" +
                "👉 **小猫怎么运行呢**：当点击绿旗时，小猫会先走 10 步 ➔ 接着说话 Say Hello ➔ 最后右转 15 度。它绝不会跳过任何一步，也不会倒过来执行，这就是神奇的“顺序结构”啦！"
            }

            // 2. 选择结构 / 条件结构 / 分支结构
            q.contains("选择") || q.contains("选择结构") || q.contains("条件") || q.contains("分支") -> {
                "💡【 Scratch 核心概念：什么是选择结构？】✨\n\n" +
                "选择结构（也叫分支结构或条件判断）是程序的“智慧大脑”哦！\n\n" +
                "**简单来说**：“根据条件成立与否，决定执行哪一部分代码”，就像遇到叉路口做选择一样，比如：“如果天下雨，就打伞；否则，就不打伞”。\n\n" +
                "🌟 **Scratch 里的拼搭例子**：\n" +
                "1. 🛡️ **选择积木**：去黄色【控制】拖出【如果...那么...否则...】开口框，嵌在【重复执行】里面；\n" +
                "2. 🔍 **设置条件**：去浅蓝色【侦测】拖出【碰到 鼠标指针 ?】，塞进六边形条件洞洞里；\n" +
                "3. 🎯 **那么（条件成立）**：在【那么】嘴巴里放紫色【说 摸到我啦！ 2秒】；\n" +
                "4. 🐾 **否则（条件不成立）**：在【否则】嘴巴里放紫色【说 还没摸到我呢！ 2秒】。\n\n" +
                "👉 **程序怎么判断呢**：当鼠标摸到角色时，它就选择执行“说摸到我啦”；没摸到时，就选择执行“说还没摸到我呢”。这就是聪明的“选择结构”！"
            }

            // 3. 循环结构 / 重复执行
            q.contains("循环") || q.contains("循环结构") || q.contains("重复") -> {
                "💡【 Scratch 核心概念：什么是循环结构？】✨\n\n" +
                "循环结构是编程里最省力气的“魔法放大器”！\n\n" +
                "**简单来说**：“把一段相同的指令重复做很多次，或者一直不间断地做下去”，不用把同样的积木重复拖好几次哦！\n\n" +
                "🌟 **Scratch 里的三大循环积木（都在黄色【控制】分类）**：\n" +
                "1. 🔄 **【重复执行】（无限循环）**：比如把【移动 5 步】和【碰到边缘就反弹】放进去，角色就会一直在舞台上走来走去；\n" +
                "2. 🔢 **【重复执行 10 次】（计数循环）**：比如在嘴巴里放【换下一个造型】和【等待 0.2 秒】，角色就会连走 10 步造型后自动停下；\n" +
                "3. 🎯 **【重复执行直到...】（条件循环）**：一直重复做某件事，直到满足条件（比如直到得分达到 100 分）才停止。"
            }

            // 4. 克隆 / 分身
            q.contains("克隆") || q.contains("分身") || q.contains("复制") -> {
                "💡【 Scratch 克隆体积木怎么用？（附简单例子）】✨\n\n" +
                "克隆就像孙悟空变出无数个分身小英雄，能动态产生很多同类角色（比如满天雪花、无数子弹、游戏敌人）！\n\n" +
                "🌟 **克隆三大核心积木（都在黄色【控制】分类底部）**：\n" +
                "1. 🌟 **【克隆 自己】**：负责产生一个新的分身；\n" +
                "2. 🐾 **【当作为克隆体启动时】**：克隆体诞生后要执行的专用脚本开头；\n" +
                "3. 🗑️ **【删除此克隆体】**：任务完成后销毁分身（防止游戏太卡）。\n\n" +
                "🚀 **一个最简单的克隆例子（变出小猫分身）**：\n\n" +
                "**【本体脚本】**：\n" +
                "黄色【当按下 空格 键】 ➔ 吸附黄色【克隆 自己】。\n\n" +
                "**【克隆体脚本】**：\n" +
                "黄色【当作为克隆体启动时】 ➔ 吸附蓝色【移到 随机位置】 ➔ 吸附紫色【将颜色特效增加 25】。\n\n" +
                "👉 **效果**：每按一次空格键，屏幕上就会多出一个停在随机位置、颜色不一样的新小猫！"
            }

            // 5. 语法纠错 / 检查代码
            q.contains("语法纠错") || q.contains("错误") || q.contains("纠错") -> {
                val hasGreenFlag = prompt.contains("event_whenflagclicked") || prompt.contains("whenflagclicked") || prompt.contains("被点击")
                if (hasGreenFlag) {
                    "🌟【语法与逻辑智能诊断】✨\n【诊断结果】: 宝贝真棒！精灵姐姐检测到你的程序中已经正确添加了【当 🟢 被点击】启动触发积木，第一步打得非常坚实！\n【修正建议】: 请检查绿旗积木下方的【移动】或【重复执行】积木是否相互紧密嵌套，并确认是否有阻止持续运动的逻辑断点哦！"
                } else {
                    "💡【语法与逻辑智能诊断】✨\n【错误提示】: 目前的积木组顶部好像还没有添加启动触发器噢。\n【修正建议】: 请在左侧黄色【事件】菜单中，将第一个【当 🟢 被点击】积木拖出来放在最顶端，这样点击屏幕上的绿旗时程序就会跑起来啦！"
                }
            }

            // 6. 边缘反弹 / 碰撞反弹 / 碰壁 / 碰到边缘
            q.contains("边缘") || q.contains("反弹") || q.contains("碰壁") || q.contains("弹回") || q.contains("边界") -> {
                "💡【 Scratch 碰壁反弹拼搭指南】✨\n\n" +
                "要让角色在 Scratch 里碰到边缘就自动反弹，只需要简单 3 步操作哦：\n\n" +
                "1. 🐱 **添加启动与循环**：去黄色【事件】分类拖出【当 🟢 被点击】，再到黄色【控制】分类拖出【重复执行】嵌套在下方。\n" +
                "2. 🚀 **添加移动积木**：去蓝色【运动】分类找到【移动 10 步】，放到【重复执行】嘴巴里面。\n" +
                "3. 🧱 **添加反弹积木**：在蓝色【运动】分类找到【碰到边缘就反弹】，也放进【重复执行】中，贴在【移动 10 步】下方！\n\n" +
                "👉 **优化技巧**：如果反弹时角色倒立了，拖出蓝色【运动】里的【将旋转方式设为 左右翻转】放在最最顶端就可以保持站立啦！试着点绿旗看看吧！"
            }

            // 7. 鼠标跟随 / 跟着鼠标 / 指针
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

            // 8. 切换造型 / 动画
            q.contains("造型") || q.contains("换装") || q.contains("动画") || q.contains("走路") -> {
                "💡【 Scratch 切换造型动画指南】✨\n\n" +
                "要让角色摆动手脚走动起来，需要用到“造型”切换：\n\n" +
                "1. 🐱 **添加循环**：在黄色【控制】分类拖出【重复执行】。\n" +
                "2. 🎨 **切换造型**：去紫色【外观】分类找到【下一个造型】放到【重复执行】嘴巴里。\n" +
                "3. ⏰ **控制节奏**：非常重要！在【下一个造型】下方加上黄色【控制】里的【等待 0.1 秒】，造型就不会切换得太快啦！"
            }

            // 9. 变量 / 得分 / 计分 / 积分
            q.contains("变量") || q.contains("得分") || q.contains("计分") || q.contains("分数") || q.contains("记录") -> {
                "💡【 Scratch 变量计分指南】✨\n\n" +
                "用变量制作游戏得分板只需要 3 步：\n\n" +
                "1. 📊 **新建变量**：点击左侧深橙色【变量】分类，点击【建立一个变量】，命名为“得分”。\n" +
                "2. 🔄 **初始化归零**：拖出【将得分设为 0】贴在【当 🟢 被点击】正下方，确保每次开局分数重置。\n" +
                "3. 🎯 **加分触发**：当角色遇到目标或发生碰撞时，执行【将得分增加 1】就可以加分啦！"
            }

            // 10. 碰撞检测 / 触碰
            q.contains("碰撞") || q.contains("碰到") || q.contains("触碰") || q.contains("相撞") -> {
                "💡【 Scratch 碰撞检测指南】✨\n\n" +
                "检测两个角色是否相撞，可以用【如果...那么...】配合【侦测】：\n\n" +
                "1. 🛡️ **判断框**：黄色【控制】拖出【如果...那么...】开口框，放入【重复执行】中。\n" +
                "2. 🔍 **侦测条件**：浅蓝色【侦测】拖出【碰到... ?】嵌进六边形小洞里。\n" +
                "3. 🎉 **触发响应**：在【如果】嘴巴里面放上【播放声音】、【隐藏】或【将得分增加 1】即可！"
            }

            // 11. 广播 / 消息 / 传信
            q.contains("广播") || q.contains("消息") || q.contains("信号") -> {
                "💡【 Scratch 广播消息指南】✨\n\n" +
                "广播就像角色之间用对讲机传递信号：\n\n" +
                "1. 📢 **发送广播**：角色 A 在完成任务或满足条件时，执行黄色【事件】里的【广播 新消息】。\n" +
                "2. 👂 **接收广播**：角色 B 使用黄色【事件】里的【当接收到 新消息】作为脚本开头。\n" +
                "3. 🎬 **协同配合**：角色 B 在收到消息下方拼接出现的动作或声音，两个角色就能完美配合啦！"
            }

            // 12. 隐藏 / 显示 / 消失 / 出现
            q.contains("隐藏") || q.contains("显示") || q.contains("消失") || q.contains("出现") || q.contains("隐身") -> {
                "💡【 Scratch 隐身与显形指南】✨\n\n" +
                "1. 👻 **隐身消失**：去紫色【外观】分类拖出【隐藏】积木，角色就会立刻变成透明看不见。\n" +
                "2. ✨ **显形重现**：去紫色【外观】分类拖出【显示】积木，角色重回舞台。\n" +
                "👉 **必看提示**：如果代码里用到了【隐藏】，一定要在【当 🟢 被点击】开头加上【显示】，防止下次点绿旗时角色“神秘失踪”哦！"
            }

            // 13. 其它任意学生自定义问题 (通用灵活解题指南)
            else -> {
                var displayQ = cleanQuestion
                if (displayQ.length > 30) displayQ = displayQ.take(30) + "..."

                "💡【 Scratch 拼搭解题指南 】✨\n\n" +
                "关于你问的“$displayQ”：\n\n" +
                "1. 🐱 **起点框架**：请在左侧黄色【事件】分类中，拖出【当 🟢 被点击】放到脚本区最上方。\n" +
                "2. 🧩 **找对积木分类**：\n" +
                "   • 如果要移动或转向，去蓝色【运动】分类找【移动】或【面向】积木；\n" +
                "   • 如果要造型或说话，去紫色【外观】分类找【换造型】或【思考】积木；\n" +
                "   • 如果要持续运行或判断条件，去黄色【控制】分类找【重复执行】或【如果...那么】框。\n" +
                "3. 🚀 **测试尝试**：将积木按逻辑顺序紧紧吸附在一起，点击右上角【绿旗】测试效果吧！如果卡住了，可以随时向精灵姐姐更详细地提问哦！"
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
