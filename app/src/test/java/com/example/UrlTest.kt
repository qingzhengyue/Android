package com.example

import io.ktor.http.Url
import org.junit.Test

class UrlTest {
    @Test
    fun testUrl() {
        try {
            Url("http://   ")
        } catch (e: Exception) {
            println("ERROR_NAME: ${e.javaClass.name}")
            println("ERROR_MSG: ${e.message}")
        }
    }
}
