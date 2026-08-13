package com.example

import io.ktor.http.*
import org.junit.Test
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

class TestKtor {
    @Test
    fun testUrl() {
        try {
            createSupabaseClient("169.254.8.1:8000", "some_key") {
                install(Postgrest)
            }
        } catch(e: Exception) {
            println("ERROR_OUTPUT_CREATE: " + e.message)
        }
    }
}
