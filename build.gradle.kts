import java.io.ByteArrayOutputStream
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
  java
  application
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlinter)
  alias(libs.plugins.sqldelight)
  alias(libs.plugins.ksp)
  idea
}

group = "com.ryanmoelter"
version = "0.9.2"

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
  jvmTarget = "21"
  // Error on non-exhaustive when, can remove in kotlin 1.7.0 when this behavior is default
  freeCompilerArgs = freeCompilerArgs + "-progressive"
}
compileTestKotlin.kotlinOptions {
  jvmTarget = "21"
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

val gitDescribe: String by lazy {
  val stdout = ByteArrayOutputStream()
  rootProject.exec {
    commandLine("git", "rev-parse", "--verify", "--short", "HEAD")
    standardOutput = stdout
  }
  stdout.toString().trim()
}

task("createProperties") {
  dependsOn("processResources")
  doLast {
    mkdir("${layout.buildDirectory.get()}/resources/main")
    val file = File("${layout.buildDirectory.get()}/resources/main/version.properties")
    file.createNewFile()
    val gitSha = gitDescribe

    val map =
      mapOf(
        "version" to project.version.toString(),
        "dist" to gitSha,
      )
    val p = map.toProperties()
    val writer = file.writer()
    p.store(writer, null)
    writer.close()
  }
}

val classes: Task by tasks
classes.dependsOn("createProperties")
