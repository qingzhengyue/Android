# 星梦智学 - Scratch AI 编程助教

面向小学 3-6 年级的“AI+教育”双端教学系统。学生可在 Scratch 工作区完成任务、请求分层提示并提交作品；教师可发布任务、配置 AI 辅导边界、查看作品并结合 AI 报告复核评分。

## 演示账号

- 教师：`T1001` / `123456`
- 学生：`3101` / `123456`

登录页也提供一键演示入口。首次启动会写入匿名示例数据，断网时仍可演示任务、编程、提交、本地结构评测和教师复核闭环。

## 构建

1. 使用 Android Studio 自带 JDK 21。
2. 在项目根目录创建 `.env`，按 `.env.example` 配置模型密钥；不配置时使用本地评测兜底。
3. 执行 `./gradlew assembleDebug`（Windows 使用 `gradlew.bat assembleDebug`）。

比赛材料与逐项自检位于 [`docs/competition`](docs/competition)。正式提交前必须替换文档中的占位符并完成真实性、匿名性复核。

