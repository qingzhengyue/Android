package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 1. 教师 (Teacher) 与数据库表一一对应
@Serializable
@Entity(tableName = "teacher")
data class Teacher(
    @PrimaryKey(autoGenerate = true) 
    @SerialName("teacher_id")
    val teacherId: Int = 0,
    @ColumnInfo(name = "workId") 
    @SerialName("work_id")
    val workId: String, // 工号
    @ColumnInfo(name = "name") 
    @SerialName("name")
    val name: String, // 姓名
    @ColumnInfo(name = "password") 
    @SerialName("password")
    val password: String, // 密码
    @ColumnInfo(name = "createTime") 
    @SerialName("create_time")
    val createTime: Long = System.currentTimeMillis() // 创建时间
)

// 2. 班级 (ClassEntity) 因为 Class 是 Kotlin/Java 关键字，用 ClassEntity 代替，表名依然为 "class"
@Serializable
@Entity(tableName = "class")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) 
    @SerialName("class_id")
    val classId: Int = 0,
    @ColumnInfo(name = "className") 
    @SerialName("class_name")
    val className: String, // 班级名称
    @ColumnInfo(name = "grade") 
    @SerialName("grade")
    val grade: String, // 对应年级
    @ColumnInfo(name = "teacherId") 
    @SerialName("teacher_id")
    val teacherId: Int, // 教师ID
    @ColumnInfo(name = "createTime") 
    @SerialName("create_time")
    val createTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isActive", defaultValue = "1")
    @SerialName("is_active")
    val isActive: Boolean = true // 是否启用（软删除标记）
)

// 3. 学生 (Student)
@Serializable
@Entity(tableName = "student")
data class Student(
    @PrimaryKey(autoGenerate = true) 
    @SerialName("student_id")
    val studentId: Int = 0,
    @ColumnInfo(name = "studentNumber") 
    @SerialName("student_number")
    val studentNumber: String, // 学号
    @ColumnInfo(name = "name") 
    @SerialName("name")
    val name: String, // 姓名
    @ColumnInfo(name = "password") 
    @SerialName("password")
    val password: String, // 密码
    @ColumnInfo(name = "classId") 
    @SerialName("class_id")
    val classId: Int, // 班级ID
    @ColumnInfo(name = "registerTime") 
    @SerialName("register_time")
    val registerTime: Long = System.currentTimeMillis() // 注册时间
)

// 4. 学习任务 (LearningTask)
@Serializable
@Entity(tableName = "learning_task")
data class LearningTask(
    @PrimaryKey(autoGenerate = true) 
    @SerialName("task_id")
    val taskId: Int = 0,
    @ColumnInfo(name = "taskName") 
    @SerialName("task_name")
    val taskName: String, // 任务名称
    @ColumnInfo(name = "taskDetail") 
    @SerialName("task_detail")
    val taskDetail: String, // 任务详情
    @ColumnInfo(name = "grade") 
    @SerialName("grade")
    val grade: String, // 对应年级
    @ColumnInfo(name = "deadline") 
    @SerialName("deadline")
    val deadline: String, // 截止时间（可读文本格式）
    @ColumnInfo(name = "deadlineTime") 
    @SerialName("deadline_time")
    val deadlineTime: Long, // 截止时间时间截
    @ColumnInfo(name = "teacherId") 
    @SerialName("teacher_id")
    val teacherId: Int, // 发布教师ID
    @ColumnInfo(name = "classId") 
    @SerialName("class_id")
    val classId: Int, // 所属班级ID
    @ColumnInfo(name = "status") 
    @SerialName("status")
    val status: String // 发布状态 (如: "未开始", "进行中", "已提交", "已截止")
)

fun LearningTask.isExpired(): Boolean {
    val currentTime = System.currentTimeMillis()
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = sdf.parse(this.deadline)
        if (date != null) {
            val cal = java.util.Calendar.getInstance()
            cal.time = date
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
            cal.set(java.util.Calendar.MINUTE, 59)
            cal.set(java.util.Calendar.SECOND, 59)
            cal.timeInMillis < currentTime
        } else {
            this.deadlineTime < currentTime
        }
    } catch (e: Exception) {
        this.deadlineTime < currentTime
    }
}

