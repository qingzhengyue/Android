# 🎒 “星梭智学”少儿硬件级拼搭式 Scratch 智能编程辅学平台 (Android)
## 本地部署、关系型数据库建档、API 接口设计及汇报白皮书

> **致谢与说明**：本系统专为**小学3-6年级课堂智能编程教学**而打造，紧密契合“软件搭配便携低杂硬件、深度契合课堂场景、全绿化AI诊断辅助”等教学理念。本白皮书旨在为您打通**本地物理建档、服务器开发部署**，并为您提供**学术汇报、课堂应用汇报、立项答辩**的高水平架构阐述。

---

## 目录
- [一、 项目定位与独创课堂创新点](#一-项目定位与独创课堂创新点)
- [二、 系统架构设计与多端交互模型](#二-系统架构设计与多端交互模型)
- [三、 贯穿式课前-课中-课后教学全流程图谱](#三-贯穿式课前-课中-课后教学全流程图谱)
- [四、 软件核心功能模块精细说明](#四-软件核心功能模块精细说明)
- [五、 本地部署物理数据库 DDL 件流表设计 (MySQL / SQLite)](#五-本地部署物理数据库-ddl-件流表设计-mysql--sqlite)
- [六、 局域网 Restful API 业务接口规范](#六-局域网-restful-api-业务接口规范)
- [七、 教师本地极速部署与局域网落地流程](#七-教师本地极速部署与局域网落地流程)

---

## 一、 项目定位与独创课堂创新点

本应用并不是一个简单的 Scratch 编程工具，而是一套**深度切入小学真实课堂生态系统的“软硬件一体化编程学习闭环系统”**。

在传统的小学信息技术（编程）课堂中，教师面临许多痛点：机房笨重维护困难、小学生面对电脑容易私自上网/玩游戏、以及老师在30-40人的大班中无法逐个纠错引导。平台的核心优势通过软硬结合与 AI 辅助彻底解决了这些缺陷：

### 🎯 4 大重磅创新点 (学术与汇报核心)

1. **「硬件级高专注防分心形态」，彻底杜绝网络杂音**：
   * **痛点**：传统微机课上，由于使用的是通用 PC 浏览器或电脑系统，小学生会私底下偷偷打开网络游戏网页、下载游戏，或者利用聊天工具闲聊，导致教师需要长时间巡视维护课堂秩序。
   * **创新设计**：本 App 搭载于定制化的掌上嵌入式手持终端或平板安卓硬件。终端在系统层或应用层进行**安全锁定（Kiosk 模式）**，设备开机即锁死在本 Scratch 编程界面。设备本身不具备通用浏览器和多余软件下载权限。极简而高度专一，让小学生在课堂上“除了编程，什么也玩不了”，注意力回归算法拼搭。

2. **「超强灵活、轻量便携的移动课堂形态」，打破传统机房时空限制**：
   * **痛点**：传统信息技术课堂必须依托固定的计算机教室（机房），建设成本高达数十万，且排课死板，无法在常规教室中即时展开。
   * **创新设计**：拼搭终端采用小巧的无线锂电掌上形态，像书本一样轻盈、便于携带，即拿即走。教学活动不必再去机房，可在任一常规智慧教室、科学教室、甚至户外活动场地中“随时无缝铺开”。每人一机，课前分发、课后箱收，具有极高的部署灵活性。

3. **「首创“去标准答案”的少儿启发式 AI 编程精灵」：**
   * **痛点**：普通 AI 助手往往会直接给出写好的 JSON 积木代码或逻辑答案，导致小学生复制粘贴，变成不动脑筋的“搬运工”。
   * **创新设计**：自研 **少儿心智阶梯式 AI Prompt 引导策略**。AI 扮演“编程精灵姐姐”，其语言极其温柔有爱、使用大量萌趣比喻（例如：将变量比作收纳盒，将循环比作旋转木马）。AI 不给任何现成答案，仅挑错并给出类似“拖动什么颜色积木接到哪一步”的阶梯式搭建说明，促成学生自主纠错。同时，教师后台可对每个班级的 AI 单日调用频率、提示级别进行一键限锁。

4. **「内置 AI + 教师双轨综合评测机制（AI 判卷 + 老师反馈）」：**
   * **痛点**：大班额教学中，一个老师要审核 40 份 Scratch 积木代码，导致评语千篇一律，无法触及具体逻辑缺陷。
   * **创新设计**：首创双轨审查制。学生提交后，**Gemini API** 扮演的 AI 测评官立即使能作品的静态 JSON 抽象语法树（AST），从【语法规范 (25分)】、【多维度算法逻辑 (30分)】、【任务契合度 (25分)】、【趣味创意 (20分)】四维度秒级生成极具童真和鼓励性的诊断报告。在此基础上，教师可直接查阅得分并在后台追加亲写批阅意见或实施“一键打回重构”，真正做到精细化因材施教。

---

## 二、 系统架构设计与多端交互模型

基于信息安全与运行健壮性，本平台自底向上采用 **MVVM (Model-View-ViewModel) 架构模式 & 本地局域网分布式持久化方案**。

```
              ┌─────────────────────────────────────────────────────────┐
              │                移动/手持硬件终端 (Android APP)            │
              ├─────────────────────────────────────────────────────────┤
              │   [UI 表现层 (Compose)]                                │
              │        ├──────► 互动机房客户端 (学生端 / 教师端)            │
              │        └──────► 零延迟物理 WebView 编排空间                │
              │                                                         │
              │   [ViewModel 状态机 & 离线保护机制]                      │
              │        ├──────► 双轨注册与登录状态机                      │
              │        ├──────► 离线草稿箱/定时任务存盘                    │
              │        └──────► 全流程数据绑定、状态自驱、断网缓冲         │
              │                                                         │
              │   [仓储中转层 AppRepository (Room 数据库本地缓存)]      │
              │        └──────► SQLite 零延迟快照防瞬断断电丢失           │
              └──────────────────────────┬──────────────────────────────┘
                                         ▲
                                 蜂窝 / 本地 WiFi 局域网
                                         ▼
              ┌─────────────────────────────────────────────────────────┐
              │             学校随堂物理服务器 / 本地化云端级后台         │
              ├─────────────────────────────────────────────────────────┤
              │   [后端 API 路由 (NestJS / Spring Boot 服务级架构)]      │
              │        ├──────► /api/auth    (多角色物理建档认证)        │
              │        ├──────► /api/classes (教学班物理绑档)            │
              │        ├──────► /api/tasks   (作业教学下发管控)          │
              │        ├──────► /api/works   (代码静态树 AI 综合测评)     │
              │        └──────► /api/ai      (少儿阶梯安全引导指令)       │
              │                                                         │
              │   [数据持久化引擎 - MySQL 8.0 物理关系数据库]          │
              │        └──────► 跨网课、跨终端学号秒同步数据归档         │
              └─────────────────────────────────────────────────────────┘
```

---

## 三、 贯穿式课前-课中-课后教学全流程图谱

```
                 课前准备                  课中编程与拼搭                 课后数据归档
             
              ┌────────────┐             ┌────────────────┐             ┌────────────┐
              │ 教师端：    │             │ 学生端：登录、  │             │ 教师端：    │
              │ 录入班级学 │             │ 查阅今日编程任 │             │ 全景学期看板│
              │ 籍与学生数 │             │ 务模板         │             │ 掌控学情分布│
              └─────┬──────┘             └───────┬────────┘             └─────▲──────┘
                    │                            │                            │
                    ▼                            ▼                            │
              ┌────────────┐             ┌────────────────┐             ┌─────┴──────┐
              │ 教师下发：  │             │ 全屏高度适配， │             │ 批阅打分：  │
              │ 课堂趣味编 │────────────►│ 多渠道高速通道 │             │ 查看 AI 分析│
              │ 程挑战任务 │             │ 拖拽设计积木   │             │ 报告，定分 │
              └────────────┘             └───────┬────────┘             └─────▲──────┘
                                                 │                            │
                                                 ▼                            │
                                         ┌────────────────┐             ┌─────┴──────┐
                                         │ 遇到逻辑瓶颈？  │             │ 学生正式   │
                                         │ 一键唤醒 AI    │────────────►│ 提交作品：  │
                                         │ 阶梯辅学姐姐   │             │ AI 秒级评测 │
                                         └────────────────┘             └────────────┘
```

---

## 四、 软件核心功能模块精细说明

### 1. 独立双端统一登录注册模块 (学生端 & 教师端)
* **设计意图**：严禁无账号的游客模式污染数据。双端隔离物理账号。学生注册时必须物理拉取和选择所属教学班级。
* **安全性**：可接入硬件序列号 (UUID) 绑定，方便学校固定资产实名分牌。

### 2. 精准极速全屏 Web 骨架编程空间 (内置双镜像通道)
* **全屏适配**：剔除一切屏幕缩放和多余提示，手势全屏化专属于拖拽、摆放和衔接 Scratch 积木。
* **高可用性**：针对学校内网偶尔断连 MIT 官方网的困境，**全球首创“抗弱网一键国内镜像/官方源一键自适应瞬时切换”技术**。网络慢时点击一键切换为国内极速通道，保证流畅开课。

### 3. 限频绿色防抄袭 AI 心智助理 (精灵辅导)
* **防止依赖**：教师端可在后台配置该班级的每日提问限度（如：防沉迷单日最多询问 8 次）。
* **启发思考**：拒绝输出直接的代码拷贝。AI 采用阶梯式点拨，只用图文和动作步骤教孩子在什么分区找何种颜色的积木进行逻辑接轨，培养计算思维。

### 4. 今日挑战任务下发与离线随堂草稿箱
* **任务清单**：学生登录后直接展示关联班级在进行中的课后/课堂任务，并可以直接使用老师配制的基础骨架模板代码（如“水果消除”、“接苹果游戏”）一键初始化编辑。
* **数据安全**：随堂自动存盘和手动备份到本地 Local DB (Room)，彻底防范掌上硬件断电、死机导致小学生编程文件丢失时大哭的教学灾难。

### 5. AI 与教师双层审判台 (批改与反馈)
* **AI 闪电战**：学生端一键提交，Gemini 会基于积木 JSON 进行四维雷达度量，直接生成鼓励性诊断报告。
* **名师精绘图**：教师端独立界面能查阅各班级学生的提交记录，手动阅览作品、查阅 AI 报告详情、完成二次批分、添加老师的亲笔点评、或者打回让孩子继续在设备中改进。

---

## 五、 本地部署物理数据库 DDL 件流表设计 (MySQL / SQLite)

为了将此平台完美部署到您的学校物理机房服务器中，以下提供了精细化并且严格遵守外键物理映射逻辑的 standard **MySQL 8.0 DDL 建表脚本**。在部署本地服务器时，您可以直接将以下代码整体复制并在您的 MySQL 连接器中直接执行：

```sql
-- 设置运行环境规范
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 教师基本账户表 (teacher)
-- ----------------------------
DROP TABLE IF EXISTS `teacher`;
CREATE TABLE `teacher` (
  `teacherId` INT AUTO_INCREMENT NOT NULL COMMENT '自增教师唯一主键',
  `workId` VARCHAR(50) NOT NULL UNIQUE COMMENT '教师唯一登录工号/账号',
  `name` VARCHAR(50) NOT NULL COMMENT '老师姓名',
  `password` VARCHAR(255) NOT NULL COMMENT '登录密码(MD5或BCrypt高强度哈希加密密文)',
  `createTime` BIGINT NOT NULL COMMENT '账户创建毫秒级时间截',
  PRIMARY KEY (`teacherId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师后台核心角色认证档案表';

-- ----------------------------
-- 2. 班级档案索引表 (class)
-- ----------------------------
DROP TABLE IF EXISTS `class`;
CREATE TABLE `class` (
  `classId` INT AUTO_INCREMENT NOT NULL COMMENT '自增班级物理主键',
  `className` VARCHAR(100) NOT NULL COMMENT '班级中文真实标识命名(如：三年级2班、卓越社团)',
  `grade` VARCHAR(50) NOT NULL COMMENT '对应年级（用于匹配 Scratch 阶梯大纲）',
  `teacherId` INT NOT NULL COMMENT '该班绑定的专职教学与负责人教师ID',
  `createTime` BIGINT NOT NULL COMMENT '建课建班归档时间戳',
  PRIMARY KEY (`classId`),
  CONSTRAINT `fk_class_teacher` FOREIGN KEY (`teacherId`) REFERENCES `teacher` (`teacherId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物理教学班级名录，支撑一师多班教学制';

-- ----------------------------
-- 3. 学生实名关系表 (student)
-- ----------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student` (
  `studentId` INT AUTO_INCREMENT NOT NULL COMMENT '学生核心自增主键',
  `studentNumber` VARCHAR(50) NOT NULL UNIQUE COMMENT '全国学籍唯一实号或学校自造序列学号',
  `name` VARCHAR(50) NOT NULL COMMENT '学生中文名',
  `password` VARCHAR(255) NOT NULL COMMENT '孩子简易登录密码（明文或哈希散列）',
  `classId` INT NOT NULL COMMENT '该生目前就读及数据绑定的班级ID',
  `registerTime` BIGINT NOT NULL COMMENT '学生首次终端开机注册入网毫秒戳',
  PRIMARY KEY (`studentId`),
  CONSTRAINT `fk_student_class` FOREIGN KEY (`classId`) REFERENCES `class` (`classId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生信息档案注册表，用于精确定向追踪学业轨迹';

-- ----------------------------
-- 4. 协同 AI 教学深度配置防抄袭表 (ai_teaching_config)
-- ----------------------------
DROP TABLE IF EXISTS `ai_teaching_config`;
CREATE TABLE `ai_teaching_config` (
  `configId` INT AUTO_INCREMENT NOT NULL COMMENT 'AI智能策略主键ID',
  `classId` INT NOT NULL UNIQUE COMMENT '被控班级ID',
  `teacherId` INT NOT NULL COMMENT '下达安全操作规范的教师ID',
  `aiHintLevel` VARCHAR(50) NOT NULL DEFAULT '入门阶梯引导' COMMENT 'AI提示规范等级("入门阶梯引导","中级启发纠错","高级全能分析")',
  `codeGenerationLimit` INT NOT NULL DEFAULT 0 COMMENT '绝对禁止生成代码(0: 禁用防止抄袭, 1: 开启限制级参考)',
  `creativeGuideDailyLimit` INT NOT NULL DEFAULT 8 COMMENT '每日学生最大AI精灵辅助上限(护眼、防过度沉迷和算法思考偷懒)',
  PRIMARY KEY (`configId`),
  CONSTRAINT `fk_config_class` FOREIGN KEY (`classId`) REFERENCES `class` (`classId`) ON DELETE CASCADE,
  CONSTRAINT `fk_config_teacher` FOREIGN KEY (`teacherId`) REFERENCES `teacher` (`teacherId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面向各班级的 AI 教师辅助限权策略下达表';

-- ----------------------------
-- 5. 课堂/作业任务下发控制表 (learning_task)
-- ----------------------------
DROP TABLE IF EXISTS `learning_task`;
CREATE TABLE `learning_task` (
  `taskId` INT AUTO_INCREMENT NOT NULL COMMENT '任务自增唯一ID',
  `taskName` VARCHAR(200) NOT NULL COMMENT '编程任务挑战通俗标题（如：小猫吃苹果、疯狂赛车）',
  `taskDetail` TEXT NOT NULL COMMENT '老师编写的重点积木提醒、核心玩法描述',
  `grade` VARCHAR(50) NOT NULL COMMENT '面向及限制年级',
  `deadline` VARCHAR(100) NOT NULL COMMENT '截止人类可读时间表达字（如：2026-06-30）',
  `deadlineTime` BIGINT NOT NULL COMMENT '截止精确物理时间戳（到时禁止提交）',
  `teacherId` INT NOT NULL COMMENT '发布本任务的任课老师ID',
  `classId` INT NOT NULL COMMENT '该任务专享指向公开展示的班级ID',
  `status` VARCHAR(50) NOT NULL DEFAULT '进行中' COMMENT '任务进行状态("未开始","进行中","已截止收官")',
  PRIMARY KEY (`taskId`),
  CONSTRAINT `fk_task_teacher` FOREIGN KEY (`teacherId`) REFERENCES `teacher` (`teacherId`) ON DELETE CASCADE,
  CONSTRAINT `fk_task_class` FOREIGN KEY (`classId`) REFERENCES `class` (`classId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课堂编程教学与随堂评测项目下发发布表';

-- ----------------------------
-- 6. 学生随堂备份轻量草稿箱表 (scratch_draft)
-- ----------------------------
DROP TABLE IF EXISTS `scratch_draft`;
CREATE TABLE `scratch_draft` (
  `draftId` INT AUTO_INCREMENT NOT NULL COMMENT '随堂草稿自增物理主键',
  `draftName` VARCHAR(200) NOT NULL COMMENT '草稿自定义名字，学生命名（如：迷宫一稿）',
  `blockCode` LONGTEXT NOT NULL COMMENT '前端 Scratch 工作区拼插产生的全局 AST JSON 数据大字符文本文件流',
  `studentId` INT NOT NULL COMMENT '创建/编辑此草稿的对应学生ID',
  `taskId` INT DEFAULT NULL COMMENT '关联的具体教学任务ID(为NULL代表课外自由创作)',
  `createTime` BIGINT NOT NULL,
  `lastModifiedTime` BIGINT NOT NULL,
  PRIMARY KEY (`draftId`),
  CONSTRAINT `fk_draft_student` FOREIGN KEY (`studentId`) REFERENCES `student` (`studentId`) ON DELETE CASCADE,
  CONSTRAINT `fk_draft_task` FOREIGN KEY (`taskId`) REFERENCES `learning_task` (`taskId`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='草稿云端同步/本地离线定时存盘表，保全小学生心血；防机房瞬时停电';

-- ----------------------------
-- 7. Scratch 正式提交代码作品表 (scratch_work)
-- ----------------------------
DROP TABLE IF EXISTS `scratch_work`;
CREATE TABLE `scratch_work` (
  `workId` INT AUTO_INCREMENT NOT NULL COMMENT '已提交作品唯一ID',
  `workName` VARCHAR(200) NOT NULL COMMENT '最终作品名',
  `workCode` LONGTEXT NOT NULL COMMENT '正式提交版 Scratch 内部积木流完整的序列化大字符段',
  `studentId` INT NOT NULL COMMENT '作答并提交的学生ID',
  `classId` INT NOT NULL COMMENT '提交时关联的教学班级ID',
  `taskId` INT NOT NULL COMMENT '绑定下发的今日挑战任务ID',
  `submitCount` INT DEFAULT 1 COMMENT '重构作品、多次修改后二次提交在当前表上的计数累计器',
  `submitTime` BIGINT NOT NULL COMMENT '实际物理提交时间毫秒戳',
  `reviewStatus` VARCHAR(50) NOT NULL DEFAULT '待审核' COMMENT '目前的总评态("待审核","已评测已打分","指令重做打回中")',
  `teacherScore` INT DEFAULT NULL COMMENT '讲台教师手打打分分值(满分100)',
  `teacherComment` TEXT DEFAULT NULL COMMENT '教师亲写、鼓励性或者订正类的中文文字描述',
  `teacherReviewTime` BIGINT DEFAULT NULL COMMENT '教师批卷打分判定时间戳',
  PRIMARY KEY (`workId`),
  CONSTRAINT `fk_work_student` FOREIGN KEY (`studentId`) REFERENCES `student` (`studentId`) ON DELETE CASCADE,
  CONSTRAINT `fk_work_class` FOREIGN KEY (`classId`) REFERENCES `class` (`classId`),
  CONSTRAINT `fk_work_task` FOREIGN KEY (`taskId`) REFERENCES `learning_task` (`taskId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生正式提交评估的代码实体与教师总审评价综合表';

-- ----------------------------
-- 8. 雷达图谱多层 AI 自动评测报告表 (work_ai_report)
-- ----------------------------
DROP TABLE IF EXISTS `work_ai_report`;
CREATE TABLE `work_ai_report` (
  `reportId` INT AUTO_INCREMENT NOT NULL COMMENT 'AI评卷自增唯物主键ID',
  `workId` INT NOT NULL COMMENT '深度指向的正式代码作品表',
  `studentId` INT NOT NULL COMMENT '被测评的小朋友ID',
  `grammarScore` INT NOT NULL COMMENT '【语法与封装规范】AI给分，多事件或自定义积木嵌套健康度评分，满分 25分',
  `logicScore` INT NOT NULL COMMENT '【计算逻辑与流程算法】运算、并行、广播逻辑复杂度评分，满分 30分',
  `taskMatchScore` INT NOT NULL COMMENT '【目标对齐与完成契合度】检测该作品是否含有老师要的积木要素，满分 25分',
  `creativeScore` INT NOT NULL COMMENT '【趣味度与艺术创意度】交互趣味、动作特效丰富度评分，满分 20分',
  `averageScore` INT NOT NULL COMMENT '四维度雷达合计总分，满分100分',
  `optimizationSuggestions` TEXT NOT NULL COMMENT '极其丰富有爱心的少儿大文段诊断结果：包含闪光点表扬、小迷糊提醒、优化提示',
  `reportTime` BIGINT NOT NULL,
  PRIMARY KEY (`reportId`),
  CONSTRAINT `fk_report_work` FOREIGN KEY (`workId`) REFERENCES `scratch_work` (`workId`) ON DELETE CASCADE,
  CONSTRAINT `fk_report_student` FOREIGN KEY (`studentId`) REFERENCES `student` (`studentId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大语言模型（大底座深度微调）静态编译并雷达检测出的多维详细考评记录表';

-- ----------------------------
-- 9. AI 心智辅助精灵逐笔交互日志 (ai_assist_record)
-- ----------------------------
DROP TABLE IF EXISTS `ai_assist_record`;
CREATE TABLE `ai_assist_record` (
  `callId` INT AUTO_INCREMENT NOT NULL COMMENT '日志主键ID',
  `studentId` INT NOT NULL COMMENT '发起询问的学生ID',
  `classId` INT NOT NULL COMMENT '此交互发生的实时班级ID(利于老师掌握全班瓶颈)',
  `assistType` VARCHAR(100) NOT NULL COMMENT '精灵角色分类("语法纠错","创意引导","知识点讲解")',
  `callTime` BIGINT NOT NULL,
  `requestContent` TEXT NOT NULL COMMENT '发送求助时，工作区积木主要内容的语义压缩说明',
  `aiResult` TEXT NOT NULL COMMENT '精灵姐姐给以的小学生能够立刻动手实践的指示提示',
  `draftId` INT DEFAULT NULL,
  PRIMARY KEY (`callId`),
  CONSTRAINT `fk_assist_student` FOREIGN KEY (`studentId`) REFERENCES `student` (`studentId`) ON DELETE CASCADE,
  CONSTRAINT `fk_assist_class` FOREIGN KEY (`classId`) REFERENCES `class` (`classId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI精灵课上问询逐笔跟踪日志，可由此为教师提供“难关频现积木排行”数据支撑';

SET FOREIGN_KEY_CHECKS = 1;
```

---

## 六、 局域网 Restful API 业务接口规范

若您需要在本地自行搭建 Web 后端（如采用 Node.js / Java）配合本 Android App 实现网络化。请保证实现了以下与 App 字段和业务逻辑完全吻合的 Rest 接口规范：

### 1. 登录与多轨账户注册
*   **学生随堂秒级入网注册**
    *   `POST /api/auth/student/register`
    *   **入参**：`{"studentNumber": "学号如S3001", "name": "王小帅", "password": "简易密码", "classId": 3}`
    *   **回包**：`{"code": 200, "message": "注册物理绑定成功", "studentId": 14}`
*   **学生端快捷闪电登录**
    *   `POST /api/auth/student/login`
    *   **入参**：`{"studentNumber": "学号如S3001", "password": "密码"}`
    *   **回包**：`{"code": 200, "student": {"studentId": 14, "name": "王小帅", "classId": 3}}`
*   **教师端入网注册**
    *   `POST /api/auth/teacher/register`
    *   **入参**：`{"workId": "教工号如T1001", "name": "王老师", "password": "账户密码"}`
*   **教师端后台统一登录**
    *   `POST /api/auth/teacher/login`
    *   **入参**：`{"workId": "教工号如T1001", "password": "密码"}`

### 2. 班组建档与今日挑战下发 (教师端)
*   **物理创建新的教学班级档案**
    *   `POST /api/classes/create`
    *   **入参**：`{"className": "三年级三班", "grade": "三年级", "teacherId": 2}`
    *   **回包**：会自动向 `ai_teaching_config` 生成一条入门级默认引导策略、单日提问额度定标为 8。
*   **新发可自动获取骨架代码的核心挑战任务**
    *   `POST /api/tasks/create`
    *   **入参**：`{"taskName": "小猫捉迷藏", "taskDetail": "利用重复循环积木，在角色碰到边缘后让它随机转向并改变大小，隐藏2秒再现身。", "grade": "三年级", "deadlineStr": "2026-06-30", "classId": 3, "teacherId": 2}`

### 3. 多渠道 AI 与手动评测交互 (学生端核心)
*   **向大模型通道发起限流绿色启发求助**
    *   `POST /api/ai/assist`
    *   **入参**：`{"studentId": 14, "classId": 3, "assistType": "语法纠错", "blockCode": "LONGTEXT_JSON"}`
    *   **后端流控制**：
        1. 物理检查 `checkDailyAssistOk` 高温探测。校验 `dailyCount` 是否超过 `creativeGuideDailyLimit`。
        2. 将 `aiHintLevel` 拼装到系统 Prompt 中（如：提示级别定义为入门级别，只准指点拖动积木操作，绝不允许输出代码块）。
        3. 调用大底座模型（如 Gemini REST）。
        4. 结算计入 `ai_assist_record`，并返回启发文本。
*   **代码最终提交与双规自动测评报告生成**
    *   `POST /api/works/submit`
    *   **入参**：`{"workName": "王小帅猫咪漫步终稿", "workCode": "LONG_TEXT_JSON", "studentId": 14, "classId": 3, "taskId": 5}`
    *   **回包**：生成并返回 `WorkAiReport`，其中包含雷达合项（Grammar, Logic, TaskMatch, Creative）四维拆合，满分 100 综合判定值及鼓励建议。

---

## 品、七、 教师本地极速部署与局域网落地流程

为了帮助任课老师轻松把系统应用到每星期的常态化课堂中，只需遵循以下**三步极速操作**：

1. **第一步：建立机房本地数据库环境**
   * 在教师讲台的多媒体机房控制端电脑（Windows/Linux 均可）解压安装轻量级 MySQL 或一键集成环境（如 XAMPP 或 PhpStudy）。
   * 启动服务并运行第 [五] 章节的全部建表 DDL 代码完成全架构物理建档。

2. **第二步：启动服务器（API Server）并记录内网 IP 地址**
   * 运行您的局域网后端服务（可极简采用一键运行的 Spring Boot Jar 包，或一劳永逸运行 Node 服务）。
   * 主动连接到教室内的统一物理无线路由器，在控制台中运行 `ipconfig`（Mac 下为 `ifconfig`）查阅并记录教师机在该室内 WiFi 的内网固定 IP 格式，例如：`192.168.1.100`。

3. **第三步：分发安卓拼搭终端设备，登录即可享极致无干扰编程**
   * 将每台平板手持拼搭端统一连在教室内同一台路由器 WiFi 下。
   * 运行本 Android 应用程序，首次使用在登录界面指定该 IP 地址，即可秒级拉取当前班级课前录入的所有学籍序列、开展流畅的拼插交互。
   * 下课前将设备统一插在便携充电箱中，数据已自动安防存在局域网服务器，防患于未然！
