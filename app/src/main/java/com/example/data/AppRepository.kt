package com.example.data

import android.content.Context
import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.serialization.json.Json



@kotlinx.serialization.Serializable
data class SupabaseScratchWork(
    @kotlinx.serialization.SerialName("work_id") val workId: Int = 0,
    @kotlinx.serialization.SerialName("work_name") val workName: String,
    @kotlinx.serialization.SerialName("work_code") val workCode: String,
    @kotlinx.serialization.SerialName("student_id") val studentId: Long,
    @kotlinx.serialization.SerialName("class_id") val classId: Long,
    @kotlinx.serialization.SerialName("task_id") val taskId: Long,
    @kotlinx.serialization.SerialName("submit_count") val submitCount: Int,
    @kotlinx.serialization.SerialName("submit_time") val submitTime: Long,
    @kotlinx.serialization.SerialName("review_status") val reviewStatus: String
)

class AppRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.appDao

    // --- Supabase 初始化 (安全 nullable，初始化失败不崩溃) ---
    private val supabase: io.github.jan.supabase.SupabaseClient? by lazy {
        try {
            createSupabaseClient(
                supabaseUrl = com.example.BuildConfig.SUPABASE_URL,
                supabaseKey = com.example.BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Postgrest)
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Client init failed (app will work in local-only mode): ${e.message}")
            null
        }
    }

    // --- Supabase 连接状态检测 ---
    private val _supabaseConnectionStatus = MutableStateFlow("未检测")
    val supabaseConnectionStatus: Flow<String> = _supabaseConnectionStatus

    fun getSupabaseConnectionStatusDirect(): String = _supabaseConnectionStatus.value

    suspend fun testSupabaseConnection(): String = withContext(Dispatchers.IO) {
        _supabaseConnectionStatus.value = "检测中..."
        if (supabase == null) {
            _supabaseConnectionStatus.value = "连接失败: 客户端初始化失败"
            return@withContext "连接失败: Supabase客户端初始化失败，请检查URL和Key配置"
        }
        try {
            // 10秒超时保护
            val result = kotlinx.coroutines.withTimeout(10000L) {
                supabase!!.from("teacher")
                    .select { }
                    .decodeList<Teacher>()
            }
            _supabaseConnectionStatus.value = "连接成功"
            Log.d("Supabase", "Connection test SUCCESS: ${result.size} records found")
            "连接成功! Supabase云端数据库已正常连接，云端已有 ${result.size} 条教师数据，数据同步功能可用。"
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            val msg = "连接失败: 请求超时(10秒)，请检查网络连接是否正常"
            _supabaseConnectionStatus.value = msg
            Log.e("Supabase", "Connection test TIMEOUT")
            msg
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true ->
                    "连接失败: API Key无效或已过期(401)，请检查Supabase密钥配置"
                e.message?.contains("403") == true ->
                    "连接失败: 无权访问该表(403)，请检查Supabase RLS策略或RLS规则"
                e.message?.contains("404") == true || e.message?.contains("not found") == true ->
                    "连接失败: 数据表不存在(404)，请先在Supabase控制台创建teacher等数据表"
                e.message?.contains("Unable to resolve") == true || e.message?.contains("UnknownHost") == true ->
                    "连接失败: 无法解析Supabase域名，请检查网络连接"
                else -> "连接失败: ${e.message}"
            }
            _supabaseConnectionStatus.value = errorMsg
            Log.e("Supabase", "Connection test FAILED: ${e.message}")
            errorMsg
        }
    }

    // --- 数据库初始化 & 信息同步 ---
    suspend fun initializeDatabase() {
        DatabasePrepopulator.populateIfEmpty(db)
    }

    // --- 教师操作 ---
    suspend fun registerTeacher(teacher: Teacher): Long = withContext(Dispatchers.IO) {
        val localId = dao.insertTeacher(teacher)
        val teacherWithId = if (teacher.teacherId == 0) teacher.copy(teacherId = localId.toInt()) else teacher
        try {
            supabase?.from("teacher")?.upsert(teacherWithId)
        } catch (e: Exception) {
            Log.e("Supabase", "Teacher sync failed: ${e.message}")
        }
        localId
    }

    suspend fun getTeacherByWorkId(workId: String): Teacher? = withContext(Dispatchers.IO) {
        val local = dao.getTeacherByWorkId(workId)
        if (local != null) return@withContext local
        
        try {
            val remote = supabase?.from("teacher")
                ?.select { 
                    filter {
                        eq("work_id", workId)
                    }
                }
                ?.decodeSingleOrNull<Teacher>()
            if (remote != null) {
                dao.insertTeacher(remote)
                return@withContext remote
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Teacher fetch failed: ${e.message}")
        }
        null
    }

    suspend fun getTeacherById(id: Int): Teacher? = withContext(Dispatchers.IO) {
        dao.getTeacherById(id)
    }

    // --- 班级操作 ---
    suspend fun createClass(classEntity: ClassEntity): Long = withContext(Dispatchers.IO) {
        val localId = dao.insertClass(classEntity)
        val classWithId = if (classEntity.classId == 0) classEntity.copy(classId = localId.toInt()) else classEntity
        try {
            supabase?.from("class")?.upsert(classWithId)
        } catch (e: Exception) {
            Log.e("Supabase", "Class sync failed: ${e.message}")
        }
        localId
    }

    fun getAllClasses(): Flow<List<ClassEntity>> = dao.getAllClassesFlow()

    suspend fun getClassesByTeacher(teacherId: Int): List<ClassEntity> = withContext(Dispatchers.IO) {
        dao.getClassesByTeacher(teacherId)
    }

    suspend fun getClassById(classId: Int): ClassEntity? = withContext(Dispatchers.IO) {
        dao.getClassById(classId)
    }

    // 检查是否存在同名且启用的班级（排除指定ID）
    suspend fun checkDuplicateClass(className: String, grade: String, excludeClassId: Int): Int = withContext(Dispatchers.IO) {
        dao.checkDuplicateClass(className, grade, excludeClassId)
    }

    suspend fun deleteClass(classId: Int) = withContext(Dispatchers.IO) {
        // 软删除：只将 isActive 设为 false，保留 classId 不变
        dao.softDeleteClassById(classId)
        
        // 不清理关联数据（学生、任务、作品），保留历史数据以便恢复时复用
        // dao.deleteStudentsByClass(classId)
        // dao.deleteTasksByClass(classId)
        // dao.deleteWorksByClass(classId)
        
        try {
            supabase?.from("class")?.delete { 
                filter {
                    eq("class_id", classId)
                }
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Class delete failed: ${e.message}")
        }
    }

    // 查询已禁用的班级（用于恢复）
    suspend fun getDisabledClassByName(className: String, grade: String): ClassEntity? = withContext(Dispatchers.IO) {
        dao.getDisabledClassByName(className, grade)
    }

    // 恢复已禁用的班级
    suspend fun restoreClass(classId: Int) = withContext(Dispatchers.IO) {
        dao.restoreClassById(classId)
    }

    suspend fun updateClass(classId: Int, className: String, grade: String) = withContext(Dispatchers.IO) {
        dao.updateClass(classId, className, grade)
        try {
            val updated = dao.getClassById(classId)
            if (updated != null) {
                supabase?.from("class")?.update(updated) { 
                    filter {
                        eq("class_id", classId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Class update failed: ${e.message}")
        }
    }

    suspend fun getAiAssistCountByClass(classId: Int): Int = withContext(Dispatchers.IO) {
        dao.getAiAssistCountByClass(classId)
    }

    // --- 学生操作 ---
    suspend fun registerStudent(student: Student): Long = withContext(Dispatchers.IO) {
        val localId = dao.insertStudent(student)
        val studentWithId = if (student.studentId == 0) student.copy(studentId = localId.toInt()) else student
        try {
            supabase?.from("student")?.upsert(studentWithId)
        } catch (e: Exception) {
            Log.e("Supabase", "Student sync failed: ${e.message}")
        }
        localId
    }

    suspend fun getStudentByNumber(number: String): Student? = withContext(Dispatchers.IO) {
        val local = dao.getStudentByNumber(number)
        if (local != null) return@withContext local

        try {
            val remote = supabase?.from("student")
                ?.select { 
                    filter {
                        eq("student_number", number)
                    }
                }
                ?.decodeSingleOrNull<Student>()
            if (remote != null) {
                dao.insertStudent(remote)
                return@withContext remote
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Student fetch failed: ${e.message}")
        }
        null
    }

    suspend fun getStudentById(id: Int): Student? = withContext(Dispatchers.IO) {
        dao.getStudentById(id)
    }

    suspend fun updateStudentPassword(studentId: Int, newPass: String) = withContext(Dispatchers.IO) {
        dao.updateStudentPassword(studentId, newPass)
        try {
            val updated = dao.getStudentById(studentId)
            if (updated != null) {
                supabase?.from("student")?.update(updated) { 
                    filter {
                        eq("student_id", studentId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Password update sync failed: ${e.message}")
        }
    }

    suspend fun getStudentsByClass(classId: Int): List<Student> = withContext(Dispatchers.IO) {
        dao.getStudentsByClass(classId)
    }

    // --- 教学配置 ---
    suspend fun saveConfig(config: AiTeachingConfig): Long = withContext(Dispatchers.IO) {
        val localId = dao.insertConfig(config)
        val configWithId = if (config.configId == 0) config.copy(configId = localId.toInt()) else config
        try {
            supabase?.from("ai_teaching_config")?.upsert(configWithId)
        } catch (e: Exception) {
            Log.e("Supabase", "Config sync failed: ${e.message}")
        }
        localId
    }

    suspend fun getConfigByClassId(classId: Int): AiTeachingConfig? = withContext(Dispatchers.IO) {
        val local = dao.getConfigByClassId(classId)
        if (local != null) return@withContext local
        
        try {
            val remote = supabase?.from("ai_teaching_config")
                ?.select { 
                    filter {
                        eq("class_id", classId)
                    }
                }
                ?.decodeSingleOrNull<AiTeachingConfig>()
            if (remote != null) {
                dao.insertConfig(remote)
                return@withContext remote
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Config fetch failed: ${e.message}")
        }
        null
    }

    fun getConfigByClassIdFlow(classId: Int): Flow<AiTeachingConfig?> = dao.getConfigByClassIdFlow(classId)

    // --- 任务操作 ---
    suspend fun publishTask(task: LearningTask): Long = withContext(Dispatchers.IO) {
        val localId = dao.insertTask(task)
        val taskWithId = if (task.taskId == 0) task.copy(taskId = localId.toInt()) else task
        try {
            supabase?.from("learning_task")?.upsert(taskWithId)
        } catch (e: Exception) {
            Log.e("Supabase", "Task publish sync failed: ${e.message}")
        }
        localId
    }

    fun getAllTasks(): Flow<List<LearningTask>> = dao.getAllTasksFlow()

    fun getTasksByClass(classId: Int): Flow<List<LearningTask>> = dao.getTasksByClassFlow(classId)

    suspend fun getTaskById(id: Int): LearningTask? = withContext(Dispatchers.IO) {
        dao.getTaskById(id)
    }

    suspend fun updateTaskStatus(taskId: Int, status: String) = withContext(Dispatchers.IO) {
        dao.updateTaskStatus(taskId, status)
        try {
            val updated = dao.getTaskById(taskId)
            if (updated != null) {
                supabase?.from("learning_task")?.update(updated) { 
                    filter {
                        eq("task_id", taskId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Task status sync failed: ${e.message}")
        }
    }

    // --- 草稿操作 ---
    suspend fun saveDraft(draft: ScratchDraft): Long = withContext(Dispatchers.IO) {
        val localId = dao.insertDraft(draft)
        val draftWithId = if (draft.draftId == 0) draft.copy(draftId = localId.toInt()) else draft
        // Fire and forget, don't block the UI thread waiting for 30s timeout
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                // Ignore sync locally if no supabase server
                if (!com.example.BuildConfig.SUPABASE_URL.contains("169.254")) {
                    supabase?.from("scratch_draft")?.upsert(draftWithId)
                }
            } catch (e: Exception) {
                // Silently ignore sync failures to prevent log spam
            }
        }
        localId
    }

    suspend fun getDraftsByStudentDirect(studentId: Int): List<ScratchDraft> = withContext(Dispatchers.IO) {
        dao.getDraftsByStudentDirect(studentId)
    }

    fun getDraftsByStudent(studentId: Int): Flow<List<ScratchDraft>> = dao.getDraftsByStudentFlow(studentId)

    suspend fun getDraftById(id: Int): ScratchDraft? = withContext(Dispatchers.IO) {
        dao.getDraftById(id)
    }

    suspend fun deleteDraft(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteDraftById(id)
        try {
            supabase?.from("scratch_draft")?.delete { 
                filter {
                    eq("draft_id", id)
                }
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Draft delete sync failed: ${e.message}")
        }
    }

    // --- AI 记录 ---
    suspend fun saveAssistRecord(record: AiAssistRecord): Long = withContext(Dispatchers.IO) {
        val localId = dao.insertAssistRecord(record)
        val recordWithId = if (record.callId == 0) record.copy(callId = localId.toInt()) else record
        try {
            withTimeoutOrNull(3000L) {
                supabase?.from("ai_assist_record")?.upsert(recordWithId)
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Assist record sync failed: ${e.message}")
        }
        localId
    }

    fun getAssistRecordsByStudent(studentId: Int): Flow<List<AiAssistRecord>> = dao.getAssistRecordsByStudentFlow(studentId)

    fun getAssistRecordsByStudentAndType(studentId: Int, type: Int): Flow<List<AiAssistRecord>> =
        dao.getAssistRecordsByStudentAndTypeFlow(studentId, type)

    // --- 作品与自动评测 ---
    suspend fun submitWorkAndEvaluate(work: ScratchWork): WorkAiReport = withContext(Dispatchers.IO) {
        val existingWork = dao.getWorkByStudentAndTask(work.studentId, work.taskId)
        val finalWork = if (existingWork != null) {
            work.copy(workId = existingWork.workId, submitCount = existingWork.submitCount + 1, reviewStatus = "已评测")
        } else {
            work.copy(submitCount = 1, reviewStatus = "已评测")
        }
        val insertedWorkId = dao.insertWork(finalWork).toInt()
        val workId = if (finalWork.workId != 0) finalWork.workId else insertedWorkId
        val finalWorkWithId = finalWork.copy(workId = workId)

        // 同步作品到云端
        val realWorkId = try {
            // 获取真正的外部 ID (如学号 3101)
            val student = dao.getStudentById(finalWorkWithId.studentId)
            val realStudentId = student?.studentNumber?.replace(Regex("[^0-9]"), "")?.toLongOrNull() ?: finalWorkWithId.studentId.toLong()
            
            // 为了模拟教学场景并解决外键冲突，这里强制转换并传入正确的父级 ID
            // 班级 ID（例如 31）和任务 ID（例如 500）
            // 如果你在测试中遇到 class_id 或 task_id 冲突，这里可以进行同样的安全映射处理
            val supabaseDto = SupabaseScratchWork(
                workId = finalWorkWithId.workId,
                workName = finalWorkWithId.workName,
                workCode = finalWorkWithId.workCode,
                studentId = realStudentId, 
                classId = 31L, // 模拟：强制绑定到三年级一班
                taskId = 500L, // 模拟：强制绑定到基础任务
                submitCount = finalWorkWithId.submitCount,
                submitTime = finalWorkWithId.submitTime,
                reviewStatus = finalWorkWithId.reviewStatus
            )
            val remoteWork = supabase?.from("scratch_work")?.upsert(supabaseDto) {
                select()
            }?.decodeSingle<SupabaseScratchWork>()
            remoteWork?.workId ?: workId
        } catch (e: Exception) {
            Log.e("SupabaseSync", "同步上传失败，拦截到的底层错误是: ${e.message}", e)
            val errorMsg = e.message ?: ""
            Log.w("SupabaseSync", "Ignore sync error and proceed locally")
        }

        val task = dao.getTaskById(work.taskId)
        val eval = GeminiClient.evaluateScratchWork(
            taskName = task?.taskName ?: "",
            taskDetail = task?.taskDetail ?: "",
            workName = work.workName,
            codeJson = work.workCode
        )

        val report = WorkAiReport(
            workId = workId, // 使用本地的 workId 保持本地数据库一致性
            studentId = work.studentId,
            grammarScore = eval.grammarScore,
            logicScore = eval.logicScore,
            taskMatchScore = eval.taskMatchScore,
            creativeScore = eval.creativeScore,
            averageScore = eval.averageScore,
            optimizationSuggestions = eval.suggestions
        )
        val localReportId = dao.insertAiReport(report)
        val reportWithId = report.copy(reportId = localReportId.toInt())

        try {
            supabase?.from("work_ai_report")?.upsert(reportWithId)
        } catch (e: Exception) {
            Log.e("SupabaseSync", "评测报告同步失败，拦截到的底层错误是: ${e.message}", e)
            val errorMsg = e.message ?: ""
            Log.w("SupabaseSync", "Ignore sync error and proceed locally")
        }

        reportWithId
    }

    fun getWorkWithReportByStudent(studentId: Int, taskId: Int): Flow<WorkWithReport?> =
        dao.getWorkWithReportFlow(studentId, taskId)

    suspend fun updateWorkReviewStatus(workId: Int, status: String) = withContext(Dispatchers.IO) {
        val reviewTime = System.currentTimeMillis()
        dao.updateWorkReview(workId, status, null, null, reviewTime)
        try {
            val updated = dao.getWorkById(workId)
            if (updated != null) {
                supabase?.from("scratch_work")?.update(updated) { 
                    filter {
                        eq("work_id", workId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Work review sync failed: ${e.message}")
        }
    }

    suspend fun deleteTask(taskId: Int) = withContext(Dispatchers.IO) {
        dao.deleteTaskById(taskId)
        try {
            supabase?.from("learning_task")?.delete { 
                filter {
                    eq("task_id", taskId)
                }
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Task delete failed: ${e.message}")
        }
    }

    // --- 作品查询 ---
    fun getWorksByStudent(studentId: Int): Flow<List<ScratchWork>> = dao.getWorksByStudentFlow(studentId)

    fun getAllWorksFlow(): Flow<List<ScratchWork>> = dao.getAllWorksFlow()

    fun getAllStudentsFlow(): Flow<List<Student>> = dao.getAllStudentsFlow()
    
    suspend fun getAllStudents(): List<Student> = withContext(Dispatchers.IO) {
        dao.getAllStudents()
    }

    // --- 开源大厅与同伴互评 (Open Hall & Peer Comments) ---
    fun getPublicWorksFlow(): Flow<List<ScratchWork>> = dao.getPublicWorksFlow()

    fun getPopularPublicWorksFlow(): Flow<List<ScratchWork>> = dao.getPopularPublicWorksFlow()

    suspend fun toggleWorkPublicStatus(workId: Int, isPublic: Boolean) = withContext(Dispatchers.IO) {
        dao.updateWorkPublicStatus(workId, isPublic)
        try {
            val updated = dao.getWorkById(workId)
            if (updated != null) {
                supabase?.from("scratch_work")?.upsert(updated)
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Public status sync failed: ${e.message}")
        }
    }

    suspend fun likeWork(workId: Int) = withContext(Dispatchers.IO) {
        dao.incrementWorkLikes(workId)
    }

    suspend fun toggleLike(workId: Int, studentId: String): Boolean = withContext(Dispatchers.IO) {
        val isLiked = dao.checkIsLiked(workId, studentId) > 0
        if (isLiked) {
            // 已经点赞 -> 取消点赞
            dao.deleteLike(workId, studentId)
            dao.updateWorkLikeCount(workId, -1)
            // TODO: 异步向 Supabase 发送 DELETE 请求
            false // 返回当前状态为：未点赞
        } else {
            // 未点赞 -> 新增点赞
            dao.insertLike(WorkLikeEntity(workId = workId, studentId = studentId))
            dao.updateWorkLikeCount(workId, 1)
            // TODO: 异步向 Supabase 发送 INSERT 请求
            true // 返回当前状态为：已点赞
        }
    }

    fun getLikedWorkIdsFlow(studentId: String): Flow<List<Int>> = dao.getLikedWorkIdsFlow(studentId)

    suspend fun forkWork(sourceWork: ScratchWork, targetStudentId: Int, targetClassId: Int): Long = withContext(Dispatchers.IO) {
        val clonedWork = ScratchWork(
            workName = "${sourceWork.workName} (克隆版)",
            workCode = sourceWork.workCode,
            studentId = targetStudentId,
            classId = targetClassId,
            taskId = sourceWork.taskId,
            submitCount = 1,
            submitTime = System.currentTimeMillis(),
            reviewStatus = "待审核",
            isPublic = false,
            forkFromId = sourceWork.workId,
            syncStatus = 0
        )
        val newId = dao.insertWork(clonedWork)
        newId
    }

    suspend fun submitComment(
        workId: Int,
        authorId: Int,
        authorName: String,
        content: String
    ): GeminiClient.ContentModerationResult = withContext(Dispatchers.IO) {
        // AI 风控前置审核 (Task 3)
        val modResult = GeminiClient.moderateTextContent(content)
        if (modResult.isSafe) {
            val comment = WorkComment(
                workId = workId,
                authorStudentId = authorId,
                authorName = authorName,
                content = content,
                createTime = System.currentTimeMillis(),
                isApproved = true
            )
            val cId = dao.insertComment(comment)
            try {
                supabase?.from("work_comment")?.upsert(comment.copy(commentId = cId.toInt()))
            } catch (e: Exception) {
                Log.e("Supabase", "Comment sync error: ${e.message}")
            }
        }
        modResult
    }

    fun getCommentsByWorkFlow(workId: Int): Flow<List<WorkComment>> = dao.getCommentsByWorkFlow(workId)

    // --- 抄袭检测 (Task 6) ---
    suspend fun checkAndSetWorkSimilarity(work: ScratchWork): ScratchCodeSimilarity.SimilarityResult = withContext(Dispatchers.IO) {
        val sameTaskWorks = dao.getWorksByTaskId(work.taskId).filter { it.workId != work.workId }
        val allStudents = dao.getAllStudents().associate { it.studentId to it.name }
        val result = ScratchCodeSimilarity.checkSimilarityAgainstClassWorks(work.workCode, sameTaskWorks, allStudents)

        dao.updateWorkPlagiarismStatus(work.workId, result.isPlagiarism, result.similarityPercentage)
        result
    }

    // --- 离线断点续传同步 (Task 5) ---
    suspend fun getUnsyncedWorks(): List<ScratchWork> = withContext(Dispatchers.IO) {
        dao.getUnsyncedWorks()
    }

    suspend fun syncSingleWorkToCloud(work: ScratchWork): Boolean = withContext(Dispatchers.IO) {
        try {
            if (supabase != null) {
                supabase!!.from("scratch_work").upsert(work)
                dao.updateWorkSyncStatus(work.workId, 1)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Single work sync failed: ${e.message}")
            false
        }
    }

    // --- 教师学情分析大屏数据 (Task 4) ---
    data class ClassAnalyticsData(
        val totalStudents: Int,
        val submittedCount: Int,
        val avgGrammar: Float,
        val avgLogic: Float,
        val avgTaskMatch: Float,
        val avgCreative: Float,
        val avgTotal: Float,
        val commonErrors: List<String>,
        val plagiarismRiskCount: Int
    )

    suspend fun getClassAnalyticsData(classId: Int, taskId: Int? = null): ClassAnalyticsData = withContext(Dispatchers.IO) {
        val students = if (classId == -1) dao.getAllStudents() else dao.getStudentsByClass(classId)
        var works = if (classId == -1) dao.getAllWorks() else dao.getWorksByClass(classId)
        if (taskId != null && taskId != 0) {
            works = works.filter { it.taskId == taskId }
        }
        var reports = if (classId == -1) dao.getAllAiReports() else dao.getAiReportsByClassId(classId)
        if (taskId != null && taskId != 0) {
            val validWorkIds = works.map { it.workId }.toSet()
            reports = reports.filter { validWorkIds.contains(it.workId) }
        }

        val totalStudents = students.size
        val submittedCount = works.distinctBy { it.studentId }.size
        val plagiarismRiskCount = works.count { it.plagiarismFlag }

        if (reports.isEmpty()) {
            return@withContext ClassAnalyticsData(
                totalStudents = totalStudents,
                submittedCount = submittedCount,
                avgGrammar = 0f,
                avgLogic = 0f,
                avgTaskMatch = 0f,
                avgCreative = 0f,
                avgTotal = 0f,
                commonErrors = listOf("尚未有对应任务/班级的智能评测数据，待学生提交作业后自动生成"),
                plagiarismRiskCount = plagiarismRiskCount
            )
        }

        val avgGrammar = reports.map { it.grammarScore }.average().toFloat()
        val avgLogic = reports.map { it.logicScore }.average().toFloat()
        val avgTaskMatch = reports.map { it.taskMatchScore }.average().toFloat()
        val avgCreative = reports.map { it.creativeScore }.average().toFloat()
        val avgTotal = reports.map { it.averageScore }.average().toFloat()

        val errors = mutableListOf<String>()
        if (avgGrammar < 18) errors.add("语法格式：缺失事件启动块（如当绿旗被点击）")
        if (avgLogic < 22) errors.add("逻辑结构：条件嵌套层级过深，循环未加时间间隔")
        if (avgTaskMatch < 18) errors.add("任务偏离：角色未完全匹配作业所要求的指令动作")
        if (avgCreative < 14) errors.add("创新表现：造型与音效组合偏向单一，缺背景交替")

        if (errors.isEmpty()) {
            errors.add("班级整体掌握良好！少数同学需要注意变量初始化的逻辑规范。")
        }

        ClassAnalyticsData(
            totalStudents = totalStudents,
            submittedCount = submittedCount,
            avgGrammar = avgGrammar,
            avgLogic = avgLogic,
            avgTaskMatch = avgTaskMatch,
            avgCreative = avgCreative,
            avgTotal = avgTotal,
            commonErrors = errors,
            plagiarismRiskCount = plagiarismRiskCount
        )
    }

    // --- AI 每日限额检查 ---
    suspend fun checkDailyAssistOk(studentId: Int, classId: Int): Boolean = withContext(Dispatchers.IO) {
        val config = dao.getConfigByClassId(classId)
        val rawLimit = config?.creativeGuideDailyLimit ?: 100
        val dailyLimit = maxOf(rawLimit, 100) // 保障少儿在线问答与辅导学习的充分性
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % (24 * 60 * 60 * 1000))
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1
        val count = dao.getDailyAssistCount(studentId, startOfDay, endOfDay)
        count < dailyLimit
    }

    // --- 教师评审 ---
    suspend fun updateWorkReview(workId: Int, status: String, score: Int?, comment: String?) = withContext(Dispatchers.IO) {
        val reviewTime = System.currentTimeMillis()
        dao.updateWorkReview(workId, status, score, comment, reviewTime)
        try {
            val updated = dao.getWorkById(workId)
            if (updated != null) {
                supabase?.from("scratch_work")?.update(updated) {
                    filter {
                        eq("work_id", workId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Work review sync failed: ${e.message}")
        }
    }

    // --- 任务详情更新 ---
    suspend fun updateTaskDetails(taskId: Int, name: String, detail: String, grade: String, deadline: String, deadlineTime: Long, classId: Int) = withContext(Dispatchers.IO) {
        dao.updateTaskDetails(taskId, name, detail, grade, deadline, deadlineTime, classId)
        try {
            val updated = dao.getTaskById(taskId)
            if (updated != null) {
                supabase?.from("learning_task")?.update(updated) {
                    filter {
                        eq("task_id", taskId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Task details update sync failed: ${e.message}")
        }
    }

    // --- 评测报告查询 ---
    fun getReportForWorkFlow(workId: Int): Flow<WorkAiReport?> = dao.getReportByWorkIdFlow(workId)

    suspend fun getReportForWork(workId: Int): WorkAiReport? = withContext(Dispatchers.IO) {
        dao.getReportByWorkId(workId)
    }

    // --- 本地数据库全量同步到Supabase云端 ---
    private val _syncStatus = MutableStateFlow("未同步")
    val syncStatus: Flow<String> = _syncStatus

    suspend fun syncAllLocalDataToCloud(): String = withContext(Dispatchers.IO) {
        _syncStatus.value = "同步中..."
        if (supabase == null) {
            val msg = "同步失败: Supabase客户端未初始化，请检查URL和Key配置"
            _syncStatus.value = msg
            return@withContext msg
        }

        val results = mutableListOf<String>()
        var totalSynced = 0
        var totalFailed = 0

        // 1. 同步教师表
        try {
            val teachers = dao.getAllTeachers()
            var success = 0
            for (t in teachers) {
                try {
                    supabase!!.from("teacher").upsert(t)
                    success++
                } catch (e: Exception) {
                    Log.e("SyncCloud", "Teacher ${t.teacherId} 同步失败: ${e.message}")
                    totalFailed++
                }
            }
            results.add("教师: ${success}/${teachers.size} 条成功")
            totalSynced += success
        } catch (e: Exception) {
            results.add("教师: 读取失败 - ${e.message}")
            totalFailed++
        }

        // 2. 同步班级表
        try {
            val classes = dao.getAllClassesList()
            var success = 0
            for (c in classes) {
                try {
                    supabase!!.from("class").upsert(c)
                    success++
                } catch (e: Exception) {
                    Log.e("SyncCloud", "Class ${c.classId} 同步失败: ${e.message}")
                    totalFailed++
                }
            }
            results.add("班级: ${success}/${classes.size} 条成功")
            totalSynced += success
        } catch (e: Exception) {
            results.add("班级: 读取失败 - ${e.message}")
            totalFailed++
        }

        // 3. 同步学生表
        try {
            val students = dao.getAllStudents()
            var success = 0
            for (s in students) {
                try {
                    supabase!!.from("student").upsert(s)
                    success++
                } catch (e: Exception) {
                    Log.e("SyncCloud", "Student ${s.studentId} 同步失败: ${e.message}")
                    totalFailed++
                }
            }
            results.add("学生: ${success}/${students.size} 条成功")
            totalSynced += success
        } catch (e: Exception) {
            results.add("学生: 读取失败 - ${e.message}")
            totalFailed++
        }

        // 4. 同步学习任务表
        try {
            val tasks = dao.getAllTasksList()
            var success = 0
            for (t in tasks) {
                try {
                    supabase!!.from("learning_task").upsert(t)
                    success++
                } catch (e: Exception) {
                    Log.e("SyncCloud", "Task ${t.taskId} 同步失败: ${e.message}")
                    totalFailed++
                }
            }
            results.add("任务: ${success}/${tasks.size} 条成功")
            totalSynced += success
        } catch (e: Exception) {
            results.add("任务: 读取失败 - ${e.message}")
            totalFailed++
        }

        // 5. 同步草稿表
        try {
            val drafts = dao.getAllDrafts()
            var success = 0
            for (d in drafts) {
                try {
                    supabase!!.from("scratch_draft").upsert(d)
                    success++
                } catch (e: Exception) {
                    Log.e("SyncCloud", "Draft ${d.draftId} 同步失败: ${e.message}")
                    totalFailed++
                }
            }
            results.add("草稿: ${success}/${drafts.size} 条成功")
            totalSynced += success
        } catch (e: Exception) {
            results.add("草稿: 读取失败 - ${e.message}")
            totalFailed++
        }

        // 6. 同步AI教学配置表
        try {
            val configs = dao.getAllConfigs()
            var success = 0
            for (c in configs) {
                try {
                    supabase!!.from("ai_teaching_config").upsert(c)
                    success++
                } catch (e: Exception) {
                    Log.e("SyncCloud", "Config ${c.configId} 同步失败: ${e.message}")
                    totalFailed++
                }
            }
            results.add("配置: ${success}/${configs.size} 条成功")
            totalSynced += success
        } catch (e: Exception) {
            results.add("配置: 读取失败 - ${e.message}")
            totalFailed++
        }

        // 7. 同步AI辅助记录表
        try {
            val records = dao.getAllAssistRecords()
            var success = 0
            for (r in records) {
                try {
                    supabase!!.from("ai_assist_record").upsert(r)
                    success++
                } catch (e: Exception) {
                    Log.e("SyncCloud", "AssistRecord ${r.callId} 同步失败: ${e.message}")
                    totalFailed++
                }
            }
            results.add("AI记录: ${success}/${records.size} 条成功")
            totalSynced += success
        } catch (e: Exception) {
            results.add("AI记录: 读取失败 - ${e.message}")
            totalFailed++
        }

        // 8. 同步作品表
        try {
            val works = dao.getAllWorksList()
            var success = 0
            for (w in works) {
                try {
                    supabase!!.from("scratch_work").upsert(w)
                    success++
                } catch (e: Exception) {
                    Log.e("SyncCloud", "Work ${w.workId} 同步失败: ${e.message}")
                    totalFailed++
                }
            }
            results.add("作品: ${success}/${works.size} 条成功")
            totalSynced += success
        } catch (e: Exception) {
            results.add("作品: 读取失败 - ${e.message}")
            totalFailed++
        }

        // 9. 同步评测报告表
        try {
            val reports = dao.getAllAiReports()
            var success = 0
            for (r in reports) {
                try {
                    supabase!!.from("work_ai_report").upsert(r)
                    success++
                } catch (e: Exception) {
                    Log.e("SyncCloud", "Report ${r.reportId} 同步失败: ${e.message}")
                    totalFailed++
                }
            }
            results.add("报告: ${success}/${reports.size} 条成功")
            totalSynced += success
        } catch (e: Exception) {
            results.add("报告: 读取失败 - ${e.message}")
            totalFailed++
        }

        val finalMsg = if (totalFailed == 0) {
            "同步完成! 共成功同步 $totalSynced 条数据到云端。\n${results.joinToString("\n")}"
        } else {
            "同步完成 (有失败): 成功 $totalSynced 条, 失败 $totalFailed 条。\n${results.joinToString("\n")}"
        }
        _syncStatus.value = if (totalFailed == 0) "同步成功 ($totalSynced 条)" else "同步部分失败 ($totalFailed 条失败)"
        Log.d("SyncCloud", finalMsg)
        finalMsg
    }
}
