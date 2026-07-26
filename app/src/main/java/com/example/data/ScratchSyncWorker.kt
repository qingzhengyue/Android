package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 离线优先架构 (Task 5) - 断点续传与离线数据同步器
 * 负责检测本地 `syncStatus = 0` (待同步) 的草稿与作品，在网络恢复时自动上报至 Supabase 云端。
 */
class ScratchSyncManager(private val repository: AppRepository) {

    suspend fun performBackgroundSync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val unsyncedWorks = repository.getUnsyncedWorks()
            if (unsyncedWorks.isEmpty()) {
                Log.d("ScratchSyncManager", "没有需要同步的离线作品数据")
                return@withContext SyncResult(0, 0, "全部本地数据已处于同步状态")
            }

            var successCount = 0
            var failCount = 0

            for (work in unsyncedWorks) {
                val success = repository.syncSingleWorkToCloud(work)
                if (success) {
                    successCount++
                } else {
                    failCount++
                }
            }

            val summary = "离线同步完成：成功同步 $successCount 件作品，失败 $failCount 件"
            Log.d("ScratchSyncManager", summary)
            SyncResult(successCount, failCount, summary)
        } catch (e: Exception) {
            Log.e("ScratchSyncManager", "离线同步过程异常: ${e.message}")
            SyncResult(0, 0, "同步异常: ${e.message}")
        }
    }

    data class SyncResult(
        val syncedCount: Int,
        val failedCount: Int,
        val message: String
    )
}
