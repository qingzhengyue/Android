package com.example.ui

import android.util.Log
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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

    private val _currentIdentifier = MutableStateFlow(SharedPreferencesUtil.getIdentifier(context))
    val currentIdentifier: StateFlow<String> = _currentIdentifier.asStateFlow()

    private val _currentClassId = MutableStateFlow(SharedPreferencesUtil.getClassId(context))
    val currentClassId: StateFlow<Int> = _currentClassId.asStateFlow()

    private val _currentUserId = MutableStateFlow(SharedPreferencesUtil.getUserId(context))
    val currentUserId: StateFlow<Int> = _currentUserId.asStateFlow()

    val currentStudentDetails = MutableStateFlow<Student?>(null)
    val currentStudentClass = MutableStateFlow<ClassEntity?>(null)

    private val _currentBtnLoading = MutableStateFlow(false)
    val currentBtnLoading: StateFlow<Boolean> = _currentBtnLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // --- 班级列表 ---
    private val _classesList = MutableStateFlow<List<ClassEntity>>(emptyList())
    val classesList: StateFlow<List<ClassEntity>> = _classesList.asStateFlow()

    // --- 当前编程草稿工作区状态 ---
    val currentDraftCode = MutableStateFlow(staticGetTemplateCode(1)) // 默认加载猫咪模板
    val currentDraftName = MutableStateFlow("我的太空漫步草稿")
    val currentTaskId = MutableStateFlow<Int?>(null)
    val currentTaskName = MutableStateFlow<String?>(null)
    val workspaceLoadEvent = MutableStateFlow<String?>(null)

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

    // --- Categorized Histories (Enhance 2) ---
    private val _aiRecordHistoryCorrect = MutableStateFlow<List<AiAssistRecord>>(emptyList())
    val aiRecordHistoryCorrect: StateFlow<List<AiAssistRecord>> = _aiRecordHistoryCorrect.asStateFlow()

    private val _aiRecordHistoryCreative = MutableStateFlow<List<AiAssistRecord>>(emptyList())
    val aiRecordHistoryCreative: StateFlow<List<AiAssistRecord>> = _aiRecordHistoryCreative.asStateFlow()

    private val _aiRecordHistoryExplain = MutableStateFlow<List<AiAssistRecord>>(emptyList())
    val aiRecordHistoryExplain: StateFlow<List<AiAssistRecord>> = _aiRecordHistoryExplain.asStateFlow()

    // --- AI Stability & Status Monitoring (Fix 1) ---
    private val _aiConsecutiveFailures = MutableStateFlow(0)
    val aiConsecutiveFailures: StateFlow<Int> = _aiConsecutiveFailures.asStateFlow()

    private val _aiServiceStatus = MutableStateFlow("服务正常")
    val aiServiceStatus: StateFlow<String> = _aiServiceStatus.asStateFlow()

    private val _aiDailyLimitReached = MutableStateFlow(false)
    val aiDailyLimitReached: StateFlow<Boolean> = _aiDailyLimitReached.asStateFlow()

    private val _aiClassConfig = MutableStateFlow<AiTeachingConfig?>(null)
    val aiClassConfig: StateFlow<AiTeachingConfig?> = _aiClassConfig.asStateFlow()

    private val _currentClass = MutableStateFlow<ClassEntity?>(null)
    val currentClass: StateFlow<ClassEntity?> = _currentClass.asStateFlow()

    private val _classConfigMap = MutableStateFlow<Map<String, Any>>(emptyMap())
    val classConfigMap: StateFlow<Map<String, Any>> = _classConfigMap.asStateFlow()

    private val _realTimeStateEnabled = MutableStateFlow(false)
    val realTimeStateEnabled: StateFlow<Boolean> = _realTimeStateEnabled.asStateFlow()

    // --- 教师端查看学生代码工作区状态 ---
    val teacherViewingWorkspace = MutableStateFlow(false)

    // --- 教师查看学生作品 .sb3 文件路径（跨屏幕传递，页面加载完成后自动注入） ---
    val teacherPendingSb3Path = MutableStateFlow<String?>(null)

    // --- 教师查看学生作品项目数据（通过 @JavascriptInterface 传递给 WebView，避免 base64 大字符串注入） ---
    var teacherProjectData: String? = null

    // --- Supabase 云端数据库连接状态 ---
    private val _supabaseStatus = MutableStateFlow("未检测")
    val supabaseStatus: StateFlow<String> = _supabaseStatus.asStateFlow()

    private val _supabaseTesting = MutableStateFlow(false)
    val supabaseTesting: StateFlow<Boolean> = _supabaseTesting.asStateFlow()

    // --- 本地数据同步到云端状态 ---
    private val _syncStatus = MutableStateFlow("未同步")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    fun syncAllDataToCloud() {
        viewModelScope.launch {
            _syncing.value = true
            _syncStatus.value = "同步中..."
            try {
                val result = repository.syncAllLocalDataToCloud()
                _syncStatus.value = result
            } catch (e: Exception) {
                _syncStatus.value = "同步异常: ${e.message}"
            } finally {
                _syncing.value = false
            }
        }
    }

    fun testSupabaseConnection() {
        viewModelScope.launch {
            _supabaseTesting.value = true
            _supabaseStatus.value = "检测中..."
            try {
                val result = repository.testSupabaseConnection()
                _supabaseStatus.value = result
            } catch (e: Exception) {
                _supabaseStatus.value = "检测异常: ${e.message}"
            } finally {
                _supabaseTesting.value = false
            }
        }
    }

    fun setRealTimeStateEnabled(enabled: Boolean) {
        _realTimeStateEnabled.value = enabled
    }

    // --- 智能精灵全局状态提升 (修复1-3) ---
    val showAiAssistSheet = MutableStateFlow(false)
    val aiActiveTab = MutableStateFlow("语法纠错")
    val dialogueHistoryList = MutableStateFlow<List<DialogueHistoryItem>>(
        listOf(
            DialogueHistoryItem(
                title = "【精灵姐姐】",
                question = "开始我们今天的编程冒险吧！",
                answer = "哈喽！我是你的智能精灵姐姐，今天想和我一起探索什么神奇的 Scratch 编程魔法呢？✨",
                timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            )
        )
    )

    fun parseConfigFromDescription(desc: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val trimmed = desc.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val json = org.json.JSONObject(trimmed)
                result["level"] = json.optString("level", "三年级")
                result["dailyLimit"] = json.optInt("dailyLimit", 10)
                result["grammarCorrect"] = json.optBoolean("grammarCorrect", true)
                result["creativeGuide"] = json.optBoolean("creativeGuide", true)
                result["knowledgeExplain"] = json.optBoolean("knowledgeExplain", true)
                result["codeGenerate"] = json.optBoolean("codeGenerate", false)
                result["style"] = json.optString("style", "趣味活泼")
                result["remark"] = json.optString("remark", "")
                return result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        result["level"] = "三年级"
        result["dailyLimit"] = 10
        result["grammarCorrect"] = true
        result["creativeGuide"] = true
        result["knowledgeExplain"] = true
        result["codeGenerate"] = false
        result["style"] = "趣味活泼"
        result["remark"] = ""
        return result
    }

    fun syncClassConfig() {
        val classId = _currentClassId.value
        if (classId > 0) {
            val classDesc = SharedPreferencesUtil.getClassDescription(context, classId)
            _classConfigMap.value = parseConfigFromDescription(classDesc)
        }
    }

    fun getStudentLearningHours(studentId: Int): Double {
        val draftCount = _draftsList.value.size
        val submissionCount = _worksList.value.size
        val aiCount = _aiRecordHistory.value.size
        
        // 5 mins per draft, 15 mins per submission, 3 mins per AI help call
        val totalMinutes = draftCount * 5.0 + submissionCount * 15.0 + aiCount * 3.0
        val hours = totalMinutes / 60.0
        return Math.round(hours * 10.0) / 10.0
    }

    init {
        viewModelScope.launch {
            repository.initializeDatabase()
            loadClasses()
            if (_isLoggedIn.value) {
                onUserLoggedIn()
            }
            // Start class config polling every 5 seconds to ensure instant sync
            while (true) {
                syncClassConfig()
                delay(5000L)
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
        _currentIdentifier.value = SharedPreferencesUtil.getIdentifier(context)

        if (role == "student") {
            viewModelScope.launch {
                _currentClass.value = repository.getClassById(classId)
                val student = repository.getStudentById(studentId)
                currentStudentDetails.value = student
                if (student != null) {
                    currentStudentClass.value = repository.getClassById(student.classId)
                }
            }
            syncClassConfig()
            // 获取本班任务
            viewModelScope.launch {
                repository.getTasksByClass(classId).collect { list ->
                    _tasksList.value = list.filter { it.status != "已撤销" }
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
            // 获取 AI 助手记录并同步到对答对话记录中
            viewModelScope.launch {
                repository.getAssistRecordsByStudent(studentId).collect { list ->
                    _aiRecordHistory.value = list
                    // 同步到在线对答历史中以供随时回溯
                    val mappedItems = list.map { record ->
                        DialogueHistoryItem(
                            id = record.callId.toString(),
                            title = "【${record.assistType}】",
                            question = record.requestContent,
                            answer = record.aiResult,
                            timestamp = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.callTime))
                        )
                    }
                    val defaultGreeting = DialogueHistoryItem(
                        title = "【精灵姐姐】",
                        question = "开始我们今天的编程冒险吧！",
                        answer = "哈喽！我是你的智能精灵姐姐，今天想和我一起探索什么神奇的 Scratch 编程魔法呢？✨",
                        timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    )
                    dialogueHistoryList.value = listOf(defaultGreeting) + mappedItems
                }
            }
            viewModelScope.launch {
                repository.getAssistRecordsByStudentAndType(studentId, 1).collect {
                    _aiRecordHistoryCorrect.value = it
                }
            }
            viewModelScope.launch {
                repository.getAssistRecordsByStudentAndType(studentId, 2).collect {
                    _aiRecordHistoryCreative.value = it
                }
            }
            viewModelScope.launch {
                repository.getAssistRecordsByStudentAndType(studentId, 3).collect {
                    _aiRecordHistoryExplain.value = it
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
        val cleanNum = studentNum.trim().uppercase()
        val cleanPass = pass.trim()
        viewModelScope.launch {
            _currentBtnLoading.value = true
            _authError.value = null
            val student = repository.getStudentByNumber(cleanNum)
            if (student == null) {
                _authError.value = "没有找到该学号的学生，请确认或先注册！"
            } else if (student.password != cleanPass) {
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
        val cleanNum = studentNum.trim().uppercase()
        val cleanName = name.trim()
        val cleanPass = pass.trim()
        viewModelScope.launch {
            _currentBtnLoading.value = true
            _authError.value = null
            val existing = repository.getStudentByNumber(cleanNum)
            if (existing != null) {
                _authError.value = "该学号已被注册！请直接登录。"
            } else {
                val newId = repository.registerStudent(
                    Student(
                        studentNumber = cleanNum,
                        name = cleanName,
                        password = cleanPass,
                        classId = classId
                    )
                ).toInt()
                SharedPreferencesUtil.saveLoginSession(
                    context = context,
                    userId = newId,
                    role = "student",
                    userName = cleanName,
                    classId = classId,
                    identifier = cleanNum
                )
                _isLoggedIn.value = true
                onUserLoggedIn()
                onSuccess()
            }
            _currentBtnLoading.value = false
        }
    }

    fun teacherRegister(workId: String, name: String, pass: String, onSuccess: () -> Unit) {
        val cleanId = workId.trim().uppercase()
        val cleanName = name.trim()
        val cleanPass = pass.trim()
        viewModelScope.launch {
            _currentBtnLoading.value = true
            _authError.value = null
            val existing = repository.getTeacherByWorkId(cleanId)
            if (existing != null) {
                _authError.value = "该工号已被注册！请直接登录。"
            } else {
                val newId = repository.registerTeacher(
                    Teacher(
                        workId = cleanId,
                        name = cleanName,
                        password = cleanPass
                    )
                ).toInt()
                SharedPreferencesUtil.saveLoginSession(
                    context = context,
                    userId = newId,
                    role = "teacher",
                    userName = cleanName,
                    identifier = cleanId
                )
                _isLoggedIn.value = true
                onUserLoggedIn()
                onSuccess()
            }
            _currentBtnLoading.value = false
        }
    }

    fun teacherLogin(workId: String, pass: String, onSuccess: () -> Unit) {
        val cleanId = workId.trim().uppercase()
        val cleanPass = pass.trim()
        viewModelScope.launch {
            _currentBtnLoading.value = true
            _authError.value = null
            val teacher = repository.getTeacherByWorkId(cleanId)
            if (teacher == null) {
                _authError.value = "未找到教师工号，请联系学校信息管理员。"
            } else if (teacher.password != cleanPass) {
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
        _currentIdentifier.value = ""
        currentStudentDetails.value = null
        currentStudentClass.value = null
    }

    // --- 在线编程、草稿及提交 ---
    fun selectTemplate(id: Int) {
        val code = getTemplateCode(id)
        currentDraftCode.value = code
        currentDraftName.value = when (id) {
            1 -> "我的猫咪漫步草稿"
            2 -> "水果捕获游戏草稿"
            3 -> "神奇电子琴草稿"
            else -> "迷宫探险草稿"
        }
        workspaceLoadEvent.value = code
    }

    fun loadDraftToWorkspace(draft: ScratchDraft) {
        currentDraftCode.value = draft.blockCode
        currentDraftName.value = draft.draftName
        currentTaskId.value = draft.taskId
        workspaceLoadEvent.value = draft.blockCode
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
        teacherPendingSb3Path.value = null
        currentDraftCode.value = work.workCode
        currentDraftName.value = "${work.workName} (载入版本)"
        currentTaskId.value = if (work.taskId == 0) null else work.taskId
        workspaceLoadEvent.value = work.workCode
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
        teacherPendingSb3Path.value = null
        val code = """{"targets":[{"isStage":true,"name":"Stage","variables":{},"lists":{},"broadcasts":{},"blocks":{},"comments":{},"currentCostume":0,"costumes":[{"name":"背景1","bitmapResolution":1,"dataFormat":"svg","assetId":"cd21584322f79459ecb5864133b44723","md5ext":"cd21584322f79459ecb5864133b44723.svg","rotationCenterX":240,"rotationCenterY":180}],"sounds":[],"volume":100,"layerOrder":0},{"isStage":false,"name":"角色1","variables":{},"lists":{},"broadcasts":{},"blocks":{},"comments":{},"currentCostume":0,"costumes":[{"name":"造型1","bitmapResolution":1,"dataFormat":"svg","assetId":"b7853f557e44241d288a7593e62c0d58","md5ext":"b7853f557e44241d288a7593e62c0d58.svg","rotationCenterX":48,"rotationCenterY":50}],"sounds":[],"volume":100,"visible":true,"x":0,"y":0,"size":100,"direction":90,"draggable":false,"rotationStyle":"all around","layerOrder":1}],"monitors":[],"extensions":[],"meta":{"semver":"3.0.0","vm":"0.2.0","agent":"Android"}}"""
        currentDraftCode.value = code
        currentDraftName.value = "全新的 Scratch 创意草稿"
        currentTaskId.value = null
        currentTaskName.value = "自由创作"
        workspaceLoadEvent.value = code
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
        android.util.Log.d("SupabaseDebug", "====== 🎯 提交作品按钮被成功触发了！======")
        viewModelScope.launch {
            val studentId = _currentUserId.value
            val classId = _currentClassId.value
            if (studentId == -1) {
                android.util.Log.e("SupabaseDebug", "错误：当前 studentId = -1，用户未登录！")
                onResult("错误：请先登录")
                return@launch
            }
            val tid = currentTaskId.value ?: 0
            if (tid != 0) {
                val matchedTask = tasksList.value.find { it.taskId == tid }
                if (matchedTask != null && matchedTask.isExpired()) {
                    onResult("⚠️ 提交失败：该任务已于【${matchedTask.deadline}】截止，老师已禁止补交作业。")
                    return@launch
                }
            }
            _currentBtnLoading.value = true
            try {
                val work = ScratchWork(
                    studentId = studentId,
                    classId = classId,
                    taskId = currentTaskId.value ?: 0,
                    submitCount = 1,
                    workName = currentDraftName.value,
                    workCode = currentDraftCode.value,
                    submitTime = System.currentTimeMillis(),
                    reviewStatus = "已评测"
                )
                val report = repository.submitWorkAndEvaluate(work)
                
                // ----- 加上这行强力调试代码 -----
                android.util.Log.d("SupabaseDebug", "开始执行 Supabase 上传逻辑，workId = ${report.workId}")
                // ------------------------------
                
                var uploadStatusTip = "" // 用来记录上传状态
                
                try {
                    val taskIdForMock = currentTaskId.value ?: 0
                    val matchedTaskForMock = tasksList.value.find { it.taskId == taskIdForMock }
                    val taskNameForMock = matchedTaskForMock?.taskName ?: ""
                    
                    val mockFile = com.example.data.MockWorkRepository.getMockSb3FileForTask(context, taskIdForMock.toLong(), taskNameForMock)
                    val localFile = java.io.File(context.filesDir, "student_${studentId}_project_${report.workId}.sb3")
                    
                    // Copy mock file to localFile for upload
                    mockFile.copyTo(localFile, overwrite = true)
                    
                    android.util.Log.d("SupabaseDebug", "本地 .sb3 文件生成成功，路径：${localFile.absolutePath}，大小：${localFile.length()}字节")
                    
                    com.example.data.SupabaseManager.uploadScratchProject(localFile, localFile.name)
                    
                    android.util.Log.d("SupabaseDebug", "SupabaseManager.uploadScratchProject 调用完成")
                    uploadStatusTip = "\n☁️ 云端同步成功！"
                } catch (e: Exception) {
                    val errorMsg = e.message ?: e.toString()
                    uploadStatusTip = "\n⚠️ 云端上传失败，原因：$errorMsg"
                    android.util.Log.e("SupabaseDebug", "Upload to Supabase failed with exception", e)
                }

                onResult("作品提报并评测成功！综合评分：${report.averageScore} 分。$uploadStatusTip")
                onUserLoggedIn() // refresh lists
            } catch (e: Exception) {
                e.printStackTrace()
                onResult("提交失败: ${e.message}")
            } finally {
                _currentBtnLoading.value = false
            }
        }
    }

    private var realTimeCheckJob: kotlinx.coroutines.Job? = null
    private var lastAiCallTime = 0L

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val activeNetwork = cm?.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (e: Exception) {
            true
        }
    }

    private suspend fun callGeminiWithTimeoutAndRetry(prompt: String): String {
        var lastException: Throwable? = null
        var currentDelay = 1000L
        for (attempt in 0..2) {
            try {
                return kotlinx.coroutines.withTimeout(15000L) {
                    GeminiClient.generateContent(prompt, isNetworkAvailable())
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                lastException = e
            } catch (e: Exception) {
                lastException = e
            }
            if (attempt < 2) {
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        if (lastException is kotlinx.coroutines.TimeoutCancellationException) {
            throw lastException
        } else {
            throw lastException ?: Exception("Unknown error")
        }
    }

    // --- AI 实时辅助功能 ---
    fun callAiAssistant(funcType: String, currentCodeInjected: String? = null, param: String = "") {
        if (_aiLoading.value) {
            Log.w("AIFlow", "[GUARD] AI调用正在进行中, 忽略重复调用: funcType=$funcType")
            return
        }
        Log.d("AIFlow", "[1/7] callAiAssistant 被调用: funcType=$funcType, hasCode=${currentCodeInjected != null}, codeLen=${currentCodeInjected?.length ?: 0}")
        if (funcType == "语法纠错") {
            realTimeCheckJob?.let { it.cancel() }
        }
        val job = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
            if (funcType == "语法纠错") {
                val now = System.currentTimeMillis()
                val gap = now - lastAiCallTime
                if (gap < 1000L) {
                    delay(1000L - gap)
                }
                lastAiCallTime = System.currentTimeMillis()
            }

            val studentId = _currentUserId.value
            val classId = _currentClassId.value
            Log.d("AIFlow", "[2/7] 用户状态: studentId=$studentId, classId=$classId")
            if (studentId == -1) {
                Log.e("AIFlow", "错误: studentId=-1, 用户未登录, 直接返回")
                return@launch
            }

            _aiLoading.value = true
            _aiResult.value = null
            _aiResultType.value = funcType
            Log.d("AIFlow", "[3/7] aiLoading已设为true, 开始检查每日限额...")

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
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "老师暂未开启此功能噢，先自己开动脑筋想一想吧！", android.widget.Toast.LENGTH_LONG).show()
                        }
                        _aiResult.value = "【老师限制了该功能】老师暂未开启此功能噢，先自己开动脑筋想一想吧！"
                        _aiLoading.value = false
                        return@launch
                    }
                    if (funcType == "创意引导" && !creativeGuide) {
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "老师暂未开启此功能噢，先自己开动脑筋想一想吧！", android.widget.Toast.LENGTH_LONG).show()
                        }
                        _aiResult.value = "【老师限制了该功能】老师暂未开启此功能噢，先自己开动脑筋想一想吧！"
                        _aiLoading.value = false
                        return@launch
                    }
                    if ((funcType == "知识点讲解" || funcType == "考点讲解") && !knowledgeExplain) {
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "老师暂未开启此功能噢，先自己开动脑筋想一想吧！", android.widget.Toast.LENGTH_LONG).show()
                        }
                        _aiResult.value = "【老师限制了该功能】老师暂未开启此功能噢，先自己开动脑筋想一想吧！"
                        _aiLoading.value = false
                        return@launch
                    }
                    if ((funcType == "代码优化建议" || funcType == "完整代码生成") && !codeGenerate) {
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "老师暂未开启此功能噢，先自己开动脑筋想一想吧！", android.widget.Toast.LENGTH_LONG).show()
                        }
                        _aiResult.value = "【老师限制了该功能】老师暂未开启此功能噢，先自己开动脑筋想一想吧！"
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
            val levelInstruction = "【理解深度限制】：特别注意，提问的学生是【$level】的学生。所以你在语言通俗度、比喻认知、逻辑步骤的深度上，必须100%符合【$level】阶段小学生的认知理解规律 and 实际能力。"

            val systemInstruction = """
                你是一个超级有爱心、说话极其温柔和蔼、充满童趣 of 少儿编程(Scratch 3.0)“编程精灵姐姐”。
                因为提问的小朋友只有 8-12 岁（小学3-6年级），你的回答必须100%符合 their 认知规律 and 心理特点：
                1. 【态度特别温柔、热情】：千万不能用成年人冰冷严肃的书面式文字！多用鼓励性话语（如“宝贝真棒！”、“这个创意妙极了！”、“来，精灵姐姐教你一个新魔法！”），并多用卡通和水果类的表情符号（✨, 🐱, 🚀, 💡, 🐾, 🎈, 🎮）。
                2. 【绝对要具体、提供一步步可跟着做的动作指南】：绝对不要讲抽象概念（诸如“在适当的生命周期回调中加入循环”、“保证边界校验完整”等）。必须具体到：第一步，在左边菜单里点击【什么颜色/什么分类】；第二步，在里面找到【什么名字的积木】并用手指拖拽出来；第三步，把它粘在【什么积木】的下面。
                3. 【一定要用有趣好玩的比喻解说术语】：
                   - 【变量】比作“用来收纳玩具的魔法彩色小盒子”。
                   - 【循环/重复执行】比作“小猫坐上了永远停不下来的欢快旋转木马”。
                   - 【条件判断/如果..那么】比作“天气预报小哨兵”，只在符合条件时才吹哨放行。
                   - 【坐标(X, Y)】比作“小猫站在一排横座位 and 一排纵座位交叉的方格教室里”。
                4. 【视觉分段排版】：句子短小，多用 ①、②、③ 标清动手步骤，重点积木 and 参数名字用中括号【】 and 粗体加亮以便小学生看清。
                5. $styleInstruction
                6. $levelInstruction
            """.trimIndent()

            val code = currentCodeInjected ?: currentDraftCode.value
            val prompt = when (funcType) {
                "语法纠错" -> """
                    $systemInstruction
                    
                    我的 Scratch 积木代码 JSON 是：$code
                    请帮我分析这份代码，找出其中的语法错误、逻辑冲突或没对齐没拼好的地方，并按以下标准格式给出诊断指导：
                    
                    【极其重要的事实核查规则】：
                    传入的代码是孩子实时编写的 Scratch 代码 JSON。请认真查验：如果代码中已经包含了 "event_whenflagclicked" 或包含 "whenflagclicked"（代表“当 🟢 被点击”事件积木），绝对不可以诬陷孩子说缺少【当 🟢 被点击】！相反，你要夸奖他已经正确放置了绿旗启动积木，然后再去检查里面的循环、条件、移动或积木连接逻辑！
                    
                    每个发现的问题，必须以两个核心标签起头输出：
                    【错误提示】: (说明在积木何处出现了什么原因 of 逻辑小迷糊/错误)
                    【修正建议】: (教导孩子应该如何拼搭、拖动、或者如何改好积木)
                """.trimIndent()
                
                "创意引导" -> """
                    $systemInstruction
                    
                    我想在这个作品的基础上进行一些好玩的创意延展。
                    我这次希望创作的主题是：【${param.ifBlank { "自由拓展与创意优化" }}】。
                    我的 Scratch 积木代码目前是：$code
                    请专门围绕【${param.ifBlank { "自由拓展与创意优化" }}】这个主题，结合我目前的代码，给我 2-3 个符合小学生认知的酷炫 Scratch 魔法创意！
                    每一个小魔法，必须先用一个有趣的名字包装，然后写出具体的积木拼法：
                    - 分步骤①、②、③说明在左侧什么分类里拖拽哪块积木，把它拼插在哪里，参数改成什么。
                    - 结尾加上一句鼓励，说明这个魔法在小游戏里能带来什么震撼效果。
                """.trimIndent()
                
                "代码优化建议" -> """
                    $systemInstruction
                                    
                    我的 Scratch 积木代码是：$code
                    请以极其温柔、富有童趣的口吻，帮我看看这个代码有没有可以精简或者优化的地方：
                    1. 热烈赞赏我当前的编写，指出写得棒的地方！
                    2. 告诉我有没有重复拼搭或者可以更巧妙用"重复执行"或者"变量"来减少多余积木的思路。
                    3. 给出幽默而通俗的比喻，并说明一二三步具体的优化教程。
                """.trimIndent()
                
                "知识点讲解", "考点讲解" -> """
                    $systemInstruction
                                    
                    我现在想学习 Scratch 编程的核心知识点/考点：【${param.ifBlank { "变量与广播" }}】。
                    我目前的 Scratch 积木代码是：$code
                    请专门围绕【${param.ifBlank { "变量与广播" }}】这个知识点，用最温柔、最通俗易懂的小学生比喻进行讲解：
                    1. 用童趣十足的比喻说明【${param.ifBlank { "变量与广播" }}】是什么（例如魔法小盒子、信鸽广播等）。
                    2. 结合我当前的代码进度，告诉我为什么要用到【${param.ifBlank { "变量与广播" }}】。
                    3. 分步骤①、②、③指导我怎么在左侧积木栏拖出对应的【${param.ifBlank { "变量与广播" }}】积木并拼搭到作品里！
                """.trimIndent()
                                
                else -> "$systemInstruction\n请分析以下Scratch积木代码并给出温暖有爱的具体拼搭指引：$code"
            }

            Log.d("AIFlow", "[3.5/7] Prompt装配完成: funcType=$funcType, prompt总长度=${prompt.length}字符, 代码部分长度=${code?.length ?: 0}")

            // 3. 异步获取 Gemini 响应并填充记录
            Log.d("AIFlow", "[4/7] 开始调用Gemini API (funcType=$funcType, prompt长度=${prompt.length})...")
            val aiResponse = try {
                // 安全超时: 整个API调用最多等45秒
                kotlinx.coroutines.withTimeout(45000L) {
                    callGeminiWithTimeoutAndRetry(prompt)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("AIFlow", "Gemini调用总超时(45s): ${e.message}")
                "【连接超时啦 ⏰】精灵姐姐刚才可能开小差去采花了，没有在规定时间内赶回来。别着急，我们可以【点击重试】或者重新发送一次哦！"
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.w("AIFlow", "Gemini调用被取消(协程取消): ${e.message}")
                throw e // 重新抛出CancellationException以让finally正常执行
            } catch (e: Exception) {
                Log.e("AIFlow", "Gemini调用异常: ${e.javaClass.simpleName}: ${e.message}", e)
                "【服务器忙碌中 ☁️】太空信号有点不稳定，精灵姐姐暂时没有收到你的魔法代码。别着急，让网络飞一会儿，咱们过 10 秒钟再点一下重试吧！"
            }
            Log.d("AIFlow", "[5/7] Gemini响应已收到, response长度=${aiResponse.length}")
            _aiResult.value = aiResponse

            val isFailed = aiResponse.startsWith("【连接超时") || aiResponse.startsWith("【服务器忙碌")

            if (isFailed) {
                _aiConsecutiveFailures.value += 1
                if (_aiConsecutiveFailures.value >= 3) {
                    _aiServiceStatus.value = "已降级 (连续3次请求失败，实时检测已自动关闭)"
                    _realTimeStateEnabled.value = false
                } else {
                    _aiServiceStatus.value = "服务异常 (重试中...)"
                }
            } else {
                _aiConsecutiveFailures.value = 0
                _aiServiceStatus.value = "服务正常"
            }

            // 4. 写回本地调用日志供记录审计
            val assistTypeIntVal = when (funcType) {
                "语法纠错" -> 1
                "创意引导" -> 2
                "知识点讲解", "考点讲解", "知识点" -> 3
                else -> 1
            }

            repository.saveAssistRecord(
                AiAssistRecord(
                    studentId = studentId,
                    classId = classId,
                    assistType = funcType,
                    assistTypeInt = assistTypeIntVal,
                    requestContent = if (funcType == "创意引导") "主题: ${currentDraftName.value.ifEmpty { "自由拓展" }}" else "对应草稿: ${currentDraftName.value}",
                    aiResult = aiResponse,
                    draftId = null
                )
            )
            Log.d("AIFlow", "[6/7] AI辅助记录已保存, 即将设置aiLoading=false")
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.w("AIFlow", "callAiAssistant 协程被取消: ${e.message}")
                throw e // 重新抛出以确保finally执行
            } catch (e: Exception) {
                Log.e("AIFlow", "callAiAssistant 发生未捕获异常: ${e.javaClass.simpleName}: ${e.message}", e)
                _aiResult.value = "【发生异常 ${e.javaClass.simpleName}】${e.message ?: "未知错误"}"
            } finally {
                _aiLoading.value = false
                Log.d("AIFlow", "[7/7] callAiAssistant 结束, aiLoading已复位为false")
            }
        }
        if (funcType == "语法纠错") {
            realTimeCheckJob = job
        }
    }

    fun callAiCustomQuestion(question: String, onResponse: (String) -> Unit) {
        Log.d("AIFlow", "callAiCustomQuestion 被调用: question长度=${question.length}")
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val studentId = _currentUserId.value
            val classId = _currentClassId.value
            if (studentId == -1) {
                Log.e("AIFlow", "callAiCustomQuestion: studentId=-1, 用户未登录")
                return@launch
            }

            _aiLoading.value = true
            _aiDailyLimitReached.value = false
            try {

            // 0. 安全过滤与输入验证
            val trimmed = question.trim()
            if (trimmed.isEmpty()) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "提问不能空空如也哦，快写点什么吧！", android.widget.Toast.LENGTH_LONG).show()
                }
                onResponse("提问不能空空如也哦，快写点什么吧！")
                _aiLoading.value = false
                return@launch
            }
            // 包含中文字符校验
            val hasChinese = trimmed.any { it in '\u4e00'..'\u9fa5' }
            if (!hasChinese) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "提问最好包含中文字符噢，请写几句通俗的中文吧！", android.widget.Toast.LENGTH_LONG).show()
                }
                onResponse("提问最好包含中文字符噢，请写几句通俗的中文吧！")
                _aiLoading.value = false
                return@launch
            }
            // 纯特殊字符过滤（必须含有字母、数字或汉字）
            val hasValidChar = trimmed.any { it.isLetterOrDigit() || it in '\u4e00'..'\u9fa5' }
            if (!hasValidChar) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "精灵姐姐看不懂奇怪的符号噢，请说一句正常的中文吧！", android.widget.Toast.LENGTH_LONG).show()
                }
                onResponse("精灵姐姐看不懂奇怪的符号噢，请说一句正常的中文吧！")
                _aiLoading.value = false
                return@launch
            }

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
                    val grammarCorrect = json.optBoolean("grammarCorrect", true)
                    if (!grammarCorrect) {
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "老师暂未开启此功能噢，先自己开动脑筋想一想吧！", android.widget.Toast.LENGTH_LONG).show()
                        }
                        onResponse("老师暂未开启此功能噢，先自己开动脑筋想一想吧！")
                        _aiLoading.value = false
                        return@launch
                    }
                    level = json.optString("level", "三年级")
                    style = json.optString("style", "趣味活泼")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val styleInstruction = "【语调特色】：特别注意，你现在说话的辅导语调语气必须表现出【$style】的提示词特色风格。"
            val levelInstruction = "【理解深度限制】：特别注意，提问的学生是【$level】的学生。所以你在语言通俗度、比喻认知、逻辑步骤的深度上，必须100%符合【$level】阶段小学生的认知理解规律和实际能力。"

            val systemInstruction = """
                你是一个超级有爱心、说话极其温柔和蔼、充满童趣 of 少儿编程(Scratch 3.0)“编程精灵姐姐”。
                因为提问的小朋友只有 8-12 岁（小学3-6年级），你的回答必须100%符合他们的认知规律和心理特点：
                1. 【态度特别温柔、热情】：使用鼓励性话语，多用卡通 and 水果类的表情符号（✨, 🐱, 🚀, 💡, 🐾, 🎈, 🎮）。
                2. 【绝对要具体、提供一步步可跟着做的动作指南】。
                例如：第一步，在左边菜单里点击【什么颜色/什么分类】；第二步，在里面找到【什么名字 of 积木】并用手指拖拽出来；第三步，把它贴在组件下方。
                3. $styleInstruction
                4. $levelInstruction
                现有 Scratch 代码如下:
                $code
            """.trimIndent()

            val prompt = "$systemInstruction\n\n小朋友问：“$question”"
            val response = try {
                kotlinx.coroutines.withTimeout(45000L) {
                    callGeminiWithTimeoutAndRetry(prompt)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                "【连接超时啦 ⏰】精灵姐姐刚才可能开小差去采花了，没有在规定时间内赶回来。别着急，我们可以【点击重试】或者重新发送一次哦！"
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                "【服务器忙碌中 ☁️】太空信号有点不稳定，精灵姐姐暂时没有收到你的魔法代码。别着急，让网络飞一会儿，咱们过 10 秒钟再点一下重试吧！"
            }
            onResponse(response)

            val isFailed = response.startsWith("【连接超时") || response.startsWith("【服务器忙碌")

            if (isFailed) {
                _aiConsecutiveFailures.value += 1
                if (_aiConsecutiveFailures.value >= 3) {
                    _aiServiceStatus.value = "已降级 (连续3次请求失败，实时检测已自动关闭)"
                    _realTimeStateEnabled.value = false
                } else {
                    _aiServiceStatus.value = "服务异常 (重试中...)"
                }
            } else {
                _aiConsecutiveFailures.value = 0
                _aiServiceStatus.value = "服务正常"
            }

            // 写回本地调用日志
            repository.saveAssistRecord(
                AiAssistRecord(
                    studentId = studentId,
                    classId = classId,
                    assistType = "在线对答",
                    assistTypeInt = 1, // On-line dialog under grammar correctness tab
                    requestContent = question,
                    aiResult = response,
                    draftId = null
                )
            )
            } catch (e: Exception) {
                Log.e("AIFlow", "callAiCustomQuestion 异常: ${e.javaClass.simpleName}: ${e.message}", e)
                onResponse("【发生异常】${e.message ?: "未知错误"}")
            } finally {
                _aiLoading.value = false
                Log.d("AIFlow", "callAiCustomQuestion 结束, aiLoading已复位")
            }
        }
    }

    // --- 考点讲解: 深入浅出的少儿编程知识点剖析, 结合学生当前代码 ---
    fun callKnowledgeExplain(topic: String) {
        Log.d("AIFlow", "[考点讲解] 开始, topic=$topic")
        if (_aiLoading.value) {
            Log.w("AIFlow", "[考点讲解] 正在加载中, 忽略")
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _aiLoading.value = true
            _aiResult.value = null
            _aiResultType.value = "知识点讲解"
            try {
                val studentId = _currentUserId.value
                val classId = _currentClassId.value
                if (studentId == -1) {
                    _aiResult.value = "请先登录后再使用此功能"
                    _aiLoading.value = false
                    return@launch
                }

                // 每日限额检查
                val countOk = repository.checkDailyAssistOk(studentId, classId)
                if (!countOk) {
                    _aiResult.value = "今天的AI使用次数已用完，明天再来吧！"
                    _aiLoading.value = false
                    return@launch
                }

                val currentCode = currentDraftCode.value
                val codeContextPrompt = if (currentCode.isNotBlank() && currentCode != "{}") {
                    "\n\n学生当前的 Scratch 积木代码如下：\n$currentCode\n请在讲解中顺便告诉学生：“在你当前的代码里，这个知识点可以加在...处哦！”"
                } else ""

                // 深度定制的少儿编程考点讲解提示词
                val prompt = """
                    你是一个超级有爱心、说话极其温柔可爱、充满童趣的 Scratch 3.0“编程精灵姐姐”。
                    请用小学 3-6 年级（8-12岁）小朋友完全能听懂的语言，深度讲解【$topic】这个 Scratch 编程核心知识点。$codeContextPrompt

                    请严格按照以下 4 个结构输出，文字要短小活泼，多用表情符号（✨, 🐱, 💡, 🚀, 🎈, 🎮）：

                    1. 🌟【奇妙比喻】：用生活中的事物（如魔法收纳盒、旋转木马、交通红绿灯、打卡小哨兵等）做生动有趣的比喻，一句话解释它是什么。
                    2. 💡【为什么有用】：告诉小朋友这个知识点在做游戏/动画时（比如控制得分、让角色跑起来、判断碰撞）能带来什么神奇效果。
                    3. 🐾【动手拼搭三步走】：
                       ① 找菜单：点击左侧【什么颜色/分类】菜单（如黄色控制、蓝色运动、橙色变量）；
                       ② 找积木：拿到【具体积木名称】积木；
                       ③ 怎么拼：拼插在什么积木里面或下面，参数该填多少。
                    4. 🎮【一分钟小试身手】：给出一个超级简单有趣的 1 分钟动手尝试小挑战，鼓励孩子立刻去试试！
                """.trimIndent()

                Log.d("AIFlow", "[考点讲解] 调用API, prompt长度=${prompt.length}")

                val response = try {
                    kotlinx.coroutines.withTimeout(45000L) {
                        callGeminiWithTimeoutAndRetry(prompt)
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    "【连接超时啦 ⏰】精灵姐姐刚才可能开小差去采花了，没有在规定时间内赶回来。别着急，我们可以重新点一下考点按钮重试哦！"
                } catch (e: Exception) {
                    Log.e("AIFlow", "[考点讲解] API异常: ${e.message}")
                    "【网络有点小毛病 ☁️】精灵姐姐暂时没有收到信号。别着急，让网络飞一会儿，咱们过 10 秒钟再点一下重试吧！"
                }

                _aiResult.value = response
                repository.saveAssistRecord(
                    AiAssistRecord(
                        studentId = studentId, classId = classId,
                        assistType = "知识点讲解", assistTypeInt = 3,
                        requestContent = topic, aiResult = response, draftId = null
                    )
                )
                Log.d("AIFlow", "[考点讲解] 完成, 响应长度=${response.length}")
            } catch (e: Exception) {
                Log.e("AIFlow", "[考点讲解] 异常: ${e.message}")
                _aiResult.value = "出错了，请重试"
            } finally {
                _aiLoading.value = false
            }
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

    // --- 教师端更新/修改/删除任务 ---
    fun updateTaskStatusByTeacher(taskId: Int, status: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateTaskStatus(taskId, status)
                onResult("任务状态已成功更新为：$status！")
                onUserLoggedIn()
            } catch (e: Exception) {
                e.printStackTrace()
                onResult("更新任务状态失败: ${e.message}")
            }
        }
    }

    fun editTaskByTeacher(taskId: Int, name: String, detail: String, grade: String, deadlineStr: String, classId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val deadlineTime = try {
                    df.parse(deadlineStr)?.time ?: (System.currentTimeMillis() + 7 * 24 * 3600 * 1000L)
                } catch (e: Exception) {
                    System.currentTimeMillis() + 7 * 24 * 3600 * 1000L
                }
                repository.updateTaskDetails(taskId, name, detail, grade, deadlineStr, deadlineTime, classId)
                onResult("任务修改成功，信息已保存！")
                onUserLoggedIn()
            } catch (e: Exception) {
                e.printStackTrace()
                onResult("修改任务失败: ${e.message}")
            }
        }
    }

    fun deleteTaskByTeacher(taskId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
                onResult("任务已彻底安全删除！")
                onUserLoggedIn()
            } catch (e: Exception) {
                e.printStackTrace()
                onResult("删除任务失败: ${e.message}")
            }
        }
    }

    // --- 教师端创建新班级 & 配制默认 AI 安全等级 ---
    fun createNewClassByTeacher(className: String, grade: String, description: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val teacherId = _currentUserId.value
            if (teacherId == -1) return@launch

            // 1. 检查是否存在同名且启用的班级（软删除的班级不算）
            val duplicateCount = repository.checkDuplicateClass(className, grade, excludeClassId = 0)
            if (duplicateCount > 0) {
                // 如果已存在同名班级，获取该班级信息
                val existingClass = _classesList.value.find { it.grade == grade && it.className == className }
                if (existingClass != null) {
                    val existingStudents = repository.getStudentsByClass(existingClass.classId)
                    if (existingStudents.isNotEmpty()) {
                        onResult("该年级下已存在【${className}】且有 ${existingStudents.size} 名学生，无需重复创建！")
                        return@launch
                    } else {
                        // 如果已存在但无学生，提示用户可以直接使用现有班级或先删除再创建
                        onResult("该年级下已存在【${className}】但无学生。如需重新创建，请先删除现有班级档案。")
                        return@launch
                    }
                }
            }

            // 2. 检查是否存在同名但已禁用的班级（用于恢复）
            val disabledClass = repository.getDisabledClassByName(className, grade)
            if (disabledClass != null) {
                // 恢复已禁用的班级，复用原有 classId
                repository.restoreClass(disabledClass.classId)
                
                // 更新班级描述
                SharedPreferencesUtil.saveClassDescription(context, disabledClass.classId, description)
                
                // 确保 AI 配置存在
                val existingConfig = repository.getConfigByClassId(disabledClass.classId)
                if (existingConfig == null) {
                    repository.saveConfig(
                        com.example.data.AiTeachingConfig(
                            classId = disabledClass.classId,
                            teacherId = teacherId,
                            aiHintLevel = "入门阶梯引导",
                            creativeGuideDailyLimit = 8,
                            codeGenerationLimit = 0
                        )
                    )
                }
                
                // 检查是否已有学生数据，如果没有则导入预设学生
                val existingStudents = repository.getStudentsByClass(disabledClass.classId)
                var importedCount = 0
                
                if (existingStudents.isEmpty()) {
                    Log.d("CreateClass", "开始为恢复的班级【${className}】(ID: ${disabledClass.classId}) 载入预设学生...")
                    
                    // 策略1：查找同年级其他班级的学生作为模板
                    var templateStudents = repository.getAllStudents().filter { s ->
                        val studentClass = repository.getClassById(s.classId)
                        studentClass?.grade == grade
                    }
                    Log.d("CreateClass", "找到同年级模板学生数量: ${templateStudents.size}")
                    
                    // 策略2：如果同年级没有其他班级，尝试使用任意班级的学生作为模板
                    if (templateStudents.isEmpty()) {
                        templateStudents = repository.getAllStudents()
                        Log.d("CreateClass", "同年级无模板，使用任意班级模板学生数量: ${templateStudents.size}")
                    }
                    
                    if (templateStudents.isNotEmpty()) {
                        // 为该班级添加默认学生（基于模板班级的学生数量）
                        for ((index, templateStudent) in templateStudents.withIndex()) {
                            // 生成新的学号：S + 班级ID + 序号
                            val newStudentNum = "S${disabledClass.classId}${(index + 1).toString().padStart(3, '0')}"
                            // 去除模板学生名称中的括号内容，只保留姓名
                            val baseName = templateStudent.name.substringBefore('(')
                            val newStudentName = baseName
                            
                            val newStudent = Student(
                                studentNumber = newStudentNum,
                                name = newStudentName,
                                password = "123456", // 默认密码
                                classId = disabledClass.classId
                            )
                            val result = repository.registerStudent(newStudent)
                            if (result > 0) {
                                importedCount++
                                if (importedCount <= 3 || importedCount == templateStudents.size) {
                                    Log.d("CreateClass", "成功注册第 ${importedCount} 名学生: ${newStudent.studentNumber} - ${newStudent.name}")
                                }
                            } else {
                                Log.e("CreateClass", "注册学生失败: ${newStudent.studentNumber}")
                            }
                        }
                        
                        kotlinx.coroutines.delay(100)
                        Log.d("CreateClass", "学生注册完成，共导入 ${importedCount} 名学生")
                    }
                }
                
                Log.d("CreateClass", "成功恢复已禁用班级【${className}】(ID: ${disabledClass.classId})")
                
                val successMsg = if (importedCount > 0) {
                    "班级【${className}】创建成功，已自动载入 ${importedCount} 名学生！AI阶梯防护罩及防沉迷设定已就绪！"
                } else if (existingStudents.isNotEmpty()) {
                    "班级【${className}】创建成功，历史数据（${existingStudents.size} 名学生）已保留！AI阶梯防护罩及防沉迷设定已就绪！"
                } else {
                    "班级【${className}】创建成功，AI阶梯防护罩及防沉迷设定已就绪！（暂无预置学生，请手动添加）"
                }
                onResult(successMsg)
                loadClasses()
                kotlinx.coroutines.delay(150)
                onUserLoggedIn()
                return@launch
            }

            // 3. 创建新班级（首次创建，无历史记录）
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
                
                // 3. 智能载入学生数据
                Log.d("CreateClass", "开始为新班级【${className}】(ID: ${newClassId}) 载入学生...")
                
                // 策略1：查找同年级其他班级的学生作为模板
                var templateStudents = repository.getAllStudents().filter { s ->
                    val studentClass = repository.getClassById(s.classId)
                    studentClass?.grade == grade
                }
                Log.d("CreateClass", "找到同年级模板学生数量: ${templateStudents.size}")
                
                // 策略2：如果同年级没有其他班级，尝试使用任意班级的学生作为模板
                if (templateStudents.isEmpty()) {
                    templateStudents = repository.getAllStudents()
                    Log.d("CreateClass", "同年级无模板，使用任意班级模板学生数量: ${templateStudents.size}")
                }
                
                var importedCount = 0
                if (templateStudents.isNotEmpty()) {
                    // 为该新班级添加默认学生（基于模板班级的学生数量）
                    for ((index, templateStudent) in templateStudents.withIndex()) {
                        // 生成新的学号：S + 新班级ID + 序号
                        val newStudentNum = "S${newClassId}${(index + 1).toString().padStart(3, '0')}"
                        // 去除模板学生名称中的括号内容，只保留姓名
                        val baseName = templateStudent.name.substringBefore('(')
                        val newStudentName = baseName
                        
                        val newStudent = Student(
                            studentNumber = newStudentNum,
                            name = newStudentName,
                            password = "123456", // 默认密码
                            classId = newClassId
                        )
                        val result = repository.registerStudent(newStudent)
                        if (result > 0) {
                            importedCount++
                            if (importedCount <= 3 || importedCount == templateStudents.size) {
                                Log.d("CreateClass", "成功注册第 ${importedCount} 名学生: ${newStudent.studentNumber} - ${newStudent.name}")
                            }
                        } else {
                            Log.e("CreateClass", "注册学生失败: ${newStudent.studentNumber}")
                        }
                    }
                    
                    // 等待一小段时间确保数据库事务提交
                    kotlinx.coroutines.delay(100)
                    Log.d("CreateClass", "学生注册完成，共导入 ${importedCount} 名学生")
                }
                
                val successMsg = if (importedCount > 0) {
                    "班级【${className}】创建成功，已自动载入 ${importedCount} 名学生！AI阶梯防护罩及防沉迷设定已就绪！"
                } else {
                    "班级【${className}】创建成功，AI阶梯防护罩及防沉迷设定已就绪！（暂无预置学生，请手动添加）"
                }
                onResult(successMsg)
                
                loadClasses()
                // 等待一小段时间确保数据库事务提交后，再刷新学生列表
                kotlinx.coroutines.delay(150)
                // 刷一下学生列表
                onUserLoggedIn()
                Log.d("CreateClass", "班级创建完成，已刷新学生列表")
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
                // Check deduplication using DAO method (only check active classes)
                val exists = repository.checkDuplicateClass(fullClassName, grade, excludeClassId = 0) > 0
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
                val className = repository.getClassById(classId)?.className ?: "该班级"
                repository.deleteClass(classId)
                onResult("【${className}】已删除，历史数据已保留。如需恢复，请重新创建同名班级即可复用原有 classId=${classId}！")
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
                // Check deduplication using DAO method (excluding current classId and only checking active classes)
                val exists = repository.checkDuplicateClass(className, grade, excludeClassId = classId) > 0
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

    // --- 获取 Scratch 练习模板代码 ---
    fun getTemplateCode(id: Int): String {
        return staticGetTemplateCode(id)
    }

    fun enterTaskProgramming(taskId: Int, taskName: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val studentId = _currentUserId.value
            if (studentId != -1) {
                val existingDraft = repository.getDraftsByStudentDirect(studentId).find { it.taskId == taskId }
                if (existingDraft != null) {
                    currentDraftCode.value = existingDraft.blockCode
                    currentDraftName.value = existingDraft.draftName
                    currentTaskId.value = taskId
                    currentTaskName.value = taskName
                    workspaceLoadEvent.value = existingDraft.blockCode
                } else {
                    val template = staticGetTemplateCode(taskId)
                    val draftNameString = "$taskName - 草稿"
                    val newDraft = ScratchDraft(
                        studentId = studentId,
                        taskId = taskId,
                        draftName = draftNameString,
                        blockCode = template
                    )
                    repository.saveDraft(newDraft)
                    
                    currentDraftCode.value = template
                    currentDraftName.value = draftNameString
                    currentTaskId.value = taskId
                    currentTaskName.value = taskName
                    workspaceLoadEvent.value = template
                }
            }
            onComplete()
        }
    }

    fun updateStudentPassword(newPass: String, onResult: (Boolean, String) -> Unit) {
        val sId = _currentUserId.value
        if (sId == -1) {
            onResult(false, "错误：未登录")
            return
        }
        viewModelScope.launch {
            try {
                repository.updateStudentPassword(sId, newPass)
                val updatedStudent = repository.getStudentById(sId)
                currentStudentDetails.value = updatedStudent
                onResult(true, "密码修改成功！新密码已生效。")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "密码修改失败，请重试。")
            }
        }
    }

    // =========================================================================
    // --- 拓展高级模块方法 (Tasks 2, 3, 4, 5, 6) ---
    // =========================================================================

    // 开源大厅与同伴互评 (Task 2)
    val publicWorksList: StateFlow<List<ScratchWork>> = repository.getPublicWorksFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val popularWorksList: StateFlow<List<ScratchWork>> = repository.getPopularPublicWorksFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val likedWorkIds: StateFlow<Set<Int>> = _currentUserId
        .flatMapLatest { studentId ->
            if (studentId != -1) {
                repository.getLikedWorkIdsFlow(studentId.toString())
            } else {
                flowOf(emptyList())
            }
        }
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    fun toggleWorkPublic(workId: Int, isPublic: Boolean) {
        viewModelScope.launch {
            repository.toggleWorkPublicStatus(workId, isPublic)
        }
    }

    fun likeWork(workId: Int) {
        toggleLikeWork(workId)
    }

    fun toggleLikeWork(workId: Int, onResult: ((Boolean, String) -> Unit)? = null) {
        val sId = _currentUserId.value
        if (sId == -1) {
            onResult?.invoke(false, "请先登录学生账号再进行点赞哦~")
            return
        }
        viewModelScope.launch {
            try {
                val isLiked = repository.toggleLike(workId, sId.toString())
                val message = if (isLiked) "♥ 点赞成功！" else "已取消点赞"
                onResult?.invoke(isLiked, message)
            } catch (e: Exception) {
                onResult?.invoke(false, "操作失败: ${e.message}")
            }
        }
    }

    fun forkWork(sourceWork: ScratchWork, onResult: (Boolean, String) -> Unit) {
        val sId = _currentUserId.value
        val cId = _currentClassId.value
        if (sId == -1) {
            onResult(false, "请先登录学生账号再进行 Fork 二次开发哦~")
            return
        }
        viewModelScope.launch {
            try {
                val newId = repository.forkWork(sourceWork, sId, cId)
                currentDraftCode.value = sourceWork.workCode
                currentDraftName.value = "${sourceWork.workName} (克隆自 ${sourceWork.studentId})"
                workspaceLoadEvent.value = sourceWork.workCode
                onResult(true, "🎉 克隆二次开发成功！已导入工作台，新作品 ID: $newId")
            } catch (e: Exception) {
                onResult(false, "克隆失败: ${e.message}")
            }
        }
    }

    // AI 自动化内容风控与评论提交 (Task 3)
    fun submitComment(workId: Int, content: String, onResult: (Boolean, String) -> Unit) {
        val sId = _currentUserId.value
        val name = _currentUserName.value.ifBlank { "匿名同学" }
        if (sId == -1) {
            onResult(false, "请登录后发表评论")
            return
        }
        viewModelScope.launch {
            val mod = repository.submitComment(workId, sId, name, content)
            if (mod.isSafe) {
                onResult(true, "评论发表成功，已通过 AI 风控审核！")
            } else {
                onResult(false, "🚨 发表失败：${mod.reason}")
            }
        }
    }

    fun getCommentsForWork(workId: Int): Flow<List<WorkComment>> =
        repository.getCommentsByWorkFlow(workId)

    // 教师多维学情可视化大屏数据 (Task 4)
    val classAnalyticsState = MutableStateFlow<AppRepository.ClassAnalyticsData?>(null)

    fun loadClassAnalytics(classId: Int, taskId: Int? = null) {
        viewModelScope.launch {
            val data = repository.getClassAnalyticsData(classId, taskId)
            classAnalyticsState.value = data
        }
    }

    // 离线断点续传与防抖自动保存 (Task 5)
    val syncStatusNotice = MutableStateFlow<String?>(null)

    fun triggerDebouncedAutoSave(draftId: Int?, codeJson: String) {
        viewModelScope.launch {
            val studentId = _currentUserId.value
            if (studentId == -1) return@launch
            val draft = ScratchDraft(
                draftId = draftId ?: 0,
                draftName = currentDraftName.value,
                blockCode = codeJson,
                studentId = studentId,
                taskId = currentTaskId.value,
                lastModifiedTime = System.currentTimeMillis()
            )
            repository.saveDraft(draft)
            syncStatusNotice.value = "⚡ 本地自动保存完成 (${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())})"
        }
    }

    fun performOfflineSync(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val syncManager = ScratchSyncManager(repository)
            val res = syncManager.performBackgroundSync()
            syncStatusNotice.value = res.message
            onResult(res.message)
        }
    }

    // AI 评测防作弊与代码相似度检测 (Task 6)
    val plagiarismResultState = MutableStateFlow<ScratchCodeSimilarity.SimilarityResult?>(null)

    fun checkWorkPlagiarism(work: ScratchWork) {
        viewModelScope.launch {
            val result = repository.checkAndSetWorkSimilarity(work)
            plagiarismResultState.value = result
        }
    }
}

// --- 静态外置获取 Scratch 练习模板代码，用于在 ViewModel 初始化时提供初始值 ---
fun staticGetTemplateCode(id: Int): String {
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
