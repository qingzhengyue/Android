@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("io.ktor:ktor-client-core:2.3.11")
@file:DependsOn("io.ktor:ktor-client-cio:2.3.11")

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking

runBlocking {
    try {
        val client = HttpClient(CIO)
        val response = client.get("http://10.42.101.36:54321")
        println("Status: ${response.status}")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