fun LearningTask.getDisplayStatus(): String {
    return if (this.isExpired()) "已截止" else if (this.status.isBlank() || this.status == "进行中") "进行中" else this.status
}

// 5. Scratch草稿 (ScratchDraft)
@Serializable
@Entity(tableName = "scratch_draft")
data class ScratchDraft(
    @PrimaryKey(autoGenerate = true) 
    @SerialName("draft_id")
    val draftId: Int = 0,
    @ColumnInfo(name = "draftName") 
    @SerialName("draft_name")
    val draftName: String, // 草稿名称
    @ColumnInfo(name = "blockCode") 
    @SerialName("block_code")
    val blockCode: String, // Scratch积木代码内容 (JSON 字符串)
    @ColumnInfo(name = "studentId") 
    @SerialName("student_id")
    val studentId: Int, // 创建学生ID
    @ColumnInfo(name = "taskId") 
    @SerialName("task_id")
    val taskId: Int?, // 关联任务ID（可为空）
    @ColumnInfo(name = "createTime") 
    @SerialName("create_time")
    val createTime: Long = System.currentTimeMillis(), // 创建时间
    @ColumnInfo(name = "lastModifiedTime") 
    @SerialName("last_modified_time")
    val lastModifiedTime: Long = System.currentTimeMillis() // 最后修改时间
)

// 6. AI教学配置 (AiTeachingConfig)
@Serializable
@Entity(tableName = "ai_teaching_config")
data class AiTeachingConfig(
    @PrimaryKey(autoGenerate = true) 
    @SerialName("config_id")
    val configId: Int = 0,
    @ColumnInfo(name = "classId") 
    @SerialName("class_id")
    val classId: Int, // 所属班级ID
    @ColumnInfo(name = "teacherId") 
    @SerialName("teacher_id")
    val teacherId: Int, // 配置教师ID
    @ColumnInfo(name = "aiHintLevel") 
    @SerialName("ai_hint_level")
    val aiHintLevel: String, // AI代码提示等级 ("入门", "进阶", "全能")
    @ColumnInfo(name = "codeGenerationLimit") 
    @SerialName("code_generation_limit")
    val codeGenerationLimit: Int, // 完整代码生成限制 (0: 禁用, 1: 启用)
    @ColumnInfo(name = "creativeGuideDailyLimit") 
    @SerialName("creative_guide_daily_limit")
    val creativeGuideDailyLimit: Int // 创意引导单日上限
)

// 7. AI辅助调用记录 (AiAssistRecord)
@Serializable
@Entity(tableName = "ai_assist_record")
data class AiAssistRecord(
    @PrimaryKey(autoGenerate = true) 
    @SerialName("call_id")
    val callId: Int = 0,
    @ColumnInfo(name = "studentId") 
    @SerialName("student_id")
    val studentId: Int, // 调用学生ID
    @ColumnInfo(name = "classId") 
    @SerialName("class_id")
    val classId: Int, // 所属班级ID
    @ColumnInfo(name = "assistType") 
    @SerialName("assist_type")
    val assistType: String, // 辅助功能类型: "语法纠错", "创意引导", "知识点讲解"
    @ColumnInfo(name = "assist_type_int", defaultValue = "1") 
    @SerialName("assist_type_int")
    val assistTypeInt: Int = 1, // 1=语法纠错，2=创意引导，3=考点讲解
    @ColumnInfo(name = "callTime") 
    @SerialName("call_time")
    val callTime: Long = System.currentTimeMillis(), // 调用时间
    @ColumnInfo(name = "requestContent") 
    @SerialName("request_content")
    val requestContent: String, // 学生请求内容（或输入的问题 / 积木代码段说明）
    @ColumnInfo(name = "aiResult") 
    @SerialName("ai_result")
    val aiResult: String, // AI返回结果
    @ColumnInfo(name = "draftId") 
    @SerialName("draft_id")
    val draftId: Int?, // 关联草稿ID（可为空）
    @ColumnInfo(name = "sessionId", defaultValue = "") 
    @SerialName("session_id")
    val sessionId: String = "" // 关联 AI 辅导会话 ID
)

