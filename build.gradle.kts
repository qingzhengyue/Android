// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  kotlin("jvm") version "2.2.0"
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.kotlin.serialization) apply false
}


dependencies {
  implementation(kotlin("stdlib"))
}

kotlin {
  sourceSets["main"].kotlin.srcDir("src/main/java")
  sourceSets["test"].kotlin.srcDir("src/test/java")
}
