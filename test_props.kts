import java.util.Properties
import java.io.File
val p = Properties()
p.load(File("local.properties").inputStream())
println(p.getProperty("SUPABASE_URL"))