// 8. Scratch提交作品 (ScratchWork)
@Serializable
@Entity(tableName = "scratch_work")
data class ScratchWork(
    @PrimaryKey(autoGenerate = true) 
    @SerialName("work_id")
    val workId: Int = 0,
    @ColumnInfo(name = "workName") 
    @SerialName("work_name")
    val workName: String, // 作品名称
    @ColumnInfo(name = "workCode") 
    @SerialName("work_code")
    val workCode: String, // Scratch作品代码 (JSON)
    @ColumnInfo(name = "studentId") 
    @SerialName("student_id")
    val studentId: Int, // 提交学生ID
    @ColumnInfo(name = "classId") 
    @SerialName("class_id")
    val classId: Int, // 所属班级ID
    @ColumnInfo(name = "taskId") 
    @SerialName("task_id")
    val taskId: Int, // 对应任务ID
    @ColumnInfo(name = "submitCount") 
    @SerialName("submit_count")
    val submitCount: Int, // 提交次数
    @ColumnInfo(name = "submitTime") 
    @SerialName("submit_time")
    val submitTime: Long = System.currentTimeMillis(), // 提交时间
    @ColumnInfo(name = "reviewStatus") 
    @SerialName("review_status")
    val reviewStatus: String, // 审核状态 (如: "待审核", "已评测", "已打分", "打回重做")
    @ColumnInfo(name = "teacherScore") 
    @SerialName("teacher_score")
    val teacherScore: Int? = null, // 教师评分 (满分100)
    @ColumnInfo(name = "teacherComment") 
    @SerialName("teacher_comment")
    val teacherComment: String? = null, // 教师评语
    @ColumnInfo(name = "teacherReviewTime") 
    @SerialName("teacher_review_time")
    val teacherReviewTime: Long? = null, // 评价时间
    @ColumnInfo(name = "isPublic", defaultValue = "0")
    @SerialName("is_public")
    val isPublic: Boolean = false, // 是否发布到开源大厅
    @ColumnInfo(name = "forkFromId")
    @SerialName("fork_from_id")
    val forkFromId: Int? = null, // 源作品ID (克隆关联)
    @ColumnInfo(name = "likesCount", defaultValue = "0")
    @SerialName("likes_count")
    val likesCount: Int = 0, // 点赞数
    @ColumnInfo(name = "syncStatus", defaultValue = "1")
    @SerialName("sync_status")
    val syncStatus: Int = 1, // 0 = 待同步, 1 = 已同步
    @ColumnInfo(name = "plagiarismFlag", defaultValue = "0")
    @SerialName("plagiarism_flag")
    val plagiarismFlag: Boolean = false, // 是否标记为疑似抄袭
    @ColumnInfo(name = "similarityScore", defaultValue = "0")
    @SerialName("similarity_score")
    val similarityScore: Int = 0 // 相似度百分比
)

// 8.5. 作品互动评论 (WorkComment)
@Serializable
@Entity(tableName = "work_comment")
data class WorkComment(
    @PrimaryKey(autoGenerate = true)
    @SerialName("comment_id")
    val commentId: Int = 0,
    @ColumnInfo(name = "workId")
    @SerialName("work_id")
    val workId: Int,
    @ColumnInfo(name = "authorStudentId")
    @SerialName("author_student_id")
    val authorStudentId: Int,
    @ColumnInfo(name = "authorName")
    @SerialName("author_name")
    val authorName: String,
    @ColumnInfo(name = "content")
    @SerialName("content")
    val content: String,
    @ColumnInfo(name = "createTime")
    @SerialName("create_time")
    val createTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isApproved", defaultValue = "1")
    @SerialName("is_approved")
    val isApproved: Boolean = true
)

