import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

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

kotlin {
  jvmToolchain(17)
}

kotlinter {
  ignoreFormatFailures = false
  // Pin ktlint to the ruleset kotlinter 4.5.0 used; newer ktlint flags the generated YNAB client.
  ktlintVersion = "1.4.1"
}

tasks.withType<FormatTask> {
  exclude { it.file.path.contains("generated/") }
}
tasks.withType<LintTask> {
  exclude { it.file.path.contains("generated/") }
}
