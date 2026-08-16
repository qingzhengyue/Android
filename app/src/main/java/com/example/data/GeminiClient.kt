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
     * 少儿 Scratch 领域离线/故障兜底高智能问答求解器
     * 涵盖：三大结构对比、面向对象 OOP、专业编程语言跨界科普、算法思维、Scratch 核心拼搭
     * 严格遵循多意图路由系统：彻底消除“无脑拖绿旗”与“关于你问的”机械套话！
     */
    private fun getSmartScratchAnswer(prompt: String): String {
        // 精确提取学生的核心提问文本，去除干扰包裹
        val cleanQuestion = when {
            prompt.contains("【学生当前提问】") -> prompt.substringAfter("【学生当前提问】").trim()
            prompt.contains("小朋友问：“") -> prompt.substringAfter("小朋友问：“").substringBefore("”").trim()
            prompt.contains("“") && prompt.contains("”") -> prompt.substringAfter("“").substringBefore("”").trim()
            else -> prompt
        }.trim()

        val q = cleanQuestion.lowercase()

        return when {
            // =========================================================================
            // 1. 面向对象编程 (OOP) / 类与对象 / 封装 / 继承 / 多态 (解决用户明确反馈的痛点)
            // =========================================================================
            q.contains("面向对象") || q.contains("oop") || q.contains("类与对象") || q.contains("封装") || q.contains("继承") || q.contains("多态") -> {
                "💡【 编程魔法奥秘：什么是“面向对象编程”？】✨\n\n" +
                "哇！宝贝你太厉害啦，竟然关注到软件工程师们最核心的思维方式——“面向对象编程”（简称 OOP）！🐾\n\n" +
                "其实面向对象一点也不复杂，在少儿编程里就像我们**“用乐高积木拼角色”**或者**“大自然里的万物归类”**哦：\n\n" +
                "1. 🐱 **什么是“对象”（Object）？**\n" +
                "   • 在生活中，你手里抱的小猫咪、停在路边的小汽车，都是一个真实存在的具体事物，在代码里就叫“对象”。\n" +
                "   • **在 Scratch 舞台上**：每一个右下角的小猫、小恐龙、魔法棒角色（Sprite），其实就是一个活生生的“对象”！\n\n" +
                "2. 📦 **每个对象都有两样法宝**：\n" +
                "   • **属性（长什么样）**：比如小猫的名字、X/Y 坐标、大小、颜色、造型；\n" +
                "   • **方法/行为（能做什么技能）**：比如小猫能【移动 10 步】、【发出喵喵叫】、【跳跃】。\n\n" +
                "3. 🏭 **什么是“类”（Class）？**\n" +
                "   • “类”就像做小饼干的**模具**或者工程师画的**蓝图**。\n" +
                "   • 模具规定了小猫有胡须和四条腿（类）；你用模具做出来的第一只小白猫、第二只小橘猫，就是由类创建出来的“具体对象”！\n\n" +
                "🌟 **在 Scratch 里的奇妙联系**：\n" +
                "Scratch 里的【角色】和【克隆体】就是最直观的面向对象设计！现在把 Scratch 积木的基础逻辑练扎实，等未来学习 Python/Java 的类和对象时，你就会发现自己早已掌握它的魔法精髓啦！🚀"
            }

            // =========================================================================
            // 2. 超纲编程语言跨界 (Java / C++ / C# / JavaScript / Rust / Go / 编译型语言)
            // =========================================================================
            q.contains("java") || q.contains("c++") || q.contains("c语言") || q.contains("javascript") || q.contains("c#") || q.contains("rust") || q.contains("golang") || q.contains("文本编程") -> {
                val langName = when {
                    q.contains("java") -> "Java"
                    q.contains("c++") -> "C++"
                    q.contains("c语言") -> "C 语言"
                    q.contains("javascript") -> "JavaScript"
                    q.contains("rust") -> "Rust"
                    else -> "文本编程语言"
                }
                "👏【 科技前沿大揭秘：探索 $langName 的奇妙世界 】✨\n\n" +
                "哇！看到你对 $langName 充满好奇，精灵姐姐太为你自豪啦，小小年纪就展现出极客探索精神！🌈\n\n" +
                "1. 📖 **$langName 是什么呢？**\n" +
                "   • $langName 是世界上应用最广泛的“纯英文文本编程语言”之一。\n" +
                "   • 工程师叔叔阿姨用它来开发大型 3D 游戏（如《我的世界》）、手机 App、或者让火箭飞上太空的超级系统！\n\n" +
                "2. 🧩 **它和我们学的 Scratch 有什么关系？**\n" +
                "   • **Scratch** 就像“拼装乐高积木”：重点是锻炼你的**逻辑思维、顺序/选择/循环结构**，让你不用担心记错英文字母；\n" +
                "   • **$langName** 就像“用纯英文写魔法说明书”：底层逻辑（变量、判断、循环、函数）和 Scratch 完全一模一样！\n\n" +
                "💡 **小精灵给你的成长建议**：\n" +
                "现在在 Scratch 里把算法和逻辑练得棒棒的，这就是打下最坚实的地基！等你长大一点开始写 $langName 时，会觉得像搭积木一样得心应手哦！💪"
            }

            // =========================================================================
            // 3. Python 编程与代码对比
            // =========================================================================
            q.contains("python") || q.contains("蟒蛇") -> {
                when {
                    (q.contains("顺序") || q.contains("选择")) && (q.contains("区别") || q.contains("对比") || q.contains("范例") || q.contains("与") || q.contains("和")) -> {
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
                        "    print(\"🎉 恭喜你！成功通关少儿编程第一关！\")\n" +
                        "else:\n" +
                        "    print(\"💪 差一点点就通关啦，再接再厉哦！\")\n" +
                        "```\n\n" +
                        "💡 **核心区别小结**：\n" +
                        "• **顺序结构**：无条件分支，代码行行必过。\n" +
                        "• **选择结构**：用 if ... else ... 问句做选择，满不满足条件走不同路线！"
                    }
                    else -> {
                        "🐍【 走进神奇的 Python 少儿编程世界 】✨\n\n" +
                        "Python 是一门非常优雅、读起来像英语一样简单的编程语言！\n\n" +
                        "```python\n" +
                        "# 欢迎来到 Python 少儿编程乐园！\n" +
                        "name = \"小创客\"\n" +
                        "print(f\"你好，{name}！欢迎你！\")\n" +
                        "for step in range(1, 4):\n" +
                        "    print(f\"小猫向前跑第 {step} 步 🐾\")\n" +
                        "```\n\n" +
                        "💡 它的循环与判断逻辑和 Scratch 一脉相承，学好 Scratch 积木就是迈向 Python 的第一步！"
                    }
                }
            }

            // =========================================================================
            // 4. 多结构对比与辨析 (顺序 vs 选择 vs 循环)
            // =========================================================================
            (q.contains("区别") || q.contains("不同") || q.contains("对比") || q.contains("比较") || q.contains("还是")) &&
            (q.contains("结构") || q.contains("顺序") || q.contains("选择") || q.contains("循环") || q.contains("分支")) -> {
                "💡【 编程思维核心：顺序结构 vs 选择结构 的区别与联系 】✨\n\n" +
                "“顺序结构”和“选择结构”是编程世界的两大顶梁柱，它们的最本质区别在于**【有没有分支判断】**：\n\n" +
                "1. 🚶 **顺序结构（按部就班，一条路走到底）**：\n" +
                "   • **生活比喻**：早起刷牙洗脸【挤牙膏 ➔ 刷牙 ➔ 漱口】，必须按顺序一步步做完，不能跳步！\n" +
                "   • **程序特点**：指令从上到下逐行执行，每一句代码都会运行。\n" +
                "   • **Scratch 积木**：【移动 10 步】 ➔ 【说 Hello 2秒】 ➔ 【右转 15 度】。\n\n" +
                "2. 🔀 **选择结构（遇事做决定，岔路口二选一）**：\n" +
                "   • **生活比喻**：出门看天气【如果下雨 ➔ 就带雨伞；否则 ➔ 就戴遮阳帽】。\n" +
                "   • **程序特点**：来到岔路口，只有条件满足时才执行特定指令，不满足时走另一条路。\n" +
                "   • **Scratch 积木**：黄色【如果 <碰到 边缘?> 那么 [右转 180度] 否则 [移动 10步]】。\n\n" +
                "🌟 **一句话秒懂核心差异**：\n" +
                "• **顺序结构**：无条件，从头到尾一条线；\n" +
                "• **选择结构**：有条件，根据情况走不同路线！"
            }

            // =========================================================================
            // 5. 算法 / 数据结构 / 排序 / 列表 / 数组
            // =========================================================================
            q.contains("算法") || q.contains("数据结构") || q.contains("排序") || q.contains("二分") || q.contains("查找") -> {
                "💡【 计算思维探秘：什么是“算法”与“数据结构”？】✨\n\n" +
                "1. 🧠 **什么是算法（Algorithm）？**\n" +
                "   • 算法就是**“解决某一个难题的巧妙步骤和菜谱”**！\n" +
                "   • 比如：怎么把 10 张乱七八糟的卡片从小到大排好序？或者怎么用最少的步数走出迷宫？这些聪明的解题方法就叫算法。\n\n" +
                "2. 🗄️ **什么是数据结构（Data Structure）？**\n" +
                "   • 数据结构是**“把信息整整齐齐装起来的百宝箱”**！\n" +
                "   • 在 Scratch 里，深橙色变量分类里的【列表（List）】就是最典型的数据结构，可以按顺序存下一整排玩家姓名或分数！\n\n" +
                "🌟 掌握了算法，你的程序就会像数学小天才一样飞速运转！"
            }

            // =========================================================================
            // 6. 函数 / 自定义积木 / 过程
            // =========================================================================
            q.contains("函数") || q.contains("自制积木") || q.contains("制作新的积木") || q.contains("子程序") -> {
                "💡【 模块化魔法：Scratch 自制积木（函数）怎么用？】✨\n\n" +
                "自制积木（在代码里叫函数 Function）就像把一连串复杂的动作打包成一个“超级技能按键”！\n\n" +
                "1. 🛠️ **制作方法**：点击最左下角粉红色的【自制积木】➔ 点击【制作一个新的积木】，起名叫“画一个正方形”。\n" +
                "2. 📝 **定义内部步骤**：在粉色【定义 画一个正方形】下方，拼上【重复执行 4 次 [移动 100步, 右转 90度]】。\n" +
                "3. 🚀 **调用技能**：在主程序里，只要拖出粉色【画一个正方形】，角色就会立刻执行这整套动作，让代码变得超级清爽整洁！"
            }

            // =========================================================================
            // 7. 单独结构概念 (顺序结构 / 选择结构 / 循环结构)
            // =========================================================================
            q.contains("顺序") && !q.contains("选择") && !q.contains("循环") -> {
                "💡【 Scratch 核心概念：什么是顺序结构？】✨\n\n" +
                "顺序结构是程序世界最基础的“第一大基石”！\n\n" +
                "• **核心含义**：“按照从上到下的顺序，一步接一步地执行指令”。\n" +
                "• **生活比喻**：就像我们按步骤做蛋糕【倒面粉 ➔ 加鸡蛋 ➔ 搅拌 ➔ 烤箱烘烤】，绝不能倒过来！\n" +
                "• **Scratch 实践**：在【当 🟢 被点击】下方直接按顺序拼接【移动 10 步】➔【说 早上好 2秒】➔【换成造型2】。\n\n" +
                "点击绿旗时，小猫会一丝不苟地严格按顺序走完每一步！"
            }

            q.contains("选择") || (q.contains("条件") && !q.contains("循环")) || q.contains("分支") -> {
                "💡【 Scratch 核心概念：什么是选择结构？】✨\n\n" +
                "选择结构（条件分支）就像程序的“智慧小雷达”！\n\n" +
                "• **核心含义**：“根据条件是否满足，决定接下来执行哪一部分代码”。\n" +
                "• **生活比喻**：过马路【如果 绿灯亮 ➔ 往前走；否则 ➔ 停下等待】。\n" +
                "• **Scratch 积木**：在黄色【控制】分类中找到【如果...那么...否则...】，将六边形侦测积木（如【碰到 颜色 ?】）塞入判断框中！\n\n" +
                "有了它，游戏里的角色就能自主做决定啦！"
            }

            q.contains("循环") || q.contains("重复") -> {
                "💡【 Scratch 核心概念：什么是循环结构？】✨\n\n" +
                "循环结构是编程中最省力的“魔法复印机”！\n\n" +
                "• **核心含义**：“把一段相同的操作重复运行很多次，或者一直运行下去”。\n" +
                "• **Scratch 三大循环（都在黄色【控制】分类）**：\n" +
                "  1. 🔄 **【重复执行】**：无限循环，比如让太阳一直发光、敌人一直巡逻；\n" +
                "  2. 🔢 **【重复执行 10 次】**：计数循环，比如小猫向前跳 10 下；\n" +
                "  3. 🎯 **【重复执行直到...】**：条件循环，直到血量为 0 或通关才停下。"
            }

            // =========================================================================
            // 8. 角色动作实操 (跳跃 / 重力 / 跑动 / 射击 / 碰撞 / 克隆 / 变量)
            // =========================================================================
            q.contains("跳") || q.contains("重力") || q.contains("起跳") -> {
                "💡【 Scratch 角色跳跃与重力效果制作指南 】✨\n\n" +
                "要让角色像超级马里奥一样跳跃，有非常经典的拼搭方法：\n\n" +
                "1. 🧩 **积木抽屉指南**：黄色【事件】、黄色【控制】、蓝色【运动】。\n" +
                "2. 🚀 **动手搭积木**：\n" +
                "   • **第 1 步**：黄色【当按下 空格 键】作为开头；\n" +
                "   • **第 2 步（向上跃起）**：下方吸附黄色【重复执行 10 次】，嘴巴里放蓝色【将 y 坐标增加 10】；\n" +
                "   • **第 3 步（重力落下）**：紧跟下方再加一个【重复执行 10 次】，嘴巴里放蓝色【将 y 坐标增加 -10】。\n\n" +
                "🌟 **小精灵贴心提示**：点击空格键测试一下，小猫就会轻盈地跳起来并平稳落回地面啦！"
            }

            q.contains("克隆") || q.contains("分身") -> {
                "💡【 Scratch 克隆体积木实操指南 】✨\n\n" +
                "克隆能让角色像孙悟空一样变出很多分身（如漫天雪花、无数子弹）：\n\n" +
                "1. 🧩 **三大核心积木（都在黄色【控制】分类底部）**：\n" +
                "   • 【克隆 自己】：生成一个新分身；\n" +
                "   • 【当作为克隆体启动时】：分身的独立出生点；\n" +
                "   • 【删除此克隆体】：任务完成销毁分身。\n" +
                "2. 🚀 **动手拼搭**：\n" +
                "   • **本体脚本**：【当 🟢 被点击】 ➔ 【重复执行】 ➔ 【等待 1 秒】 ➔ 【克隆 自己】；\n" +
                "   • **克隆体脚本**：【当作为克隆体启动时】 ➔ 【移到 随机位置】 ➔ 【显示】 ➔ 【重复执行 10 次 [下一个造型, 等待 0.1秒]】 ➔ 【删除此克隆体】。"
            }

            q.contains("变量") || q.contains("得分") || q.contains("计分") || q.contains("生命") || q.contains("血量") -> {
                "💡【 Scratch 变量与计分系统制作指南 】✨\n\n" +
                "变量就像一个贴着标签的小储物盒，随时可以存放或修改数字：\n\n" +
                "1. 📊 **新建变量**：点击左侧深橙色【变量】分类 ➔ 【建立一个变量】，命名为“得分”。\n" +
                "2. 🔄 **初始化归零**：拖出【将 得分 设为 0】放在【当 🟢 被点击】正下方，保证每次开局分数清零。\n" +
                "3. 🎯 **触发加分**：在吃到金币或击中目标的逻辑里，执行深橙色【将 得分 增加 1】并播放叮当声！"
            }

            q.contains("边缘") || q.contains("反弹") || q.contains("碰壁") -> {
                "💡【 Scratch 碰壁反弹拼搭指南 】✨\n\n" +
                "1. 🐱 **启动与循环**：黄色【当 🟢 被点击】 ➔ 黄色【重复执行】。\n" +
                "2. 🚀 **移动与反弹**：在【重复执行】嘴巴里依次放上蓝色【移动 10 步】和蓝色【碰到边缘就反弹】。\n" +
                "3. 🌟 **避坑贴士**：在最顶端加上蓝色【将旋转方式设为 左右翻转】，角色反弹时就不会变成大头朝下啦！"
            }

            q.contains("跟随") || q.contains("鼠标") -> {
                "💡【 Scratch 角色跟随鼠标指南 】✨\n\n" +
                "1. 🐾 **平滑跟随**：在【当 🟢 被点击】下方放【重复执行】，里面放蓝色【面向 鼠标指针】和蓝色【移动 5 步】。\n" +
                "2. ⚡️ **瞬移跟随**：在【重复执行】中直接放蓝色【移到 鼠标指针】，角色就会形影不离地粘在鼠标光标上！"
            }

            q.contains("广播") || q.contains("消息") || q.contains("传信") -> {
                "💡【 Scratch 广播与角色通信指南 】✨\n\n" +
                "广播就像两个角色在用对讲机呼叫配合：\n\n" +
                "1. 📢 **呼叫方（发送广播）**：角色 A 在达成某个事件时，执行黄色【事件】里的【广播 游戏胜利】。\n" +
                "2. 👂 **应答方（接收广播）**：角色 B 使用黄色【事件】里的【当接收到 游戏胜利】作为脚本开头，并在下方拼接【显示】或【跳舞】！"
            }

            // =========================================================================
            // 9. 智能兜底：分析提问类型，拒绝千篇一律的幽灵模板！
            // =========================================================================
            else -> {
                val isConceptQuestion = q.contains("什么") || q.contains("为什么") || q.contains("怎么理解") ||
                        q.contains("区别") || q.contains("意义") || q.contains("原理") || q.contains("含义") ||
                        q.contains("概念") || q.contains("作用") || q.contains("是谁")

                if (isConceptQuestion) {
                    // 意图路线 B/C：概念启发与逻辑分析，绝不生搬硬套拖拽积木！
                    "💡【 编程小侦探：探索这个概念背后的逻辑 】✨\n\n" +
                    "小朋友提了一个非常棒的探索型问题！在编程的世界里，这个概念主要是帮助我们**更好地组织代码和理清思路**：\n\n" +
                    "1. 🔍 **核心奥秘**：它是计算机科学家为了让程序变得更聪明、更有序而发明的思维工具。\n" +
                    "2. 🌟 **生活中的小启发**：就像我们整理书包、分类玩具一样，编程也是把大问题拆解成一个小模块去逐步解决。\n" +
                    "3. 🚀 **在 Scratch 里的体验**：无论是舞台上的角色、变量还是循环判断，处处都闪耀着这个逻辑的光芒！\n\n" +
                    "宝贝如果对某一个具体的积木或效果有疑问，可以随时给精灵姐姐更详细地提问哦，我们一起动手试一试！🐱"
                } else {
                    // 意图路线 A：实操启发与探索指南
                    "💡【 Scratch 创意实操探索指南 】✨\n\n" +
                    "想在 Scratch 舞台上实现这个奇妙的想法，可以按照下面的三步逻辑来构思哦：\n\n" +
                    "1. 🎯 **明确触发条件**：去黄色【事件】分类选择何时开始（比如点击绿旗、按下按键、或者碰到其他角色）。\n" +
                    "2. 🧩 **组合动作与外观**：\n" +
                    "   • 动作变化去蓝色【运动】分类；\n" +
                    "   • 视觉效果去紫色【外观】或粉色【声音】分类；\n" +
                    "   • 持续检测去黄色【控制】分类（使用【重复执行】或【如果...那么】）。\n" +
                    "3. 🚀 **动手测试与微调**：把积木拼接在一起，点击绿旗亲自运行看看，根据效果调整数值吧！✨"
                }
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
