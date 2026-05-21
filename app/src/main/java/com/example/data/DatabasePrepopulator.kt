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

            // 3. 插入学生
            dao.insertStudent(
                Student(
                    studentNumber = "S2001",
                    name = "张小帅",
                    password = "123456",
                    classId = classId1
                )
            )

            dao.insertStudent(
                Student(
                    studentNumber = "S2002",
                    name = "李小美",
                    password = "123456",
                    classId = classId1
                )
            )

            dao.insertStudent(
                Student(
                    studentNumber = "S3001",
                    name = "王小飞",
                    password = "123456",
                    classId = classId2
                )
            )

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

            dao.insertTask(
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
            )

            dao.insertTask(
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
            )

            dao.insertTask(
                LearningTask(
                    taskName = "走迷宫 (碰到黑色反弹)",
                    taskDetail = "制作一个经典的键盘控方向‘走迷宫逃跑’游戏。画笔绘制深色迷宫路线。操作甲虫或小恐龙在迷宫内前进。重点编程逻辑：如果玩家操控角色碰到了‘黑色的迷宫围墙边线颜色’，则执行角色反向后退 15 步的指令；当走到重点黄色金币时发出 Clap 音乐奖励声，游戏获胜。",
                    grade = "四年级",
                    deadline = "2026-06-25",
                    deadlineTime = now + thirtyDaysMs,
                    teacherId = teacherId,
                    classId = classId2,
                    status = "进行中"
                )
            )

            dao.insertTask(
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
            )
        }
    }
}
