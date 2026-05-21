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
                _classesList.value = it
            }
        }
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

            // 2. 根据玩法装配 Prompt 模板
            val code = currentDraftCode.value
            val prompt = when (funcType) {
                "语法纠错" -> "分析以下Scratch项目JSON代码，找出所有语法错误和积木拼接错误，给出修正方案和对应知识点讲解：$code"
                "创意引导" -> "我正在用Scratch做一个【${currentDraftName.value}】的主题作品，给我3个创意实现思路和分步实现步骤，不要生成完整代码：$code"
                "知识点讲解" -> "讲解以下Scratch项目代码中涉及的编程知识点、原理和核心积木的用法关系：$code"
                else -> "你是一个Scratch教师。请分析以下Scratch积木：$code"
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

    // --- 查看评测报告详情 ---
    fun loadReportForWork(workId: Int) {
        viewModelScope.launch {
            _activeReport.value = repository.getReportForWork(workId)
        }
    }

    // --- 静态获取 Scratch 练习模板代码 ---
    private fun getTemplateCode(id: Int): String {
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
