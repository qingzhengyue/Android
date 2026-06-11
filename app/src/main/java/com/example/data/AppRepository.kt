package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.appDao

    // --- 数据库初始化 & 信息同步 ---
    suspend fun initializeDatabase() {
        DatabasePrepopulator.populateIfEmpty(db)
    }

    // --- 教师 ---
    suspend fun registerTeacher(teacher: Teacher): Long = withContext(Dispatchers.IO) {
        dao.insertTeacher(teacher)
    }

    suspend fun getTeacherByWorkId(workId: String): Teacher? = withContext(Dispatchers.IO) {
        dao.getTeacherByWorkId(workId)
    }

    suspend fun getTeacherById(id: Int): Teacher? = withContext(Dispatchers.IO) {
        dao.getTeacherById(id)
    }

    // --- 班级 ---
    suspend fun createClass(classEntity: ClassEntity): Long = withContext(Dispatchers.IO) {
        dao.insertClass(classEntity)
    }

    fun getAllClasses(): Flow<List<ClassEntity>> = dao.getAllClassesFlow()

    suspend fun getClassesByTeacher(teacherId: Int): List<ClassEntity> = withContext(Dispatchers.IO) {
        dao.getClassesByTeacher(teacherId)
    }

    suspend fun getClassById(classId: Int): ClassEntity? = withContext(Dispatchers.IO) {
        dao.getClassById(classId)
    }

    suspend fun deleteClass(classId: Int) = withContext(Dispatchers.IO) {
        dao.deleteClassById(classId)
        dao.deleteStudentsByClass(classId)
        dao.deleteTasksByClass(classId)
    }

    suspend fun updateClass(classId: Int, className: String, grade: String) = withContext(Dispatchers.IO) {
        dao.updateClass(classId, className, grade)
    }

    suspend fun getAiAssistCountByClass(classId: Int): Int = withContext(Dispatchers.IO) {
        dao.getAiAssistCountByClass(classId)
    }

    // --- 学生 ---
    suspend fun registerStudent(student: Student): Long = withContext(Dispatchers.IO) {
        dao.insertStudent(student)
    }

    suspend fun getStudentByNumber(number: String): Student? = withContext(Dispatchers.IO) {
        dao.getStudentByNumber(number)
    }

    suspend fun getStudentById(id: Int): Student? = withContext(Dispatchers.IO) {
        dao.getStudentById(id)
    }

    suspend fun getStudentsByClass(classId: Int): List<Student> = withContext(Dispatchers.IO) {
        dao.getStudentsByClass(classId)
    }

    // --- 教学配置 ---
    suspend fun saveConfig(config: AiTeachingConfig): Long = withContext(Dispatchers.IO) {
        dao.insertConfig(config)
    }

    suspend fun getConfigByClassId(classId: Int): AiTeachingConfig? = withContext(Dispatchers.IO) {
        dao.getConfigByClassId(classId)
    }

    fun getConfigByClassIdFlow(classId: Int): Flow<AiTeachingConfig?> = dao.getConfigByClassIdFlow(classId)

    // --- 任务 ---
    suspend fun publishTask(task: LearningTask): Long = withContext(Dispatchers.IO) {
        dao.insertTask(task)
    }

    fun getAllTasks(): Flow<List<LearningTask>> = dao.getAllTasksFlow()

    fun getTasksByClass(classId: Int): Flow<List<LearningTask>> = dao.getTasksByClassFlow(classId)

    suspend fun getTaskById(id: Int): LearningTask? = withContext(Dispatchers.IO) {
        dao.getTaskById(id)
    }

    suspend fun updateTaskStatus(taskId: Int, status: String) = withContext(Dispatchers.IO) {
        dao.updateTaskStatus(taskId, status)
    }

    // --- 草稿 ---
    suspend fun saveDraft(draft: ScratchDraft): Long = withContext(Dispatchers.IO) {
        dao.insertDraft(draft)
    }

    fun getDraftsByStudent(studentId: Int): Flow<List<ScratchDraft>> = dao.getDraftsByStudentFlow(studentId)

    suspend fun getDraftById(id: Int): ScratchDraft? = withContext(Dispatchers.IO) {
        dao.getDraftById(id)
    }

    suspend fun deleteDraft(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteDraftById(id)
    }

    // --- AI 记录 & 调用频次控制 ---
    suspend fun saveAssistRecord(record: AiAssistRecord): Long = withContext(Dispatchers.IO) {
        dao.insertAssistRecord(record)
    }

    fun getAssistRecordsByStudent(studentId: Int): Flow<List<AiAssistRecord>> = dao.getAssistRecordsByStudentFlow(studentId)

    fun getAssistRecordsByStudentAndType(studentId: Int, type: Int): Flow<List<AiAssistRecord>> =
        dao.getAssistRecordsByStudentAndTypeFlow(studentId, type)

    suspend fun deleteTaskWithSubmissions(taskId: Int) = withContext(Dispatchers.IO) {
        dao.deleteAiReportsByTaskId(taskId)
        dao.deleteWorksByTaskId(taskId)
        dao.deleteTaskById(taskId)
    }

    suspend fun checkDailyAssistOk(studentId: Int, classId: Int): Boolean = withContext(Dispatchers.IO) {
        // 今日起始结束时间
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000L) - 1

        val currentCount = dao.getDailyAssistCount(studentId, startOfDay, endOfDay)
        
        val classDesc = SharedPreferencesUtil.getClassDescription(context, classId)
        var limit = 10
        if (classDesc.trim().startsWith("{") && classDesc.trim().endsWith("}")) {
            try {
                val json = org.json.JSONObject(classDesc)
                limit = json.optInt("dailyLimit", 10)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val config = dao.getConfigByClassId(classId)
            if (config != null) {
                limit = config.creativeGuideDailyLimit
            }
        }
        currentCount < limit
    }

    // --- 作品与自动评测 ---
    suspend fun submitWorkAndEvaluate(work: ScratchWork): WorkAiReport = withContext(Dispatchers.IO) {
        // 1. 获取关联的任务详情
        val task = dao.getTaskById(work.taskId)
        val taskName = task?.taskName ?: "自由创作"
        val taskDetail = task?.taskDetail ?: "制作你喜欢的 Scratch 创意作品。"

        // 2. 插入或修改作品
        val existingWork = dao.getWorkByStudentAndTask(work.studentId, work.taskId)
        val finalWork = if (existingWork != null) {
            work.copy(
                workId = existingWork.workId,
                submitCount = existingWork.submitCount + 1,
                reviewStatus = "已评测"
            )
        } else {
            work.copy(submitCount = 1, reviewStatus = "已评测")
        }
        val insertedWorkId = dao.insertWork(finalWork).toInt()
        val workId = if (finalWork.workId != 0) finalWork.workId else insertedWorkId

        // 3. 异步调用 Gemini API 进行多维度教学评测
        val eval = GeminiClient.evaluateScratchWork(
            taskName = taskName,
            taskDetail = taskDetail,
            workName = work.workName,
            codeJson = work.workCode
        )

        // Calculate dynamic average score based on custom radar weights
        val classDesc = SharedPreferencesUtil.getClassDescription(context, work.classId)
        var weightGrammar = 25
        var weightLogic = 30
        var weightTask = 25
        var weightCreative = 20
        if (classDesc.trim().startsWith("{") && classDesc.trim().endsWith("}")) {
            try {
                val json = org.json.JSONObject(classDesc)
                weightGrammar = json.optInt("weightGrammar", 25)
                weightLogic = json.optInt("weightLogic", 30)
                weightTask = json.optInt("weightTask", 25)
                weightCreative = json.optInt("weightCreative", 20)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val totalWeight = (weightGrammar + weightLogic + weightTask + weightCreative).toDouble()
        val calculatedAverageScore = if (totalWeight > 0) {
            Math.round(
                (eval.grammarScore * weightGrammar +
                 eval.logicScore * weightLogic +
                 eval.taskMatchScore * weightTask +
                 eval.creativeScore * weightCreative) / totalWeight
            ).toInt()
        } else {
            eval.averageScore
        }

        // 4. 将 AI 评测报告插入数据库
        val report = WorkAiReport(
            workId = workId,
            studentId = work.studentId,
            grammarScore = eval.grammarScore,
            logicScore = eval.logicScore,
            taskMatchScore = eval.taskMatchScore,
            creativeScore = eval.creativeScore,
            averageScore = calculatedAverageScore,
            optimizationSuggestions = eval.suggestions
        )
        dao.insertAiReport(report)

        report
    }

    fun getWorksByStudent(studentId: Int): Flow<List<ScratchWork>> = dao.getWorksByStudentFlow(studentId)

    suspend fun getWorkById(workId: Int): ScratchWork? = withContext(Dispatchers.IO) {
        dao.getWorkById(workId)
    }

    fun getReportForWorkFlow(workId: Int): Flow<WorkAiReport?> = dao.getReportByWorkIdFlow(workId)

    suspend fun getReportForWork(workId: Int): WorkAiReport? = withContext(Dispatchers.IO) {
        dao.getReportByWorkId(workId)
    }

    suspend fun getWorksByClass(classId: Int): List<ScratchWork> = withContext(Dispatchers.IO) {
        dao.getWorksByClass(classId)
    }

    fun getAllWorksFlow(): Flow<List<ScratchWork>> = dao.getAllWorksFlow()

    fun getAllStudentsFlow(): Flow<List<Student>> = dao.getAllStudentsFlow()

    suspend fun getAllStudents(): List<Student> = withContext(Dispatchers.IO) {
        dao.getAllStudents()
    }

    suspend fun getAllTasksList(): List<LearningTask> = withContext(Dispatchers.IO) {
        dao.getAllTasksList()
    }

    suspend fun updateWorkReview(workId: Int, status: String, score: Int?, comment: String?) = withContext(Dispatchers.IO) {
        dao.updateWorkReview(workId, status, score, comment, System.currentTimeMillis())
    }

    suspend fun updateTaskDetails(taskId: Int, name: String, detail: String, grade: String, deadline: String, deadlineTime: Long) = withContext(Dispatchers.IO) {
        dao.updateTaskDetails(taskId, name, detail, grade, deadline, deadlineTime)
    }

    suspend fun updateTaskStatus(taskId: Int, status: String) = withContext(Dispatchers.IO) {
        dao.updateTaskStatus(taskId, status)
    }

    suspend fun deleteTask(taskId: Int) = withContext(Dispatchers.IO) {
        dao.deleteAiReportsByTaskId(taskId)
        dao.deleteWorksByTaskId(taskId)
        dao.deleteTaskById(taskId)
    }
}
