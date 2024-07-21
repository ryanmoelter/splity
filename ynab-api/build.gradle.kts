import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
  java
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.yelp.codegen)
  alias(libs.plugins.kotlinter)
  alias(libs.plugins.ksp)
}

group = "com.ryanmoelter"
version = "0.3-SNAPSHOT"

repositories {
  mavenCentral()
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
  packageName = "com.ynab.client"
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

kotlinter {
  failBuildWhenCannotAutoFormat = true
}

tasks.formatKotlinMain {
  exclude { it.file.path.contains("generated/") }
}
tasks.formatKotlinTest {
  exclude { it.file.path.contains("generated/") }
}

tasks.lintKotlinMain {
  exclude { it.file.path.contains("generated/") }
}
tasks.lintKotlinTest {
  exclude { it.file.path.contains("generated/") }
}