// 9. 作品AI评测报告 (WorkAiReport)
@Serializable
@Entity(tableName = "work_ai_report")
data class WorkAiReport(
    @PrimaryKey(autoGenerate = true) 
    @SerialName("report_id")
    val reportId: Int = 0,
    @ColumnInfo(name = "workId") 
    @SerialName("work_id")
    val workId: Int, // 对应作品ID
    @ColumnInfo(name = "studentId") 
    @SerialName("student_id")
    val studentId: Int, // 提交学生ID
    @ColumnInfo(name = "grammarScore") 
    @SerialName("grammar_score")
    val grammarScore: Int, // 语法得分 (满分25)
    @ColumnInfo(name = "logicScore") 
    @SerialName("logic_score")
    val logicScore: Int, // 逻辑得分 (满分30)
    @ColumnInfo(name = "taskMatchScore") 
    @SerialName("task_match_score")
    val taskMatchScore: Int, // 任务匹配度得分 (满分25)
    @ColumnInfo(name = "creativeScore") 
    @SerialName("creative_score")
    val creativeScore: Int, // 创意得分 (满分20)
    @ColumnInfo(name = "averageScore") 
    @SerialName("average_score")
    val averageScore: Int, // 综合得分 (以上四项加和，满分100)
    @ColumnInfo(name = "optimizationSuggestions") 
    @SerialName("optimization_suggestions")
    val optimizationSuggestions: String, // 优化建议与知识点补漏指引
    @ColumnInfo(name = "reportTime") 
    @SerialName("report_time")
    val reportTime: Long = System.currentTimeMillis() // 评测时间
)

// 10. 作品与评测报告联合查询结果
data class WorkWithReport(
    @Embedded val work: ScratchWork,
    @Relation(parentColumn = "workId", entityColumn = "workId") val report: WorkAiReport?
)

// 11. 作品点赞记录 (WorkLikeEntity)
@Serializable
@Entity(
    tableName = "work_likes",
    primaryKeys = ["workId", "studentId"] // 联合主键，确保唯一性
)
data class WorkLikeEntity(
    @ColumnInfo(name = "workId")
    @SerialName("work_id")
    val workId: Int,
    @ColumnInfo(name = "studentId")
    @SerialName("student_id")
    val studentId: String, // 当前登录学生的学号/ID
    @ColumnInfo(name = "createTime")
    @SerialName("create_time")
    val createTime: Long = System.currentTimeMillis()
)

@Dao
interface AppDao {
    // --- 教师操作 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher): Long

    @Query("SELECT * FROM teacher WHERE workId = :workId LIMIT 1")
    suspend fun getTeacherByWorkId(workId: String): Teacher?

    @Query("SELECT * FROM teacher WHERE teacherId = :id LIMIT 1")
    suspend fun getTeacherById(id: Int): Teacher?

    // --- 班级操作 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classEntity: ClassEntity): Long

