import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

fun main() {
    try {
        val client = createSupabaseClient("http://10.42.101.36:8000", "key") {
            install(Postgrest)
        }
        println("Success")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
