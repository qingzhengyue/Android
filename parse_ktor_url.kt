import io.ktor.http.*
fun main() {
    try {
        Url("10.42.101.36:8000")
    } catch(e: Exception) {
        println(e.message)
    }
}
