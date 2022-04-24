import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
  java
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.yelp.codegen)
  alias(libs.plugins.spotless)
  alias(libs.plugins.ksp)
}

group = "co.moelten"
version = "0.2-SNAPSHOT"

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

  api(libs.threetenbp)

  testImplementation(libs.junit4)
  testImplementation(libs.okhttp.mockwebserver)
  testImplementation(libs.coroutines.test)
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