    @Query("SELECT * FROM class WHERE isActive = 1")
    fun getAllClassesFlow(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM class WHERE teacherId = :teacherId AND isActive = 1")
    suspend fun getClassesByTeacher(teacherId: Int): List<ClassEntity>

    @Query("SELECT * FROM class WHERE classId = :classId LIMIT 1")
    suspend fun getClassById(classId: Int): ClassEntity?

    // 查询已禁用的班级（用于恢复）
    @Query("SELECT * FROM class WHERE className = :className AND grade = :grade AND isActive = 0 LIMIT 1")
    suspend fun getDisabledClassByName(className: String, grade: String): ClassEntity?

    // 软删除：将 isActive 设为 false，而不是物理删除
    @Query("UPDATE class SET isActive = 0 WHERE classId = :classId")
    suspend fun softDeleteClassById(classId: Int)

    // 恢复已禁用的班级：将 isActive 设回 true
    @Query("UPDATE class SET isActive = 1 WHERE classId = :classId")
    suspend fun restoreClassById(classId: Int)

    @Query("DELETE FROM student WHERE classId = :classId")
    suspend fun deleteStudentsByClass(classId: Int)

    @Query("DELETE FROM learning_task WHERE classId = :classId")
    suspend fun deleteTasksByClass(classId: Int)

    @Query("DELETE FROM scratch_work WHERE classId = :classId")
    suspend fun deleteWorksByClass(classId: Int)

    @Query("UPDATE class SET className = :className, grade = :grade WHERE classId = :classId")
    suspend fun updateClass(classId: Int, className: String, grade: String)
    
    // 检查同名班级是否存在（仅检查启用的班级）
    @Query("SELECT COUNT(*) FROM class WHERE className = :className AND grade = :grade AND isActive = 1 AND classId != :excludeClassId")
    suspend fun checkDuplicateClass(className: String, grade: String, excludeClassId: Int): Int

    // --- 学生操作 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Query("UPDATE student SET password = :newPass WHERE studentId = :studentId")
    suspend fun updateStudentPassword(studentId: Int, newPass: String)

    @Query("SELECT * FROM student WHERE studentNumber = :studentNumber LIMIT 1")
    suspend fun getStudentByNumber(studentNumber: String): Student?

    @Query("SELECT * FROM student WHERE studentId = :studentId LIMIT 1")
    suspend fun getStudentById(studentId: Int): Student?

    @Query("SELECT * FROM student WHERE classId = :classId")
    suspend fun getStudentsByClass(classId: Int): List<Student>

    // --- 教学配置 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AiTeachingConfig): Long

    @Query("SELECT * FROM ai_teaching_config WHERE classId = :classId LIMIT 1")
    suspend fun getConfigByClassId(classId: Int): AiTeachingConfig?

    @Query("SELECT * FROM ai_teaching_config WHERE classId = :classId LIMIT 1")
    fun getConfigByClassIdFlow(classId: Int): Flow<AiTeachingConfig?>

    // --- 学习任务 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: LearningTask): Long

    @Query("SELECT * FROM learning_task ORDER BY deadlineTime ASC")
    fun getAllTasksFlow(): Flow<List<LearningTask>>

    @Query("SELECT * FROM learning_task WHERE classId = :classId ORDER BY deadlineTime ASC")
    fun getTasksByClassFlow(classId: Int): Flow<List<LearningTask>>

    @Query("SELECT * FROM learning_task WHERE taskId = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Int): LearningTask?

    @Query("UPDATE learning_task SET status = :status WHERE taskId = :taskId")
    suspend fun updateTaskStatus(taskId: Int, status: String)

    @Query("UPDATE learning_task SET taskName = :name, taskDetail = :detail, grade = :grade, deadline = :deadline, deadlineTime = :deadlineTime, classId = :classId WHERE taskId = :taskId")
    suspend fun updateTaskDetails(taskId: Int, name: String, detail: String, grade: String, deadline: String, deadlineTime: Long, classId: Int)

    // --- 草稿操作 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: ScratchDraft): Long

    @Query("SELECT * FROM scratch_draft WHERE studentId = :studentId ORDER BY lastModifiedTime DESC")
    suspend fun getDraftsByStudentDirect(studentId: Int): List<ScratchDraft>

    @Query("SELECT * FROM scratch_draft WHERE studentId = :studentId ORDER BY lastModifiedTime DESC")
    fun getDraftsByStudentFlow(studentId: Int): Flow<List<ScratchDraft>>

    @Query("SELECT * FROM scratch_draft WHERE draftId = :draftId LIMIT 1")
    suspend fun getDraftById(draftId: Int): ScratchDraft?

    @Query("DELETE FROM scratch_draft WHERE draftId = :draftId")
    suspend fun deleteDraftById(draftId: Int)

    // --- AI 辅助调用记录 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssistRecord(record: AiAssistRecord): Long

    @Query("SELECT * FROM ai_assist_record WHERE studentId = :studentId ORDER BY callTime DESC")
    fun getAssistRecordsByStudentFlow(studentId: Int): Flow<List<AiAssistRecord>>

    @Query("SELECT * FROM ai_assist_record WHERE studentId = :studentId AND assist_type_int = :type ORDER BY callTime DESC")
    fun getAssistRecordsByStudentAndTypeFlow(studentId: Int, type: Int): Flow<List<AiAssistRecord>>

    @Query("SELECT COUNT(*) FROM ai_assist_record WHERE studentId = :studentId AND callTime >= :startOfDay AND callTime <= :endOfDay")
    suspend fun getDailyAssistCount(studentId: Int, startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM ai_assist_record WHERE classId = :classId")
    suspend fun getAiAssistCountByClass(classId: Int): Int

    @Query("DELETE FROM work_ai_report WHERE workId IN (SELECT workId FROM scratch_work WHERE taskId = :taskId)")
    suspend fun deleteAiReportsByTaskId(taskId: Int)

    @Query("DELETE FROM scratch_work WHERE taskId = :taskId")
    suspend fun deleteWorksByTaskId(taskId: Int)

    @Query("DELETE FROM learning_task WHERE taskId = :taskId")
    suspend fun deleteTaskById(taskId: Int)

    // --- 作品提交 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWork(work: ScratchWork): Long

    @Query("SELECT * FROM scratch_work WHERE studentId = :studentId ORDER BY submitTime DESC")
    fun getWorksByStudentFlow(studentId: Int): Flow<List<ScratchWork>>

    @Query("SELECT * FROM scratch_work WHERE taskId = :taskId AND studentId = :studentId LIMIT 1")
    suspend fun getWorkByStudentAndTask(studentId: Int, taskId: Int): ScratchWork?

    @Query("SELECT * FROM scratch_work WHERE workId = :workId LIMIT 1")
    suspend fun getWorkById(workId: Int): ScratchWork?

    @Query("SELECT * FROM scratch_work WHERE classId = :classId ORDER BY submitTime DESC")
    suspend fun getWorksByClass(classId: Int): List<ScratchWork>

    @Query("SELECT * FROM scratch_work ORDER BY submitTime DESC")
    fun getAllWorksFlow(): Flow<List<ScratchWork>>

    @Query("SELECT * FROM scratch_work ORDER BY submitTime DESC")
    suspend fun getAllWorks(): List<ScratchWork>

    @Query("SELECT * FROM student")
    fun getAllStudentsFlow(): Flow<List<Student>>

    @Query("SELECT * FROM teacher")
    suspend fun getAllTeachers(): List<Teacher>

    @Query("SELECT * FROM class")
    suspend fun getAllClassesList(): List<ClassEntity>

    @Query("SELECT * FROM student")
    suspend fun getAllStudents(): List<Student>

    @Query("SELECT * FROM learning_task")
    suspend fun getAllTasksList(): List<LearningTask>

    @Query("SELECT * FROM scratch_draft")
    suspend fun getAllDrafts(): List<ScratchDraft>

    @Query("SELECT * FROM ai_teaching_config")
    suspend fun getAllConfigs(): List<AiTeachingConfig>

    @Query("SELECT * FROM ai_assist_record")
    suspend fun getAllAssistRecords(): List<AiAssistRecord>

    @Query("SELECT * FROM scratch_work")
    suspend fun getAllWorksList(): List<ScratchWork>

    @Query("SELECT * FROM work_ai_report")
    suspend fun getAllAiReports(): List<WorkAiReport>

    @Query("UPDATE scratch_work SET reviewStatus = :status, teacherScore = :score, teacherComment = :comment, teacherReviewTime = :reviewTime WHERE workId = :workId")
    suspend fun updateWorkReview(workId: Int, status: String, score: Int?, comment: String?, reviewTime: Long)

    // --- 开源大厅与同伴互评 (Open Hall & Peer Comments) ---
    @Query("SELECT * FROM scratch_work WHERE isPublic = 1 ORDER BY submitTime DESC")
    fun getPublicWorksFlow(): Flow<List<ScratchWork>>

    @Query("SELECT * FROM scratch_work WHERE isPublic = 1 ORDER BY likesCount DESC, submitTime DESC")
    fun getPopularPublicWorksFlow(): Flow<List<ScratchWork>>

    @Query("UPDATE scratch_work SET isPublic = :isPublic WHERE workId = :workId")
    suspend fun updateWorkPublicStatus(workId: Int, isPublic: Boolean)

    @Query("UPDATE scratch_work SET likesCount = likesCount + 1 WHERE workId = :workId")
    suspend fun incrementWorkLikes(workId: Int)

    // --- 点赞记录操作 (Work Like DAO) ---
    @Query("SELECT COUNT(*) FROM work_likes WHERE workId = :workId AND studentId = :studentId")
    suspend fun checkIsLiked(workId: Int, studentId: String): Int

    @Query("SELECT workId FROM work_likes WHERE studentId = :studentId")
    fun getLikedWorkIdsFlow(studentId: String): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLike(like: WorkLikeEntity)

    @Query("DELETE FROM work_likes WHERE workId = :workId AND studentId = :studentId")
    suspend fun deleteLike(workId: Int, studentId: String)

    @Query("UPDATE scratch_work SET likesCount = likesCount + :delta WHERE workId = :workId")
    suspend fun updateWorkLikeCount(workId: Int, delta: Int)

    @Query("UPDATE scratch_work SET plagiarismFlag = :flag, similarityScore = :similarity WHERE workId = :workId")
    suspend fun updateWorkPlagiarismStatus(workId: Int, flag: Boolean, similarity: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: WorkComment): Long

    @Query("SELECT * FROM work_comment WHERE workId = :workId AND isApproved = 1 ORDER BY createTime DESC")
    fun getCommentsByWorkFlow(workId: Int): Flow<List<WorkComment>>

    @Query("SELECT * FROM work_comment WHERE workId = :workId AND isApproved = 1 ORDER BY createTime DESC")
    suspend fun getCommentsByWork(workId: Int): List<WorkComment>

    // --- 离线同步 (Offline Sync) ---
    @Query("SELECT * FROM scratch_work WHERE syncStatus = 0")
    suspend fun getUnsyncedWorks(): List<ScratchWork>

    @Query("UPDATE scratch_work SET syncStatus = :status WHERE workId = :workId")
    suspend fun updateWorkSyncStatus(workId: Int, status: Int)

    // --- 教师学情分析 (Analytics) ---
    @Query("SELECT * FROM work_ai_report WHERE studentId IN (SELECT studentId FROM student WHERE classId = :classId)")
    suspend fun getAiReportsByClassId(classId: Int): List<WorkAiReport>

    @Query("SELECT * FROM scratch_work WHERE taskId = :taskId")
    suspend fun getWorksByTaskId(taskId: Int): List<ScratchWork>

    // --- AI 评测报告 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiReport(report: WorkAiReport): Long

    @Query("SELECT * FROM work_ai_report WHERE workId = :workId LIMIT 1")
    fun getReportByWorkIdFlow(workId: Int): Flow<WorkAiReport?>

    @Query("SELECT * FROM work_ai_report WHERE workId = :workId LIMIT 1")
    suspend fun getReportByWorkId(workId: Int): WorkAiReport?

    // --- 作品与报告联合查询 ---
    @Transaction
    @Query("SELECT * FROM scratch_work WHERE studentId = :studentId AND taskId = :taskId LIMIT 1")
    fun getWorkWithReportFlow(studentId: Int, taskId: Int): Flow<WorkWithReport?>
}

@Database(
    entities = [
        Teacher::class,
        ClassEntity::class,
        Student::class,
        LearningTask::class,
        ScratchDraft::class,
        AiTeachingConfig::class,
        AiAssistRecord::class,
        ScratchWork::class,
        WorkComment::class,
        WorkAiReport::class,
        WorkLikeEntity::class
    ],
    version = 7, // 升级到版本7，添加 AI 辅导会话 sessionId 隔离支持
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val appDao: AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scratch_ai_teaching.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Serializable
data class ScratchWorkInsertDto(
    @SerialName("work_name")
    val workName: String,
    @SerialName("work_code")
    val workCode: String,
    @SerialName("student_id")
    val studentId: Int,
    @SerialName("class_id")
    val classId: Int,
    @SerialName("task_id")
    val taskId: Int,
    @SerialName("submit_count")
    val submitCount: Int,
    @SerialName("review_status")
    val reviewStatus: String
)

@Serializable
data class WorkAiReportInsertDto(
    @SerialName("work_id") val workId: Int,
    @SerialName("student_id") val studentId: Int,
    @SerialName("grammar_score") val grammarScore: Int,
    @SerialName("logic_score") val logicScore: Int,
    @SerialName("task_match_score") val taskMatchScore: Int,
    @SerialName("creative_score") val creativeScore: Int,
    @SerialName("average_score") val averageScore: Int,
    @SerialName("optimization_suggestions") val optimizationSuggestions: String
)
