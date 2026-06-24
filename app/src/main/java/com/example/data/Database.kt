package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. 教师 (Teacher) 与数据库表一一对应
@Entity(tableName = "teacher")
data class Teacher(
    @PrimaryKey(autoGenerate = true) val teacherId: Int = 0,
    @ColumnInfo(name = "workId") val workId: String, // 工号
    @ColumnInfo(name = "name") val name: String, // 姓名
    @ColumnInfo(name = "password") val password: String, // 密码
    @ColumnInfo(name = "createTime") val createTime: Long = System.currentTimeMillis() // 创建时间
)

// 2. 班级 (ClassEntity) 因为 Class 是 Kotlin/Java 关键字，用 ClassEntity 代替，表名依然为 "class"
@Entity(tableName = "class")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val classId: Int = 0,
    @ColumnInfo(name = "className") val className: String, // 班级名称
    @ColumnInfo(name = "grade") val grade: String, // 对应年级
    @ColumnInfo(name = "teacherId") val teacherId: Int, // 教师ID
    @ColumnInfo(name = "createTime") val createTime: Long = System.currentTimeMillis()
)

// 3. 学生 (Student)
@Entity(tableName = "student")
data class Student(
    @PrimaryKey(autoGenerate = true) val studentId: Int = 0,
    @ColumnInfo(name = "studentNumber") val studentNumber: String, // 学号
    @ColumnInfo(name = "name") val name: String, // 姓名
    @ColumnInfo(name = "password") val password: String, // 密码
    @ColumnInfo(name = "classId") val classId: Int, // 班级ID
    @ColumnInfo(name = "registerTime") val registerTime: Long = System.currentTimeMillis() // 注册时间
)

// 4. 学习任务 (LearningTask)
@Entity(tableName = "learning_task")
data class LearningTask(
    @PrimaryKey(autoGenerate = true) val taskId: Int = 0,
    @ColumnInfo(name = "taskName") val taskName: String, // 任务名称
    @ColumnInfo(name = "taskDetail") val taskDetail: String, // 任务详情
    @ColumnInfo(name = "grade") val grade: String, // 对应年级
    @ColumnInfo(name = "deadline") val deadline: String, // 截止时间（可读文本格式）
    @ColumnInfo(name = "deadlineTime") val deadlineTime: Long, // 截止时间时间截
    @ColumnInfo(name = "teacherId") val teacherId: Int, // 发布教师ID
    @ColumnInfo(name = "classId") val classId: Int, // 所属班级ID
    @ColumnInfo(name = "status") val status: String // 发布状态 (如: "未开始", "进行中", "已提交", "已截止")
)

// 5. Scratch草稿 (ScratchDraft)
@Entity(tableName = "scratch_draft")
data class ScratchDraft(
    @PrimaryKey(autoGenerate = true) val draftId: Int = 0,
    @ColumnInfo(name = "draftName") val draftName: String, // 草稿名称
    @ColumnInfo(name = "blockCode") val blockCode: String, // Scratch积木代码内容 (JSON 字符串)
    @ColumnInfo(name = "studentId") val studentId: Int, // 创建学生ID
    @ColumnInfo(name = "taskId") val taskId: Int?, // 关联任务ID（可为空）
    @ColumnInfo(name = "createTime") val createTime: Long = System.currentTimeMillis(), // 创建时间
    @ColumnInfo(name = "lastModifiedTime") val lastModifiedTime: Long = System.currentTimeMillis() // 最后修改时间
)

// 6. AI教学配置 (AiTeachingConfig)
@Entity(tableName = "ai_teaching_config")
data class AiTeachingConfig(
    @PrimaryKey(autoGenerate = true) val configId: Int = 0,
    @ColumnInfo(name = "classId") val classId: Int, // 所属班级ID
    @ColumnInfo(name = "teacherId") val teacherId: Int, // 配置教师ID
    @ColumnInfo(name = "aiHintLevel") val aiHintLevel: String, // AI代码提示等级 ("入门", "进阶", "全能")
    @ColumnInfo(name = "codeGenerationLimit") val codeGenerationLimit: Int, // 完整代码生成限制 (0: 禁用, 1: 启用)
    @ColumnInfo(name = "creativeGuideDailyLimit") val creativeGuideDailyLimit: Int // 创意引导单日上限
)

// 7. AI辅助调用记录 (AiAssistRecord)
@Entity(tableName = "ai_assist_record")
data class AiAssistRecord(
    @PrimaryKey(autoGenerate = true) val callId: Int = 0,
    @ColumnInfo(name = "studentId") val studentId: Int, // 调用学生ID
    @ColumnInfo(name = "classId") val classId: Int, // 所属班级ID
    @ColumnInfo(name = "assistType") val assistType: String, // 辅助功能类型: "语法纠错", "创意引导", "知识点讲解"
    @ColumnInfo(name = "assist_type", defaultValue = "1") val assistTypeInt: Int = 1, // 1=语法纠错，2=创意引导，3=考点讲解
    @ColumnInfo(name = "callTime") val callTime: Long = System.currentTimeMillis(), // 调用时间
    @ColumnInfo(name = "requestContent") val requestContent: String, // 学生请求内容（或输入的问题 / 积木代码段说明）
    @ColumnInfo(name = "aiResult") val aiResult: String, // AI返回结果
    @ColumnInfo(name = "draftId") val draftId: Int? // 关联草稿ID（可为空）
)

