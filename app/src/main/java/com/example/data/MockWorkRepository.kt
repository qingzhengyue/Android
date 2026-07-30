package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object MockWorkRepository {
    suspend fun getMockSb3FileForTask(context: Context, taskId: Long, themeName: String): File = withContext(Dispatchers.IO) {
        val fileName = when {
            themeName.contains("太空") -> "space_walk.sb3"
            themeName.contains("猫咪") -> "cat_stroll.sb3"
            themeName.contains("迷宫") -> "maze.sb3"
            else -> "default_work.sb3"
        }

        val cacheDir = context.cacheDir
        val outFile = File(cacheDir, "mock_${taskId}_${fileName}")

        if (!outFile.exists()) {
            try {
                context.assets.open("mock_works/$fileName").use { inputStream ->
                    FileOutputStream(outFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (!outFile.exists()) {
                    outFile.writeText("Dummy Scratch Project for $themeName")
                }
            }
        }
        outFile
    }
}
