package com.example.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.example.BuildConfig

object SupabaseManager {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL.trim(),
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY.trim()
    ) {
        install(io.github.jan.supabase.postgrest.Postgrest)
        install(Storage) {
            resumable {
                cache = io.github.jan.supabase.storage.resumable.MemoryResumableCache()
            }
        }
    }

    suspend fun uploadScratchProject(localFile: File, remoteFileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val bucket = client.storage.from("student-works")
            val fileBytes = localFile.readBytes()
            
            // Avoid upsert which requires UPDATE permissions
            bucket.upload(remoteFileName, fileBytes) {
                upsert = false
            }
            
            println("上传成功：$remoteFileName")
            true
        }
    }
    
    suspend fun insertWorkAiReportRecord(report: WorkAiReportInsertDto): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                client.postgrest["work_ai_report"].insert(report)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun insertScratchWorkRecord(work: ScratchWorkInsertDto): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Ensure Postgrest is imported and used to insert data
                client.postgrest["scratch_work"].insert(work)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
