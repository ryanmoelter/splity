import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
  java
  alias(libs.plugins.kotlin.jvm)
  id("com.yelp.codegen.plugin").version("1.4.1")
  alias(libs.plugins.spotless)
  alias(libs.plugins.ksp)
}

group = "co.moelten"
version = "0.1-SNAPSHOT"

repositories {
  mavenCentral()
  jcenter()
}

dependencies {
  implementation(kotlin("reflect"))

  implementation(libs.moshi)
  implementation(libs.moshi.kotlin)
  implementation(libs.moshi.adapters)
  ksp(libs.moshi.kotlin.codegen)
  api(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.moshi)

  api("org.threeten:threetenbp:1.5.1")

  testImplementation("junit:junit:4.13.2")
  testImplementation("com.squareup.okhttp3:mockwebserver:4.9.1")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")
}

generateSwagger {
  platform = "kotlin-coroutines"
  packageName = "com.youneedabudget.client"
  inputFile = file("./ynab.json")
  outputDir = file("./src/main/kotlin/")
}

val compileKotlin: org.jetbrains.kotlin.gradle.dsl.KotlinCompile<KotlinJvmOptions> by tasks
val compileTestKotlin: org.jetbrains.kotlin.gradle.dsl.KotlinCompile<KotlinJvmOptions> by tasks
compileKotlin.kotlinOptions {
  jvmTarget = "11"
}
compileTestKotlin.kotlinOptions {
  jvmTarget = "11"
}

spotless {
  format("misc") {
    target("*.gradle", "*.md", ".gitignore")

    trimTrailingWhitespace()
    indentWithSpaces(2)
    endWithNewline()
  }
  kotlin {
    trimTrailingWhitespace()
    endWithNewline()
  }
}
