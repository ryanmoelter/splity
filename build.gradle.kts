import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
  java
  alias(libs.plugins.kotlin.jvm)
  application
  id("com.diffplug.spotless").version("5.15.0")
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

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation(libs.coroutines.test)
  testImplementation("io.strikt:strikt-core:0.33.0")
  testImplementation("io.mockk:mockk:1.12.2")
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
