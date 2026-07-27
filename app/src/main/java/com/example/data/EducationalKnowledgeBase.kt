package com.example.data

/**
 * 教育专有知识库 (RAG - Retrieval-Augmented Generation)
 * 结合《义务教育信息科技课程标准》及 3-6 年级 Scratch 图形化编程认知规律，
 * 为 AI 助手提供严谨、符合教育规律的上下文增强指导。
 */
object EducationalKnowledgeBase {

    data class KnowledgeChunk(
        val category: String,
        val keywords: List<String>,
        val content: String,
        val pedagogicalAdvice: String
    )

    private val knowledgeBase = listOf(
        KnowledgeChunk(
            category = "顺序结构",
            keywords = listOf("顺序", "一步步", "执行顺序", "开始", "绿旗"),
            content = "《信息科技课标》阶段一要求：学生应理解计算机程序是按顺序一步步执行指令的集合。积木的上下拼搭决定了角色动作的先后顺序。",
            pedagogicalAdvice = "引导学生用语言口述角色动作的先后次序（如“先向右走10步，再说你好”），避免一口气拼搭过多无序积木。"
        ),
        KnowledgeChunk(
            category = "循环结构",
            keywords = listOf("重复", "循环", "一直", "计数", "嵌套"),
            content = " Scratch 中【重复执行】与【重复执行N次】是控制结构的核心。小学阶段易出现无休止死循环或漏写退出条件的问题。",
            pedagogicalAdvice = "提示学生区分‘有穷循环’与‘无穷循环’的适用场景（例如动画播放用重复执行，移动固定步数用重复N次），并检查循环内的等待间隔。"
        ),
        KnowledgeChunk(
            category = "分支选择",
            keywords = listOf("如果", "条件", "否则", "碰到", "按键"),
            content = "【如果...那么】与【如果...那么...否则】用于实现程序的判断决策。学生经常遗漏将条件判断放入【重复执行】中进行持续侦测。",
            pedagogicalAdvice = "重点检查条件侦测积木是否被包裹在【重复执行】内部，否则条件判断仅在绿旗点击的一瞬间生效一次。"
        ),
        KnowledgeChunk(
            category = "事件驱动与广播",
            keywords = listOf("广播", "消息", "接收", "当角色被点击", "事件"),
            content = "广播机制是 Scratch 多角色协同与分步剧情演进的关键手段。消息命名应当具体直观（如‘游戏开始’、‘关卡一胜利’）。",
            pedagogicalAdvice = "鼓励学生使用具名广播代替无名消息1，并提示‘广播并等待’与普通‘广播’在卡顿与同步上的差异。"
        ),
        KnowledgeChunk(
            category = "变量与状态管理",
            keywords = listOf("变量", "得分", "计数器", "改变", "设为"),
            content = "变量用于在程序运行期间存储和更新数值（如得分、生命值）。常见错误包括未在绿旗点击时初始化变量。",
            pedagogicalAdvice = "强调‘开局清零/复位’原则：凡是使用了变量，必须在【当绿旗被点击】下方立即添加【将变量设为初始值】。"
        ),
        KnowledgeChunk(
            category = "造型与动画表现",
            keywords = listOf("造型", "换成造型", "下一个造型", "等待", "动画"),
            content = "角色流畅动画依赖于逐帧切换造型。若切换造型之间没有【等待X秒】积木，造型闪烁速度过快将无法被人眼识别。",
            pedagogicalAdvice = "提示学生在【下一个造型】后加上 0.1 到 0.2 秒的适当等待时间，使动画看起来细腻流畅。"
        ),
        KnowledgeChunk(
            category = "克隆与并行计算",
            keywords = listOf("克隆", "当作为克隆体启动", "删除此克隆体", "多角色"),
            content = "克隆允许动态产生大量同类型角色（如雪花、子弹）。必须注意在克隆体完成使命后及时【删除此克隆体】，避免内存泄露与卡顿。",
            pedagogicalAdvice = "引导学生关注克隆体的生命周期，检查‘删除此克隆体’积木是否在消失事件触发后被正确调用。"
        )
    )

    /**
     * RAG 检索增强：根据学生提问或积木代码，提取相关性最高的Top-3教育知识片段，拼接到 AI 系统提示词中。
     */
    fun retrieveRelevantContext(query: String, blockJson: String = ""): String {
        val combinedInput = "$query $blockJson".lowercase()

        val matchedChunks = knowledgeBase.map { chunk ->
            var score = 0
            chunk.keywords.forEach { kw ->
                if (combinedInput.contains(kw.lowercase())) {
                    score += 2
                }
            }
            if (combinedInput.contains(chunk.category.lowercase())) {
                score += 3
            }
            Pair(chunk, score)
        }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }

        val selectedChunks = if (matchedChunks.isNotEmpty()) matchedChunks else knowledgeBase.take(2)

        return buildString {
            append("【教育专有知识库与课程标准指引（RAG 增强 context）】\n")
            selectedChunks.forEachIndexed { idx, chunk ->
                append("${idx + 1}. [知识领域: ${chunk.category}]\n")
                append("   - 课标要求: ${chunk.content}\n")
                append("   - 少儿认知教学策略: ${chunk.pedagogicalAdvice}\n")
            }
            append("请结合以上少儿教育认知规律与课标要求，用充满鼓励、幽默且浅显易懂的语言回答小学生。\n")
        }
    }
}
