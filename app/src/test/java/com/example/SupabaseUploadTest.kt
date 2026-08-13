package com.example

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

class SupabaseUploadTest {
    @Test
    fun testUpload() = runBlocking {
        try {
            val client = createSupabaseClient(
                supabaseUrl = "http://169.254.8.1:8000",
                supabaseKey = com.example.BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Postgrest)
                install(Storage)
            }
            
            val tempFile = File.createTempFile("student_1_project_1_123456789", ".sb3")
            tempFile.writeBytes("dummy content".toByteArray())
            
            val bucket = client.storage.from("student-works")
            bucket.upload(tempFile.name, tempFile.readBytes()) {
                upsert = false
            }
            println("Upload success!")
        } catch (e: Exception) {
            println("UPLOAD FAILED: ${e.javaClass.name}: ${e.message}")
            e.printStackTrace()
        }
    }
}