// 8. Scratch提交作品 (ScratchWork)
@Entity(tableName = "scratch_work")
data class ScratchWork(
    @PrimaryKey(autoGenerate = true) val workId: Int = 0,
    @ColumnInfo(name = "workName") val workName: String, // 作品名称
    @ColumnInfo(name = "workCode") val workCode: String, // Scratch作品代码 (JSON)
    @ColumnInfo(name = "studentId") val studentId: Int, // 提交学生ID
    @ColumnInfo(name = "classId") val classId: Int, // 所属班级ID
    @ColumnInfo(name = "taskId") val taskId: Int, // 对应任务ID
    @ColumnInfo(name = "submitCount") val submitCount: Int, // 提交次数
    @ColumnInfo(name = "submitTime") val submitTime: Long = System.currentTimeMillis(), // 提交时间
    @ColumnInfo(name = "reviewStatus") val reviewStatus: String, // 审核状态 (如: "待审核", "已评测", "已打分", "打回重做")
    @ColumnInfo(name = "teacherScore") val teacherScore: Int? = null, // 教师评分 (满分100)
    @ColumnInfo(name = "teacherComment") val teacherComment: String? = null, // 教师评语
    @ColumnInfo(name = "teacherReviewTime") val teacherReviewTime: Long? = null // 评价时间
)

// 9. 作品AI评测报告 (WorkAiReport)
@Entity(tableName = "work_ai_report")
data class WorkAiReport(
    @PrimaryKey(autoGenerate = true) val reportId: Int = 0,
    @ColumnInfo(name = "workId") val workId: Int, // 对应作品ID
    @ColumnInfo(name = "studentId") val studentId: Int, // 提交学生ID
    @ColumnInfo(name = "grammarScore") val grammarScore: Int, // 语法得分 (满分25)
    @ColumnInfo(name = "logicScore") val logicScore: Int, // 逻辑得分 (满分30)
    @ColumnInfo(name = "taskMatchScore") val taskMatchScore: Int, // 任务匹配度得分 (满分25)
    @ColumnInfo(name = "creativeScore") val creativeScore: Int, // 创意得分 (满分20)
    @ColumnInfo(name = "averageScore") val averageScore: Int, // 综合得分 (以上四项加和，满分100)
    @ColumnInfo(name = "optimizationSuggestions") val optimizationSuggestions: String, // 优化建议与知识点补漏指引
    @ColumnInfo(name = "reportTime") val reportTime: Long = System.currentTimeMillis() // 评测时间
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

    @Query("SELECT * FROM class")
    fun getAllClassesFlow(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM class WHERE teacherId = :teacherId")
    suspend fun getClassesByTeacher(teacherId: Int): List<ClassEntity>

    @Query("SELECT * FROM class WHERE classId = :classId LIMIT 1")
    suspend fun getClassById(classId: Int): ClassEntity?

    @Query("DELETE FROM class WHERE classId = :classId")
    suspend fun deleteClassById(classId: Int)

    @Query("DELETE FROM student WHERE classId = :classId")
    suspend fun deleteStudentsByClass(classId: Int)

    @Query("DELETE FROM learning_task WHERE classId = :classId")
    suspend fun deleteTasksByClass(classId: Int)

    @Query("UPDATE class SET className = :className, grade = :grade WHERE classId = :classId")
    suspend fun updateClass(classId: Int, className: String, grade: String)

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

    @Query("SELECT * FROM ai_assist_record WHERE studentId = :studentId AND assist_type = :type ORDER BY callTime DESC")
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

    @Query("SELECT * FROM student")
    fun getAllStudentsFlow(): Flow<List<Student>>

    @Query("SELECT * FROM student")
    suspend fun getAllStudents(): List<Student>

    @Query("SELECT * FROM learning_task")
    suspend fun getAllTasksList(): List<LearningTask>

    @Query("UPDATE scratch_work SET reviewStatus = :status, teacherScore = :score, teacherComment = :comment, teacherReviewTime = :reviewTime WHERE workId = :workId")
    suspend fun updateWorkReview(workId: Int, status: String, score: Int?, comment: String?, reviewTime: Long)

    // --- AI 评测报告 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiReport(report: WorkAiReport): Long

    @Query("SELECT * FROM work_ai_report WHERE workId = :workId LIMIT 1")
    fun getReportByWorkIdFlow(workId: Int): Flow<WorkAiReport?>

    @Query("SELECT * FROM work_ai_report WHERE workId = :workId LIMIT 1")
    suspend fun getReportByWorkId(workId: Int): WorkAiReport?
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
        WorkAiReport::class
    ],
    version = 3,
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
