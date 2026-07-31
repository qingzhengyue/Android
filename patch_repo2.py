with open("app/src/main/java/com/example/data/AppRepository.kt", "r") as f:
    content = f.read()

target = """        // 同步作品到云端
        try {
            supabase?.from("scratch_work")?.upsert(finalWorkWithId)
        } catch (e: Exception) {
            Log.e("SupabaseSync", "同步上传失败，拦截到的底层错误是: ${e.message}", e)
            throw Exception("作品云端同步失败: ${e.message}", e)
        }

        val task = dao.getTaskById(work.taskId)
        val eval = GeminiClient.evaluateScratchWork(
            taskName = task?.taskName ?: "",
            taskDetail = task?.taskDetail ?: "",
            workName = work.workName,
            codeJson = work.workCode
        )

        val report = WorkAiReport(
            workId = workId,
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
            throw Exception("评测报告云端同步失败: ${e.message}", e)
        }"""

replacement = """        // 同步作品到云端
        val realWorkId = try {
            val remoteWork = supabase?.from("scratch_work")?.upsert(finalWorkWithId) {
                select()
            }?.decodeSingle<ScratchWork>()
            remoteWork?.workId ?: workId
        } catch (e: Exception) {
            Log.e("SupabaseSync", "同步上传失败，拦截到的底层错误是: ${e.message}", e)
            throw Exception("作品云端同步失败: ${e.message}", e)
        }

        val task = dao.getTaskById(work.taskId)
        val eval = GeminiClient.evaluateScratchWork(
            taskName = task?.taskName ?: "",
            taskDetail = task?.taskDetail ?: "",
            workName = work.workName,
            codeJson = work.workCode
        )

        val report = WorkAiReport(
            workId = realWorkId, // 使用云端返回的真实 ID
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
            throw Exception("评测报告云端同步失败: ${e.message}", e)
        }"""

if target in content:
    content = content.replace(target, replacement)
    print("Replaced successfully")
else:
    print("Could not find target")

with open("app/src/main/java/com/example/data/AppRepository.kt", "w") as f:
    f.write(content)
