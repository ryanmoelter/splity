import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
  java
  application
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.spotless)
}

group = "co.moelten"
version = "0.7.3-SNAPSHOT"

repositories {
  mavenCentral()
  jcenter()
}

dependencies {
  implementation(project(":ynab-api"))

  implementation(libs.coroutines.core)
  implementation(libs.hoplite.core)
  implementation(libs.hoplite.yaml)
  implementation(libs.sentry)

  testImplementation(libs.junit5)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.strikt)
  testImplementation(libs.mockk)
}

tasks.withType<Test> {
  useJUnitPlatform()
}

application {
  mainClassName = "co.moelten.splity.SplityKt"
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
