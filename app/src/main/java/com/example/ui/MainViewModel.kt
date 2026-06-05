package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val context = application.applicationContext

    // --- 用户状态 ---
    private val _isLoggedIn = MutableStateFlow(SharedPreferencesUtil.isLoggedIn(context))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserRole = MutableStateFlow(SharedPreferencesUtil.getRole(context))
    val currentUserRole: StateFlow<String?> = _currentUserRole.asStateFlow()

    private val _currentUserName = MutableStateFlow(SharedPreferencesUtil.getUserName(context))
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    private val _currentClassId = MutableStateFlow(SharedPreferencesUtil.getClassId(context))
    val currentClassId: StateFlow<Int> = _currentClassId.asStateFlow()

    private val _currentUserId = MutableStateFlow(SharedPreferencesUtil.getUserId(context))
    val currentUserId: StateFlow<Int> = _currentUserId.asStateFlow()

    private val _currentBtnLoading = MutableStateFlow(false)
    val currentBtnLoading: StateFlow<Boolean> = _currentBtnLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // --- 班级列表 ---
    private val _classesList = MutableStateFlow<List<ClassEntity>>(emptyList())
    val classesList: StateFlow<List<ClassEntity>> = _classesList.asStateFlow()

    // --- 当前编程草稿工作区状态 ---
    val currentDraftCode = MutableStateFlow(getTemplateCode(1)) // 默认加载猫咪模板
    val currentDraftName = MutableStateFlow("我的太空漫步草稿")
    val currentTaskId = MutableStateFlow<Int?>(null)
    val currentTaskName = MutableStateFlow<String?>(null)

    // --- 草稿列表 ---
    private val _draftsList = MutableStateFlow<List<ScratchDraft>>(emptyList())
    val draftsList: StateFlow<List<ScratchDraft>> = _draftsList.asStateFlow()

    // --- 任务列表 ---
    private val _tasksList = MutableStateFlow<List<LearningTask>>(emptyList())
    val tasksList: StateFlow<List<LearningTask>> = _tasksList.asStateFlow()

    // --- 提交作品及评测报告列表 ---
    private val _worksList = MutableStateFlow<List<ScratchWork>>(emptyList())
    val worksList: StateFlow<List<ScratchWork>> = _worksList.asStateFlow()

    // --- 教师端专用的全校/全班提交作品及学生列表 ---
    private val _allWorksList = MutableStateFlow<List<ScratchWork>>(emptyList())
    val allWorksList: StateFlow<List<ScratchWork>> = _allWorksList.asStateFlow()

    private val _studentsList = MutableStateFlow<List<Student>>(emptyList())
    val studentsList: StateFlow<List<Student>> = _studentsList.asStateFlow()

    // --- 选中的作品详情评测数据 ---
    private val _activeReport = MutableStateFlow<WorkAiReport?>(null)
    val activeReport: StateFlow<WorkAiReport?> = _activeReport.asStateFlow()

    // --- AI 助手与限制管理 ---
    private val _aiResult = MutableStateFlow<String?>(null)
    val aiResult: StateFlow<String?> = _aiResult.asStateFlow()

    private val _aiResultType = MutableStateFlow("") // 语法纠错、创意引导、知识点讲解
    val aiResultType: StateFlow<String> = _aiResultType.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiRecordHistory = MutableStateFlow<List<AiAssistRecord>>(emptyList())
    val aiRecordHistory: StateFlow<List<AiAssistRecord>> = _aiRecordHistory.asStateFlow()

    private val _aiDailyLimitReached = MutableStateFlow(false)
    val aiDailyLimitReached: StateFlow<Boolean> = _aiDailyLimitReached.asStateFlow()

    private val _aiClassConfig = MutableStateFlow<AiTeachingConfig?>(null)
    val aiClassConfig: StateFlow<AiTeachingConfig?> = _aiClassConfig.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDatabase()
            loadClasses()
            if (_isLoggedIn.value) {
                onUserLoggedIn()
            }
        }
    }

    private fun loadClasses() {
        viewModelScope.launch {
            repository.getAllClasses().collect {
                _classesList.value = sortClassesSmart(it)
            }
        }
    }

    private fun sortClassesSmart(list: List<ClassEntity>): List<ClassEntity> {
        val chineseToNumMap = mapOf(
            "一" to 1, "二" to 2, "两" to 2, "三" to 3, "四" to 4, "五" to 5,
            "六" to 6, "七" to 7, "八" to 8, "九" to 9, "十" to 10
        )

        fun parseChineseOrArabic(str: String): Int? {
            val clean = str.trim()
            val arabic = clean.toIntOrNull()
            if (arabic != null) return arabic
            if (chineseToNumMap.containsKey(clean)) {
                return chineseToNumMap[clean]
            }
            if (clean.length == 2) {
                val first = clean[0].toString()
                val second = clean[1].toString()
                if (first == "十") {
                    val sVal = chineseToNumMap[second] ?: 0
                    return 10 + sVal
                }
                if (second == "十") {
                    val fVal = chineseToNumMap[first] ?: 0
                    return fVal * 10
                }
            } else if (clean.length == 3) {
                val first = clean[0].toString()
                val second = clean[1].toString()
                val third = clean[2].toString()
                if (second == "十") {
                    val fVal = chineseToNumMap[first] ?: 0
                    val tVal = chineseToNumMap[third] ?: 0
                    return fVal * 10 + tVal
                }
            }
            return null
        }

        fun getGradeNum(classEntity: ClassEntity): Int {
            val gText = classEntity.grade
            if (gText.isNotBlank()) {
                val p1 = Regex("([一二三四五六七八九十1234567890]+)")
                val match = p1.find(gText)
                if (match != null) {
                    val parsed = parseChineseOrArabic(match.groupValues[1])
                    if (parsed != null) return parsed
                }
            }
            val cText = classEntity.className
            val p2 = Regex("([一二三四五六七八九十1234567890]+)\\s*(年级|级)")
            val match2 = p2.find(cText)
            if (match2 != null) {
                val parsed = parseChineseOrArabic(match2.groupValues[1])
                if (parsed != null) return parsed
            }
            return Int.MAX_VALUE
        }

        fun getClassNum(classEntity: ClassEntity): Int {
            val text = classEntity.className
            val p1 = Regex("([一二三四五六七八九十1234567890]+)\\s*班")
            val match = p1.find(text)
            if (match != null) {
                val parsed = parseChineseOrArabic(match.groupValues[1])
                if (parsed != null) return parsed
            }
            val p2 = Regex("班级\\s*([一二三四五六七八九十1234567890]+)")
            val match2 = p2.find(text)
            if (match2 != null) {
                val parsed = parseChineseOrArabic(match2.groupValues[1])
                if (parsed != null) return parsed
            }
            val re = Regex("[一二三四五六七八九十1234567890]+")
            val allMatches = re.findAll(text).mapNotNull { parseChineseOrArabic(it.value) }.toList()
            if (allMatches.size >= 2) {
                return allMatches[1]
            } else if (allMatches.size == 1) {
                return allMatches[0]
            }
            return Int.MAX_VALUE
        }

        return list.sortedWith(compareBy<ClassEntity> { classEntity ->
            getGradeNum(classEntity)
        }.thenBy { classEntity ->
            getClassNum(classEntity)
        })
    }

    private fun onUserLoggedIn() {
        val studentId = SharedPreferencesUtil.getUserId(context)
        val role = SharedPreferencesUtil.getRole(context)
        val classId = SharedPreferencesUtil.getClassId(context)

        _currentUserName.value = SharedPreferencesUtil.getUserName(context)
        _currentUserRole.value = role
        _currentClassId.value = classId
        _currentUserId.value = studentId

        if (role == "student") {
            // 获取本班任务
            viewModelScope.launch {
                repository.getTasksByClass(classId).collect {
                    _tasksList.value = it
                }
            }
            // 获取个人草稿
            viewModelScope.launch {
                repository.getDraftsByStudent(studentId).collect {
                    _draftsList.value = it
                }
            }
            // 获取提交作品
            viewModelScope.launch {
                repository.getWorksByStudent(studentId).collect {
                    _worksList.value = it
                }
            }
            // 获取 AI 助手记录
            viewModelScope.launch {
                repository.getAssistRecordsByStudent(studentId).collect {
                    _aiRecordHistory.value = it
                }
            }
            // 获取班级 AI 配置
            viewModelScope.launch {
                repository.getConfigByClassIdFlow(classId).collect {
                    _aiClassConfig.value = it
                }
            }
        } else if (role == "teacher") {
            // 教师端获取本账号发布的通配任务
            viewModelScope.launch {
                repository.getAllTasks().collect {
                    _tasksList.value = it
                }
            }
            // 教师端获取所有的作业提交(不分班级，解决班级挑选阻碍)
            viewModelScope.launch {
                repository.getAllWorksFlow().collect {
                    _allWorksList.value = it
                }
            }
            // 教师端获取所有注册的学生
            viewModelScope.launch {
                repository.getAllStudentsFlow().collect {
                    _studentsList.value = it
                }
            }
        }
    }

    // --- 用户登录/注册逻辑 ---
    fun studentLogin(studentNum: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _currentBtnLoading.value = true
            _authError.value = null
            val student = repository.getStudentByNumber(studentNum)
            if (student == null) {
                _authError.value = "没有找到该学号的学生，请确认或先注册！"
            } else if (student.password != pass) {
                _authError.value = "登录密码错误，请重新输入。"
            } else {
                SharedPreferencesUtil.saveLoginSession(
                    context = context,
                    userId = student.studentId,
                    role = "student",
                    userName = student.name,
                    classId = student.classId,
                    identifier = student.studentNumber
                )
                _isLoggedIn.value = true
                onUserLoggedIn()
                onSuccess()
            }
            _currentBtnLoading.value = false
        }
    }

    fun studentRegister(studentNum: String, name: String, pass: String, classId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _currentBtnLoading.value = true
            _authError.value = null
            val existing = repository.getStudentByNumber(studentNum)
            if (existing != null) {
                _authError.value = "该学号已被注册！请直接登录。"
            } else {
                val newId = repository.registerStudent(
                    Student(
                        studentNumber = studentNum,
                        name = name,
                        password = pass,
                        classId = classId
                    )
                ).toInt()
                SharedPreferencesUtil.saveLoginSession(
                    context = context,
                    userId = newId,
                    role = "student",
                    userName = name,
                    classId = classId,
                    identifier = studentNum
                )
                _isLoggedIn.value = true
                onUserLoggedIn()
                onSuccess()
            }
            _currentBtnLoading.value = false
        }
    }

    fun teacherRegister(workId: String, name: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _currentBtnLoading.value = true
            _authError.value = null
            val existing = repository.getTeacherByWorkId(workId)
            if (existing != null) {
                _authError.value = "该工号已被注册！请直接登录。"
            } else {
                val newId = repository.registerTeacher(
                    Teacher(
                        workId = workId,
                        name = name,
                        password = pass
                    )
                ).toInt()
                SharedPreferencesUtil.saveLoginSession(
                    context = context,
                    userId = newId,
                    role = "teacher",
                    userName = name,
                    identifier = workId
                )
                _isLoggedIn.value = true
                onUserLoggedIn()
                onSuccess()
            }
            _currentBtnLoading.value = false
        }
    }

    fun teacherLogin(workId: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _currentBtnLoading.value = true
            _authError.value = null
            val teacher = repository.getTeacherByWorkId(workId)
            if (teacher == null) {
                _authError.value = "未找到教师工号，请联系学校信息管理员。"
            } else if (teacher.password != pass) {
                _authError.value = "登录密码不正确。"
            } else {
                SharedPreferencesUtil.saveLoginSession(
                    context = context,
                    userId = teacher.teacherId,
                    role = "teacher",
                    userName = teacher.name,
                    identifier = teacher.workId
                )
                _isLoggedIn.value = true
                onUserLoggedIn()
                onSuccess()
            }
            _currentBtnLoading.value = false
        }
    }

    fun logout() {
        SharedPreferencesUtil.clearSession(context)
        _isLoggedIn.value = false
        _currentUserRole.value = null
        _currentUserName.value = ""
        _currentUserId.value = -1
        _currentClassId.value = 0
    }

    // --- 在线编程、草稿及提交 ---
    fun selectTemplate(id: Int) {
        currentDraftCode.value = getTemplateCode(id)
        currentDraftName.value = when (id) {
            1 -> "我的猫咪漫步草稿"
            2 -> "水果捕获游戏草稿"
            3 -> "神奇电子琴草稿"
            else -> "迷宫探险草稿"
        }
    }

    fun loadDraftToWorkspace(draft: ScratchDraft) {
        currentDraftCode.value = draft.blockCode
        currentDraftName.value = draft.draftName
        currentTaskId.value = draft.taskId
        viewModelScope.launch {
            draft.taskId?.let {
                val task = repository.getTaskById(it)
                currentTaskName.value = task?.taskName
            } ?: run {
                currentTaskName.value = "自由创作"
            }
        }
    }

    fun loadWorkToWorkspace(work: ScratchWork) {
        currentDraftCode.value = work.workCode
        currentDraftName.value = "${work.workName} (载入版本)"
        currentTaskId.value = if (work.taskId == 0) null else work.taskId
        viewModelScope.launch {
            if (work.taskId != 0) {
                val task = repository.getTaskById(work.taskId)
                currentTaskName.value = task?.taskName
            } else {
                currentTaskName.value = "自由创作"
            }
        }
    }

    fun clearWorkspaceToNew() {
        currentDraftCode.value = "{ " +
                "\"targets\": [{ \"isStage\": false, \"name\": \"角色1\", \"blocks\": {} }] " +
                "}"
        currentDraftName.value = "全新的 Scratch 创意草稿"
        currentTaskId.value = null
        currentTaskName.value = "自由创作"
    }

    fun saveDraftToDb(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val studentId = _currentUserId.value
            if (studentId == -1) return@launch

            val draft = ScratchDraft(
                draftName = currentDraftName.value,
                blockCode = currentDraftCode.value,
                studentId = studentId,
                taskId = currentTaskId.value,
                lastModifiedTime = System.currentTimeMillis()
            )
            val rows = repository.saveDraft(draft)
            if (rows > 0) {
                onResult("草稿【${currentDraftName.value}】已安全保存至本地！")
            } else {
                onResult("保存草稿失败，请稍后重试。")
            }
        }
    }

    fun submitWorkAndAiReport(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val studentId = _currentUserId.value
            val classId = _currentClassId.value
            val taskId = currentTaskId.value ?: 0
            if (studentId == -1) {
                onResult("当前会话已过期，请重新登录！")
                return@launch
            }

            _aiLoading.value = true
            val work = ScratchWork(
                workName = currentDraftName.value,
                workCode = currentDraftCode.value,
                studentId = studentId,
                classId = classId,
                taskId = taskId,
                submitCount = 1,
                reviewStatus = "已评测"
            )

            try {
                val report = repository.submitWorkAndEvaluate(work)
                onResult("恭喜！作品【${work.workName}】提交并AI自动评测成功！获得：${report.averageScore} 分！")
            } catch (e: Exception) {
                onResult("提交中网络或評測异常：${e.message}")
            } finally {
                _aiLoading.value = false
            }
        }
    }

    // --- AI 实时辅助功能 ---
    fun callAiAssistant(funcType: String) {
        viewModelScope.launch {
            val studentId = _currentUserId.value
            val classId = _currentClassId.value
            if (studentId == -1) return@launch

            _aiLoading.value = true
            _aiResult.value = null
            _aiResultType.value = funcType

            // 1. 验证调用额度限制
            val countOk = repository.checkDailyAssistOk(studentId, classId)
            if (!countOk) {
                _aiDailyLimitReached.value = true
                _aiResult.value = "【调用超额】你今天调用 AI 实时辅助的资助限额已经用完啦！请向王老师申请解除上限，或者明天再来向 AI 姐姐提问哦！"
                _aiLoading.value = false
                return@launch
            }
            _aiDailyLimitReached.value = false

            // Check if feature is disabled by teacher config JSON
            val classDesc = SharedPreferencesUtil.getClassDescription(context, classId)
            var level = "三年级"
            var style = "趣味活泼"
            if (classDesc.trim().startsWith("{") && classDesc.trim().endsWith("}")) {
                try {
                    val json = org.json.JSONObject(classDesc)
                    val grammarCorrect = json.optBoolean("grammarCorrect", true)
                    val creativeGuide = json.optBoolean("creativeGuide", true)
                    val knowledgeExplain = json.optBoolean("knowledgeExplain", true)
                    val codeGenerate = json.optBoolean("codeGenerate", false)
                    level = json.optString("level", "三年级")
                    style = json.optString("style", "趣味活泼")

                    if (funcType == "语法纠错" && !grammarCorrect) {
                        _aiResult.value = "【老师限制了该功能】王老师现在已在班级参数中关闭了「语法纠错」功能噢。去尝试自己调试解决或者询问老师吧！"
                        _aiLoading.value = false
                        return@launch
                    }
                    if (funcType == "创意引导" && !creativeGuide) {
                        _aiResult.value = "【老师限制了该功能】王老师现在已在班级配置中关闭了「创意引导」功能噢。"
                        _aiLoading.value = false
                        return@launch
                    }
                    if ((funcType == "知识点讲解" || funcType == "考点讲解") && !knowledgeExplain) {
                        _aiResult.value = "【老师限制了该功能】王老师现在已在班级配置中关闭了「知识点讲解/考点讲解」功能噢。"
                        _aiLoading.value = false
                        return@launch
                    }
                    if ((funcType == "代码优化建议" || funcType == "完整代码生成") && !codeGenerate) {
                        _aiResult.value = "【老师限制了该功能】王老师现在已在班级配置中关闭了「完整代码生成/优化建议」功能噢。"
                        _aiLoading.value = false
                        return@launch
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. 根据玩法装配 Prompt 模板
            // 引入专为小学3-6年级订制的少儿认知增强式 AI Prompt 系统
            val styleInstruction = "【语调特色】：特别注意，你现在说话的辅导语调语气必须表现出【$style】的提示词特色风格。"
            val levelInstruction = "【理解深度限制】：特别注意，提问的学生是【$level】的学生。所以你在语言通俗度、比喻认知、逻辑步骤的深度上，必须100%符合【$level】阶段小学生的认知理解规律和实际能力。"

            val systemInstruction = """
                你是一个超级有爱心、说话极其温柔和蔼、充满童趣的少儿编程(Scratch 3.0)“编程精灵姐姐”。
                因为提问的小朋友只有 8-12 岁（小学3-6年级），你的回答必须100%符合他们的认知规律和心理特点：
                1. 【态度特别温柔、热情】：千万不能用成年人冰冷严肃的书面式文字！多用鼓励性话语（如“宝贝真棒！”、“这个创意妙极了！”、“来，精灵姐姐教你一个新魔法！”），并多用卡通和水果类的表情符号（✨, 🐱, 🚀, 💡, 🐾, 🎈, 🎮）。
                2. 【绝对要具体、提供一步步可跟着做的动作指南】：绝对不要讲抽象概念（诸如“在适当的生命周期回调中加入循环”、“保证边界校验完整”等）。必须具体到：第一步，在左边菜单里点击【什么颜色/什么分类】；第二步，在里面找到【什么名字的积木】并用手指拖拽出来；第三步，把它粘在【什么积木】的下面。
                3. 【一定要用有趣好玩的比喻解说术语】：
                   - 【变量】比作“用来收纳玩具的魔法彩色小盒子”。
                   - 【循环/重复执行】比作“小猫坐上了永远停不下来的欢快旋转木马”。
                   - 【条件判断/如果..那么】比作“天气预报小哨兵”，只在符合条件时才吹哨放行。
                   - 【坐标(X, Y)】比作“小猫站在一排横座位和一排纵座位交叉的方格教室里”。
                4. 【视觉分段排版】：句子短小，多用 ①、②、③ 标清动手步骤，重点积木和参数名字用中括号【】和粗体加亮以便小学生看清。
                5. $styleInstruction
                6. $levelInstruction
            """.trimIndent()

            val code = currentDraftCode.value
            val prompt = when (funcType) {
                "语法纠错" -> """
                    $systemInstruction
                    
                    我的 Scratch 积木代码是：$code
                    请用最有爱心、最具体的口吻帮我看看：
                    1. 先热烈夸奖我今天努力编程的尝试！
                    2. 帮我挑出有没有悬空无用的积木、或者积木没拼对顺序的“小逻辑冲突（小迷糊）”。
                    3. 给出特别可执行的、①②③步极简拼搭改错步骤，告诉我在哪个积木分区，找什么积木，换到哪一步拼好。
                """.trimIndent()
                
                "创意引导" -> """
                    $systemInstruction
                    
                    我的作品主题是【${currentDraftName.value}】。我现在的积木块是：$code
                    请给我 2个 简单、好玩并且小孩子很容易做出来的进阶创意点子，能让我的作品变得好玩10倍！
                    对于每个点子，请给出极度具体的、小学生一二三拼搭步骤指南，格式如下：
                    🎈 创意亮点：...
                    💡 好玩在什么地方：...
                    🐾 推荐拼插魔法步骤：
                    ① 点击左边【什么颜色分类】...
                    ② 拖出【什么名字积木】...
                    ③ 拼在【什么积木】下面...
                """.trimIndent()
                
                "知识点讲解" -> """
                    $systemInstruction
                    
                    请帮我讲讲我现在做出来的这个 Scratch 积木代码：$code
                    1. 用崇拜和赞美的语气告诉我这里面用到的最酷的“编程魔法知识点”是什么。
                    2. 面向小学生，用生动形象的比喻（例如玩具盒、旋转木马、小哨兵）解释这个知识点的妙用。
                    3. 告诉我这个魔法在别的小游戏（例如打地鼠、接水果等）里面可以怎么用来创造乐趣。
                """.trimIndent()
                
                "代码优化建议" -> """
                    $systemInstruction
                    
                    我的 Scratch 积木代码是：$code
                    请以极其温柔、富有童趣的口吻，帮我看看这个代码有没有可以精简或者优化的地方：
                    1. 热烈赞赏我当前的编写，指出写得棒的地方！
                    2. 告诉我有没有重复拼搭或者可以更巧妙用“重复执行”或“变量”来减少多余积木的思路。
                    3. 给出幽默而通俗的比喻，并说明一二三步具体的优化教程。
                """.trimIndent()
                
                else -> "$systemInstruction\n请分析以下Scratch积木代码并给出温暖有爱的具体拼搭指引：$code"
            }

            // 3. 异步获取 Gemini 响应并填充记录
            val aiResponse = GeminiClient.generateContent(prompt)
            _aiResult.value = aiResponse

            // 4. 写回本地调用日志供记录审计
            repository.saveAssistRecord(
                AiAssistRecord(
                    studentId = studentId,
                    classId = classId,
                    assistType = funcType,
                    requestContent = "对应草稿: ${currentDraftName.value}",
                    aiResult = aiResponse,
                    draftId = null
                )
            )
            _aiLoading.value = false
        }
    }

    fun callAiCustomQuestion(question: String, onResponse: (String) -> Unit) {
        viewModelScope.launch {
            val studentId = _currentUserId.value
            val classId = _currentClassId.value
            if (studentId == -1) return@launch

            _aiLoading.value = true
            _aiDailyLimitReached.value = false

            // 1. 验证调用额度限制
            val countOk = repository.checkDailyAssistOk(studentId, classId)
            if (!countOk) {
                _aiDailyLimitReached.value = true
                onResponse("【调用超额】你今天调用 AI 实时辅助的资助限额已经用完啦！请向王老师申请解除上限，或者明天再来向 AI 姐姐提问哦！")
                _aiLoading.value = false
                return@launch
            }

            val code = currentDraftCode.value
            val classDesc = SharedPreferencesUtil.getClassDescription(context, classId)
            var level = "三年级"
            var style = "趣味活泼"
            if (classDesc.trim().startsWith("{") && classDesc.trim().endsWith("}")) {
                try {
                    val json = org.json.JSONObject(classDesc)
                    level = json.optString("level", "三年级")
                    style = json.optString("style", "趣味活泼")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val styleInstruction = "【语调特色】：特别注意，你现在说话的辅导语调语气必须表现出【$style】的提示词特色风格。"
            val levelInstruction = "【理解深度限制】：特别注意，提问的学生是【$level】的学生。所以你在语言通俗度、比喻认知、逻辑步骤的深度上，必须100%符合【$level】阶段小学生的认知理解规律和实际能力。"

            val systemInstruction = """
                你是一个超级有爱心、说话极其温柔和蔼、充满童趣的少儿编程(Scratch 3.0)“编程精灵姐姐”。
                因为提问的小朋友只有 8-12 岁（小学3-6年级），你的回答必须100%符合他们的认知规律和心理特点：
                1. 【态度特别温柔、热情】：使用鼓励性话语，多用卡通和水果类的表情符号（✨, 🐱, 🚀, 💡, 🐾, 🎈, 🎮）。
                2. 【绝对要具体、提供一步步可跟着做的动作指南】。
                例如：第一步，在左边菜单里点击【什么颜色/什么分类】；第二步，在里面找到【什么名字的积木】并用手指拖拽出来；第三步，把它贴在组件下方。
                3. $styleInstruction
                4. $levelInstruction
                现有 Scratch 代码如下：
                $code
            """.trimIndent()

            val prompt = "$systemInstruction\n\n小朋友问：“$question”"
            val response = GeminiClient.generateContent(prompt)
            onResponse(response)

            // 写回本地调用日志
            repository.saveAssistRecord(
                AiAssistRecord(
                    studentId = studentId,
                    classId = classId,
                    assistType = "在线对答",
                    requestContent = question,
                    aiResult = response,
                    draftId = null
                )
            )
            _aiLoading.value = false
        }
    }

    // --- 教师审查与修改打回重做 ---
    fun submitTeacherReview(workId: Int, status: String, score: Int, comment: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateWorkReview(workId, status, score, comment)
                onResult("作品评审完毕！状态设为【$status】，评分 $score 分。")
                // 刷新主页状态
                onUserLoggedIn()
            } catch (e: Exception) {
                onResult("评审提交异常：${e.message}")
            }
        }
    }

    // --- 教师端发布任务 ---
    fun publishNewTaskByTeacher(name: String, detail: String, grade: String, deadlineStr: String, classId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val teacherId = _currentUserId.value
            if (teacherId == -1) return@launch

            val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val deadlineTime = try {
                df.parse(deadlineStr)?.time ?: (System.currentTimeMillis() + 7 * 24 * 3600 * 1000L)
            } catch (e: Exception) {
                System.currentTimeMillis() + 7 * 24 * 3600 * 1000L
            }

            val task = LearningTask(
                taskName = name,
                taskDetail = detail,
                grade = grade,
                deadline = deadlineStr,
                deadlineTime = deadlineTime,
                teacherId = teacherId,
                classId = classId,
                status = "进行中"
            )
            val row = repository.publishTask(task)
            if (row > 0) {
                onResult("成功为班级发布学习任务：${name}！")
                // 刷一下
                onUserLoggedIn()
            } else {
                onResult("发布任务失败，请检查数据库配置。")
            }
        }
    }

    // --- 教师端创建新班级 & 配制默认 AI 安全等级 ---
    fun createNewClassByTeacher(className: String, grade: String, description: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val teacherId = _currentUserId.value
            if (teacherId == -1) return@launch

            // 1. 去重校验
            val exists = _classesList.value.any { it.grade == grade && it.className == className }
            if (exists) {
                onResult("该年级下已存在同名班级")
                return@launch
            }

            val classEntity = ClassEntity(
                className = className,
                grade = grade,
                teacherId = teacherId
            )
            val newClassId = repository.createClass(classEntity).toInt()
            if (newClassId > 0) {
                // 保存班级简述到 SharedPreferences
                SharedPreferencesUtil.saveClassDescription(context, newClassId, description)
                // 同时为新班级自动配制一套绿色防沉迷 AI 提示安全规范
                repository.saveConfig(
                    com.example.data.AiTeachingConfig(
                        classId = newClassId,
                        teacherId = teacherId,
                        aiHintLevel = "入门阶梯引导",
                        creativeGuideDailyLimit = 8,
                        codeGenerationLimit = 0 // 阻断抄袭模式
                    )
                )
                onResult("班级【${className}】创建成功，AI阶梯防护罩及防沉迷设定已就绪！")
                loadClasses()
                // 刷一下
                onUserLoggedIn()
            } else {
                onResult("创建班级失败，请确认名称是否冲突。")
            }
        }
    }

    // --- 教师端自动批量生成年级班级 (三年级一班至六班) & AI 安全等级初始化 (优化三) ---
    fun batchCreateClassesByTeacher(grade: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val teacherId = _currentUserId.value
            if (teacherId == -1) {
                onResult("当前会话已失效，请重新登录。")
                return@launch
            }

            val suffixList = listOf("一班", "二班", "三班", "四班", "五班", "六班")
            var successfullyCreatedCount = 0
            
            for (suffix in suffixList) {
                val fullClassName = "$grade$suffix"
                // Check deduplication
                val exists = _classesList.value.any { it.grade == grade && it.className == fullClassName }
                if (exists) continue

                val classEntity = ClassEntity(
                    className = fullClassName,
                    grade = grade,
                    teacherId = teacherId
                )
                val newClassId = repository.createClass(classEntity).toInt()
                if (newClassId > 0) {
                    SharedPreferencesUtil.saveClassDescription(context, newClassId, "自动化创建的 $fullClassName 班级空间")
                    // 同时为每个新班级自动配制一套绿色防沉迷 AI 提示安全规范
                    repository.saveConfig(
                        com.example.data.AiTeachingConfig(
                            classId = newClassId,
                            teacherId = teacherId,
                            aiHintLevel = "入门阶梯引导",
                            creativeGuideDailyLimit = 8,
                            codeGenerationLimit = 0 // 阻断抄袭模式
                        )
                    )
                    successfullyCreatedCount++
                }
            }
            
            if (successfullyCreatedCount > 0) {
                onResult("成功！已自动完成【$grade】一班至六班共 $successfullyCreatedCount 个班级的批量创建与 AI 防护罩设定！")
                loadClasses()
                onUserLoggedIn()
            } else {
                onResult("批量生成完成，跳过了已建立同名档的班级。")
            }
        }
    }

    fun deleteClassByTeacher(classId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteClass(classId)
                onResult("班级已成功删除，关联的学生及任务已一并移除。")
                loadClasses()
                onUserLoggedIn()
            } catch (e: Exception) {
                onResult("删除班级异常：${e.message}")
            }
        }
    }

    fun updateClassByTeacher(classId: Int, className: String, grade: String, description: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Check deduplication (excluding current classId)
                val exists = _classesList.value.any { it.classId != classId && it.className == className && it.grade == grade }
                if (exists) {
                    onResult("该年级下已存在同名班级")
                    return@launch
                }

                repository.updateClass(classId, className, grade)
                SharedPreferencesUtil.saveClassDescription(context, classId, description)
                
                // Also parse JSON parameters and update Room database's AiTeachingConfig
                try {
                    val existingConfig = repository.getConfigByClassId(classId)
                    val configId = existingConfig?.configId ?: 0
                    val teacherId = existingConfig?.teacherId ?: 0
                    
                    var level = "基础班"
                    var limitCount = 10
                    var codeGen = 0
                    if (description.trim().startsWith("{") && description.trim().endsWith("}")) {
                        try {
                            val json = org.json.JSONObject(description)
                            level = json.optString("level", "基础班")
                            limitCount = json.optInt("dailyLimit", 10)
                            codeGen = if (json.optBoolean("codeGenerate", false)) 1 else 0
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    val updatedConfig = com.example.data.AiTeachingConfig(
                        configId = configId,
                        classId = classId,
                        teacherId = teacherId,
                        aiHintLevel = level,
                        codeGenerationLimit = codeGen,
                        creativeGuideDailyLimit = limitCount
                    )
                    repository.saveConfig(updatedConfig)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                onResult("班级信息修改成功！")
                loadClasses()
                onUserLoggedIn()
            } catch (e: Exception) {
                onResult("修改班级异常：${e.message}")
            }
        }
    }

    fun registerStudentByTeacher(studentNumber: String, name: String, pass: String, classId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            if (studentNumber.isBlank() || name.isBlank() || pass.isBlank()) {
                onResult("各项输入不能为空！")
                return@launch
            }
            val existing = repository.getStudentByNumber(studentNumber)
            if (existing != null) {
                onResult("该学号已被占用！")
                return@launch
            }
            val student = Student(
                studentNumber = studentNumber,
                name = name,
                password = pass,
                classId = classId
            )
            val id = repository.registerStudent(student)
            if (id > 0) {
                onResult("学生【$name】添加成功！")
                onUserLoggedIn() // refresh list
            } else {
                onResult("添加失败，请重试")
            }
        }
    }

    fun batchImportStudentsByTeacher(namesStr: String, classId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            if (namesStr.isBlank()) {
                onResult("请输入学生明细名单")
                return@launch
            }
            val names = namesStr.split(Regex("[,，、\n]")).map { it.trim() }.filter { it.isNotBlank() }
            if (names.isEmpty()) {
                onResult("未能解析出学生名单")
                return@launch
            }
            var count = 0
            val prefix = "S${classId}"
            val randSuffix = (1000..9999).random()
            names.forEachIndexed { index, name ->
                val num = "$prefix${randSuffix + index}"
                val student = Student(
                    studentNumber = num,
                    name = name,
                    password = "123456",
                    classId = classId
                )
                val id = repository.registerStudent(student)
                if (id > 0) count++
            }
            onResult("成功批量导入 $count 名学生！学号前缀为 $prefix，默认密码 123456")
            onUserLoggedIn()
        }
    }

    fun getClassDescription(classId: Int): String {
        return SharedPreferencesUtil.getClassDescription(context, classId)
    }

    suspend fun getClassAiAssistCount(classId: Int): Int {
        return repository.getAiAssistCountByClass(classId)
    }

    // --- 查看评测报告详情 ---
    fun getReportForWorkFlow(workId: Int): Flow<WorkAiReport?> {
        return repository.getReportForWorkFlow(workId)
    }

    fun loadReportForWork(workId: Int) {
        viewModelScope.launch {
            _activeReport.value = repository.getReportForWork(workId)
        }
    }

    // --- 静态获取 Scratch 练习模板代码 ---
    fun getTemplateCode(id: Int): String {
        return when (id) {
            1 -> """{
  "targets": [
    {
      "isStage": false,
      "name": "猫咪漫步 (Sprite1)",
      "blocks": {
        "b1": { "opcode": "event_whenflagclicked", "next": "b2" },
        "b2": { "opcode": "control_forever", "inputs": { "SUBSTACK": ["b3"] } },
        "b3": { "opcode": "motion_movesteps", "inputs": { "STEPS": [4, "10"] }, "next": "b4" },
        "b4": { "opcode": "motion_ifonedgebounce", "next": "b5" },
        "b5": { "opcode": "motion_setrotationstyle", "fields": { "STYLE": ["左右翻转"] } }
      }
    }
  ]
}"""
            2 -> """{
  "targets": [
    { "isStage": true, "name": "核心舞台", "variables": { "v_score": ["得分", 0] } },
    {
      "isStage": false,
      "name": "接水果盘子 (Bowl)",
      "blocks": {
        "p1": { "opcode": "event_whenflagclicked", "next": "p2" },
        "p2": { "opcode": "control_forever", "inputs": { "SUBSTACK": ["p3"] } },
        "p3": { "opcode": "control_if", "inputs": { "CONDITION": ["p4"], "SUBSTACK": ["p5"] } },
        "p4": { "opcode": "sensing_keypressed", "fields": { "KEY_OPTION": ["右移键"] } },
        "p5": { "opcode": "motion_changexby", "inputs": { "DX": [4, "15"] } }
      }
    }
  ]
}"""
            3 -> """{
  "targets": [
    {
      "isStage": false,
      "name": "神奇太空电子琴 (Keyboard)",
      "blocks": {
        "k1": { "opcode": "event_whenkeypressed", "fields": { "KEY_OPTION": ["a键"] }, "next": "k2" },
        "k2": { "opcode": "sound_playuntildone", "inputs": { "SOUND_MENU": ["激光音速c调"] }, "next": "k3" },
        "k3": { "opcode": "looks_nextcostume" }
      }
    }
  ]
}"""
            else -> "{}"
        }
    }
}
