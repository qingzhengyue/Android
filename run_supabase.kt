import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

fun main() {
    createSupabaseClient("10.42.101.36:8000", "key") {
        install(Postgrest)
    }
}
