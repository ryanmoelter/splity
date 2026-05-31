import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

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
version = "0.9.2-SNAPSHOT"

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

kotlin {
  jvmToolchain(25)
  compilerOptions {
    // -progressive enables exhaustive `when` over sealed types/enums, which the codebase relies on
    freeCompilerArgs.add("-progressive")
  }
}

sqldelight {
  databases {
    create("Database") {
      packageName.set("com.ryanmoelter.ynab.database")
      schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
      // verifyMigrations is off: there are no .sqm migration files (so it was a no-op in
      // SQLDelight 1.x), and SQLDelight 2.1.0's VerifyMigrationTask runs away (OOMs) here.
      verifyMigrations.set(false)
      dialect(libs.sqldelight.dialect.sqlite)
    }
  }
}

kotlinter {
  ignoreFormatFailures = false
  // Pin ktlint to the ruleset kotlinter 4.5.0 used; newer ktlint adds style rules that would
  // reformat the codebase and flag the generated YNAB client. Bump separately if desired.
  ktlintVersion = "1.4.1"
}

tasks.withType<FormatTask> {
  exclude { it.file.path.contains("generated/") }
}
tasks.withType<LintTask> {
  exclude { it.file.path.contains("generated/") }
}

tasks.register("createProperties") {
  dependsOn("processResources")
  doLast {
    val resourcesDir = layout.buildDirectory.dir("resources/main").get().asFile
    resourcesDir.mkdirs()
    val file = resourcesDir.resolve("version.properties")
    file.createNewFile()
    val p = mapOf("version" to project.version.toString()).toProperties()
    file.writer().use { p.store(it, null) }
  }
}

tasks.named("classes") {
  dependsOn("createProperties")
}

idea {
  module {
    // Not using += due to https://github.com/gradle/gradle/issues/8749
    sourceDirs =
      sourceDirs + file("build/generated/ksp/main/kotlin") // or tasks["kspKotlin"].destination
    testSources.from(file("build/generated/ksp/test/kotlin"))
    generatedSourceDirs =
      generatedSourceDirs + file("build/generated/ksp/main/kotlin") +
      file("build/generated/ksp/test/kotlin")
  }
}
