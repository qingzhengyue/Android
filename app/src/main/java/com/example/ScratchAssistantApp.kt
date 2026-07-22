package com.example

import android.app.Application
import android.util.Log

class ScratchAssistantApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 全局未捕获异常处理器：防止 App 闪退，记录错误日志
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CrashHandler", "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            // 可选：在此处保存崩溃日志到本地文件以便下次启动时上报
            try {
                val stackTrace = Log.getStackTraceString(throwable)
                val prefs = getSharedPreferences("crash_log", MODE_PRIVATE)
                prefs.edit()
                    .putString("last_crash", stackTrace)
                    .putLong("last_crash_time", System.currentTimeMillis())
                    .apply()
            } catch (e: Exception) {
                Log.e("CrashHandler", "Failed to save crash log: ${e.message}")
            }
        }
    }
}
