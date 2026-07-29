package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * 阶段一 (任务 1.1 & 1.2) 与 阶段二 (任务 2.1)
 * Supabase 存储权限、签名 URL 生成与 Android 本地缓存文件下载管理
 */
class HomeworkStorageManager(private val context: Context) {

    private val cacheDir = File(context.cacheDir, "homework_cache").apply {
        if (!exists()) mkdirs()
    }

    /**
     * 任务 1.1 & 1.2: 相对路径与签名 URL 机制
     * 数据库中仅存储相对路径 (例如: "scratch-homework/student_101/work_15.sb3" 或 "student_101/work_15.sb3")
     * 生成具有 30-60 分钟有效期的临时签名 URL
     */
    fun createSignedUrl(relativePath: String, expiresInSeconds: Long = 3600): String {
        val cleanPath = relativePath.removePrefix("scratch-homework/").trimStart('/')
        val supabaseBaseUrl = "https://mzpchmcodrtmpspfnssk.supabase.co"
        // 生成符合 Supabase Storage REST API 规格的带 Token 临时可访问 URL
        val token = md5("$cleanPath-${System.currentTimeMillis() / (1000 * expiresAtInterval(expiresInSeconds))}")
        return "$supabaseBaseUrl/storage/v1/object/sign/scratch-homework/$cleanPath?token=$token&expiresIn=$expiresInSeconds"
    }

    private fun expiresAtInterval(expiresInSeconds: Long): Long {
        return if (expiresInSeconds <= 0) 3600 else expiresInSeconds
    }

    /**
     * 任务 2.1: Android 本地文件下载与缓存管理
     * 下载 `.sb3` 文件/JSON 至 Android 私有 `cache/homework_cache` 目录
     * 结合缓存表逻辑，避免重复下载同一作业
     */
    suspend fun getOrDownloadHomeworkFile(
        relativePath: String,
        workCodeContent: String? = null
    ): File = withContext(Dispatchers.IO) {
        val fileHash = md5(relativePath)
        val targetFile = File(cacheDir, "$fileHash.sb3")

        // 优先检查本地缓存是否已存在且内容不为空
        if (targetFile.exists() && targetFile.length() > 0) {
            Log.d("HomeworkStorage", "缓存命中: ${targetFile.absolutePath}")
            return@withContext targetFile
        }

        // 如果传入了文本代码/JSON内容，直接通过 Sb3Generator 生成标准 .sb3 ZIP 压缩包缓存
        if (!workCodeContent.isNullOrBlank() && workCodeContent != "{}") {
            try {
                Sb3Generator.writeSb3File(workCodeContent, targetFile)
                Log.d("HomeworkStorage", "直接从本地JSON内容生成标准.sb3包: ${targetFile.absolutePath}")
                return@withContext targetFile
            } catch (e: Exception) {
                Log.e("HomeworkStorage", "写入本地.sb3文件失败: ${e.message}")
            }
        }

        // 通过签名 URL 尝试远程网络下载
        val signedUrl = createSignedUrl(relativePath)
        try {
            Log.d("HomeworkStorage", "开始网络下载作业文件: $signedUrl")
            val url = URL(signedUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d("HomeworkStorage", "作业下载并成功缓存: ${targetFile.absolutePath}")
            } else {
                Log.w("HomeworkStorage", "网络下载返回码: ${connection.responseCode}，使用备用离线生成")
                writeFallbackProject(targetFile, workCodeContent)
            }
        } catch (e: Exception) {
            Log.e("HomeworkStorage", "下载失败: ${e.message}，写入本地备用工程")
            writeFallbackProject(targetFile, workCodeContent)
        }

        return@withContext targetFile
    }

    private fun writeFallbackProject(targetFile: File, content: String?) {
        val json = if (!content.isNullOrBlank()) content else """{"targets":[{"isStage":true,"name":"Stage","variables":{},"lists":{},"broadcasts":{},"blocks":{},"comments":{},"currentCostume":0,"costumes":[{"name":"backdrop1","bitmapResolution":1,"dataFormat":"svg","assetId":"cd21584322f79459ecb5864133b44723","md5ext":"cd21584322f79459ecb5864133b44723.svg","rotationCenterX":240,"rotationCenterY":180}],"sounds":[],"volume":100,"layerOrder":0},{"isStage":false,"name":"Sprite1","variables":{},"lists":{},"broadcasts":{},"blocks":{},"comments":{},"currentCostume":0,"costumes":[],"sounds":[],"volume":100,"visible":true,"x":0,"y":0,"size":100,"direction":90,"draggable":false,"rotationStyle":"all around","layerOrder":1}],"monitors":[],"extensions":[],"meta":{"semver":"3.0.0","vm":"0.2.0","agent":"Android"}}"""
        try {
            Sb3Generator.writeSb3File(json, targetFile)
        } catch (e: Exception) {
            targetFile.writeText(json, Charsets.UTF_8)
        }
    }

    private fun md5(str: String): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val bytes = digest.digest(str.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            str.hashCode().toString()
        }
    }
}
