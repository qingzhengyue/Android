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
}
