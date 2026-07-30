package com.example.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.example.BuildConfig

object SupabaseManager {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Storage)
    }

    suspend fun uploadScratchProject(localFile: File, remoteFileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val bucket = client.storage.from("student-works")
                val fileBytes = localFile.readBytes()
                
                bucket.upload(remoteFileName, fileBytes) {
                    upsert = true
                }
                
                println("上传成功：$remoteFileName")
                true
            } catch (e: Exception) {
                println("上传失败：${e.message}")
                e.printStackTrace()
                false
            }
        }
    }
}
