import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
  java
  application
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.spotless)
  alias(libs.plugins.sqldelight)
  alias(libs.plugins.ksp)
  idea
}

group = "com.ryanmoelter"
version = "0.9.1-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation(project(":ynab-api"))

  implementation(libs.kotlin.inject.runtime)
  implementation(libs.coroutines.core)
  implementation(libs.hoplite.core)
  implementation(libs.hoplite.yaml)
  implementation(libs.sentry)

  ksp(libs.kotlin.inject.compiler.ksp)

  implementation(libs.sqldelight.driver.jvm)

  testImplementation(libs.kotest.runner)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.kotest.framework.datatest)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.strikt)
  testImplementation(libs.mockk)
}

tasks.withType<Test> {
  useJUnitPlatform()
}

application {
  mainClass.set("com.ryanmoelter.splity.SplityKt")
}

val compileKotlin: org.jetbrains.kotlin.gradle.dsl.KotlinCompile<KotlinJvmOptions> by tasks
val compileTestKotlin: org.jetbrains.kotlin.gradle.dsl.KotlinCompile<KotlinJvmOptions> by tasks
compileKotlin.kotlinOptions {
  jvmTarget = "11"
  // Error on non-exhaustive when, can remove in kotlin 1.7.0 when this behavior is default
  freeCompilerArgs = freeCompilerArgs + "-progressive"
}
compileTestKotlin.kotlinOptions {
  jvmTarget = "11"
  // Error on non-exhaustive when, can remove in kotlin 1.7.0 when this behavior is default
  freeCompilerArgs = freeCompilerArgs + "-progressive"
}

sqldelight {
  database("Database") {
    packageName = "com.ryanmoelter.ynab.database"
    schemaOutputDirectory = file("src/main/sqldelight/databases")
    verifyMigrations = true
  }
}

spotless {
  format("misc") {
    target("*.gradle.kts", "*.md", ".gitignore")

    trimTrailingWhitespace()
    indentWithSpaces(2)
    endWithNewline()
  }
  kotlin {
    trimTrailingWhitespace()
    endWithNewline()
  }
}

task("createProperties") {
  dependsOn("processResources")
  doLast {
    mkdir("$buildDir/resources/main")
    val file = File("$buildDir/resources/main/version.properties")
    file.createNewFile()
    val map = mapOf("version" to project.version.toString())
    val p = map.toProperties()
    val writer = file.writer()
    p.store(writer, null)
    writer.close()
  }
}

val classes: Task by tasks
classes.dependsOn("createProperties")

idea {
  module {
    // Not using += due to https://github.com/gradle/gradle/issues/8749
    sourceDirs =
      sourceDirs + file("build/generated/ksp/main/kotlin") // or tasks["kspKotlin"].destination
    testSourceDirs = testSourceDirs + file("build/generated/ksp/test/kotlin")
    generatedSourceDirs =
      generatedSourceDirs + file("build/generated/ksp/main/kotlin") + file("build/generated/ksp/test/kotlin")
  }
}
