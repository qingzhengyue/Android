package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabasePrepopulator {

    suspend fun populateIfEmpty(db: AppDatabase) = withContext(Dispatchers.IO) {
        val dao = db.appDao

        // 1. 检查教师是否存在，若不存在则插入
        val existingTeacher = dao.getTeacherByWorkId("T1001")
        if (existingTeacher == null) {
            val teacherId = dao.insertTeacher(
                Teacher(
                    workId = "T1001",
                    name = "王老师",
                    password = "123456" // 示例简单存储
                )
            ).toInt()

            // 2. 插入班级
            val classId1 = dao.insertClass(
                ClassEntity(
                    className = "三年级一班",
                    grade = "三年级",
                    teacherId = teacherId
                )
            ).toInt()

            val classId2 = dao.insertClass(
                ClassEntity(
                    className = "四年级二班",
                    grade = "四年级",
                    teacherId = teacherId
                )
            ).toInt()

            // 3. 插入学生 (多级班级学生示例数据)
            val studentId1 = dao.insertStudent(
                Student(
                    studentNumber = "S2001",
                    name = "张小帅",
                    password = "123456",
                    classId = classId1
                )
            ).toInt()

            val studentId2 = dao.insertStudent(
                Student(
                    studentNumber = "S2002",
                    name = "李小美",
                    password = "123456",
                    classId = classId1
                )
            ).toInt()

            val studentId3 = dao.insertStudent(
                Student(
                    studentNumber = "S2003",
                    name = "周杰伦",
                    password = "123456",
                    classId = classId1
                )
            ).toInt()

            val studentId4 = dao.insertStudent(
                Student(
                    studentNumber = "S2004",
                    name = "蔡徐坤",
                    password = "123456",
                    classId = classId1
                )
            ).toInt()

            val studentId5 = dao.insertStudent(
                Student(
                    studentNumber = "S2005",
                    name = "谷爱凌",
                    password = "123456",
                    classId = classId1
                )
            ).toInt()

            val studentId6 = dao.insertStudent(
                Student(
                    studentNumber = "S3001",
                    name = "王小飞",
                    password = "123456",
                    classId = classId2
                )
            ).toInt()

            val studentId7 = dao.insertStudent(
                Student(
                    studentNumber = "S3002",
                    name = "赵丽颖",
                    password = "123456",
                    classId = classId2
                )
            ).toInt()

            val studentId8 = dao.insertStudent(
                Student(
                    studentNumber = "S3003",
                    name = "易烊千玺",
                    password = "123456",
                    classId = classId2
                )
            ).toInt()

            // 4. 插入 AI 教学配置 (ai_teaching_config)
            dao.insertConfig(
                AiTeachingConfig(
                    classId = classId1,
                    teacherId = teacherId,
                    aiHintLevel = "入门级",
                    codeGenerationLimit = 0, // 限制全套完整代码
                    creativeGuideDailyLimit = 5
                )
            )

            dao.insertConfig(
                AiTeachingConfig(
                    classId = classId2,
                    teacherId = teacherId,
                    aiHintLevel = "进阶级",
                    codeGenerationLimit = 1, // 可演示部分案例
                    creativeGuideDailyLimit = 8
                )
            )

            // 5. 插入常规 Scratch 学习任务
            val now = System.currentTimeMillis()
            val thirtyDaysMs = 30 * 24 * 60 * 60 * 1000L

            val taskId1 = dao.insertTask(
                LearningTask(
                    taskName = "猫咪漫步游戏 (左右弹跳)",
                    taskDetail = "小猫漫步活动是 Scratch 必修基础。要求在舞台放置小猫角色，使其启动后一直面向前方走动，碰到边缘后反弹回来。你必须学习使用【重复执行】、【移动 10 步】、【碰到边缘反弹】和【旋转方式设为左右翻转】这四个核心积木，并配上一张漂亮的海洋或森林舞台背景。",
                    grade = "三年级",
                    deadline = "2026-06-30",
                    deadlineTime = now + thirtyDaysMs,
                    teacherId = teacherId,
                    classId = classId1,
                    status = "进行中"
                )
            ).toInt()

            val taskId2 = dao.insertTask(
                LearningTask(
                    taskName = "水果大作战 - 接水果趣味小游戏",
                    taskDetail = "设计一个接糖果或者接苹果的捕获类游戏。苹果在上方产生随机的 X 轴并以一个速度向下坠落，玩家使用键盘左右方向键控制碗（Bowl）左右移动接住掉落的水果。设计加分点：如果碗接到水果则播放 Pop 音效、分数变量加 1，并重置水果位置到最上方。",
                    grade = "三年级",
                    deadline = "2026-07-15",
                    deadlineTime = now + (thirtyDaysMs * 2),
                    teacherId = teacherId,
                    classId = classId1,
                    status = "进行中"
                )
            ).toInt()

            val taskId3 = dao.insertTask(
                LearningTask(
                    taskName = "走迷宫 (碰到黑色反弹)",
                    taskDetail = "制作一个经典的键盘控方向‘走迷宫逃跑’游戏。画笔绘制深色迷宫围墙边线颜色。操作甲虫或小恐龙在迷宫内前进。重点编程逻辑：如果玩家操控角色碰到了‘黑色的迷宫围墙边线颜色’，则执行角色反向后退 15 步的指令；当走到重点黄色金币时发出 Clap 音乐奖励声，游戏获胜。",
                    grade = "四年级",
                    deadline = "2026-06-25",
                    deadlineTime = now + thirtyDaysMs,
                    teacherId = teacherId,
                    classId = classId2,
                    status = "进行中"
                )
            ).toInt()

            val taskId4 = dao.insertTask(
                LearningTask(
                    taskName = "神奇的电子琴 - 太空音效器",
                    taskDetail = "创建炫酷的外星太空打击乐组合！通过绑定按键键盘按键 A、S、D、F 分别对应四个不同的声音，并联动发声的四名乐队主唱角色的摇滚跳跃换发型动画状态，探索【播放声音】、【当按下特定键】和【广播和接收消息】的联动运用。",
                    grade = "四年级",
                    deadline = "2026-08-01",
                    deadlineTime = now + (thirtyDaysMs * 3),
                    teacherId = teacherId,
                    classId = classId2,
                    status = "进行中"
                )
            ).toInt()

            // 6. 插入示例作品 (ScratchWork) 与 评测报告
            val sampleCatCode = """{ "targets": [{ "isStage": false, "name": "角色1", "blocks": { "a": { "opcode": "event_whenflagclicked", "next": "b" }, "b": { "opcode": "control_forever", "inputs": { "SUBSTACK": [2, "c"] } }, "c": { "opcode": "motion_movesteps", "inputs": { "STEPS": [1, [4, "10"]] }, "next": "d" }, "d": { "opcode": "motion_ifonedgebounce" } } }] }"""
            val sampleFruitCode = """{ "targets": [{ "isStage": false, "name": "碗", "blocks": { "a": { "opcode": "event_whenflagclicked", "next": "b" }, "b": { "opcode": "control_forever", "inputs": { "SUBSTACK": [2, "c"] } }, "c": { "opcode": "control_if", "inputs": { "CONDITION": [2, "d"], "SUBSTACK": [2, "e"] } }, "d": { "opcode": "sensing_keypressed", "fields": { "KEY_OPTION": ["right arrow", null] } }, "e": { "opcode": "motion_changexby", "inputs": { "DX": [1, [4, "10"]] } } } }] }"""

            val workId1 = dao.insertWork(
                ScratchWork(
                    workName = "张小帅的猫咪漫步作品",
                    workCode = sampleCatCode,
                    studentId = studentId1,
                    classId = classId1,
                    taskId = taskId1,
                    submitCount = 1,
                    submitTime = now - 2 * 3600 * 1000L,
                    reviewStatus = "已打分",
                    teacherScore = 95,
                    teacherComment = "双向弹跳逻辑写得非常规范，小猫咪非常欢快地动起来了，加油！",
                    teacherReviewTime = now - 1 * 3600 * 1000L
                )
            ).toInt()

            dao.insertAiReport(
                WorkAiReport(
                    workId = workId1,
                    studentId = studentId1,
                    grammarScore = 24,
                    logicScore = 29,
                    taskMatchScore = 23,
                    creativeScore = 19,
                    averageScore = 95,
                    optimizationSuggestions = "非常出色的作品！你已经完全掌握了【重复执行】与【碰到边缘反弹】这两个核心运动控制积木。若能在猫咪走动时切换造型（下一个造型），会让整个画面显得更加栩栩如生哦！"
                )
            )

            val workId2 = dao.insertWork(
                ScratchWork(
                    workName = "李小美的接水果大作战",
                    workCode = sampleFruitCode,
                    studentId = studentId2,
                    classId = classId1,
                    taskId = taskId2,
                    submitCount = 1,
                    submitTime = now - 4 * 3600 * 1000L,
                    reviewStatus = "待审核"
                )
            ).toInt()

            dao.insertAiReport(
                WorkAiReport(
                    workId = workId2,
                    studentId = studentId2,
                    grammarScore = 22,
                    logicScore = 25,
                    taskMatchScore = 23,
                    creativeScore = 17,
                    averageScore = 87,
                    optimizationSuggestions = "接水果逻辑非常完整！碗的左右移动灵敏度适中。AI初评建议：可以多添加几类不同落速的水果（如炸弹、香蕉），让游戏更加充满未知的趣味吧！"
                )
            )

            val workId3 = dao.insertWork(
                ScratchWork(
                    workName = "周杰伦的猫咪左右摇摆",
                    workCode = sampleCatCode,
                    studentId = studentId3,
                    classId = classId1,
                    taskId = taskId1,
                    submitCount = 1,
                    submitTime = now - 6 * 3600 * 1000L,
                    reviewStatus = "打回重做",
                    teacherScore = 55,
                    teacherComment = "作品中好像没有发现让小猫向前走动的积木动作噢，重新看下任务卡说明吧！",
                    teacherReviewTime = now - 5 * 3600 * 1000L
                )
            ).toInt()

            // 7. 插入 AI 问答对话历史记录 (AiAssistRecord)
            dao.insertAssistRecord(
                AiAssistRecord(
                    studentId = studentId1,
                    classId = classId1,
                    assistType = "语法纠错",
                    assistTypeInt = 1,
                    callTime = now - 3 * 3600 * 1000L,
                    requestContent = "我的小猫为什么碰到边缘之后头朝下倒过来了？",
                    aiResult = "💡 这是因为 Scratch 默认角色的旋转方式是全方位旋转哦！你可以加入一个【将旋转方式设为左右翻转】的积木块，这样小猫反弹回来时就会正常直立，不会倒立啦！✨",
                    draftId = null
                )
            )

            dao.insertAssistRecord(
                AiAssistRecord(
                    studentId = studentId1,
                    classId = classId1,
                    assistType = "创意引导",
                    assistTypeInt = 2,
                    callTime = now - 1 * 3600 * 1000L,
                    requestContent = "我想设计一个关于太空探险的猫咪游戏，有什么好的点子吗？",
                    aiResult = "🚀 太空猫咪探险记点子包：\n1. **重力改变**：通过减慢下落或移动速度来模拟太空微重力环境！\n2. **太空氧气值**：添加一个氧气计数器，每秒递减，必须触碰绿色的太空补给罐才能回满氧气！\n3. **流星避险**：让几块灰色陨石从右往左滚动，触碰猫咪则扣除一滴生命值！",
                    draftId = null
                )
            )
        }
    }
}
