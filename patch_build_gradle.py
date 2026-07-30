with open("app/build.gradle.kts", "r") as f:
    content = f.read()

prefix = """val localProperties = java.util.Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

"""

target = """  defaultConfig {
    applicationId = "com.aistudio.scratchassistant.kyvtzb"
    minSdk = 26
    // 将 targetSdk 降级到 35
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }"""

replacement = """  defaultConfig {
    applicationId = "com.aistudio.scratchassistant.kyvtzb"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    
    val geminiKey = localProperties.getProperty("GEMINI_API_KEY") ?: System.getenv("GEMINI_API_KEY") ?: ""
    buildConfigField("String", "GEMINI_API_KEY", "\\"$geminiKey\\"")

    val supabaseUrl = localProperties.getProperty("SUPABASE_URL") ?: System.getenv("SUPABASE_URL") ?: ""
    val supabaseKey = localProperties.getProperty("SUPABASE_ANON_KEY") ?: System.getenv("SUPABASE_ANON_KEY") ?: ""
    buildConfigField("String", "SUPABASE_URL", "\\"$supabaseUrl\\"")
    buildConfigField("String", "SUPABASE_ANON_KEY", "\\"$supabaseKey\\"")
  }"""

content = prefix + content.replace(target, replacement)
with open("app/build.gradle.kts", "w") as f:
    f.write(content)

