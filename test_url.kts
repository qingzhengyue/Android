@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("io.ktor:ktor-client-core:2.3.11")

import io.ktor.http.Url

try {
    val u = Url("http://10.42.101.36:8000")
    println(u)
} catch(e: Exception) {
    e.printStackTrace()
}
