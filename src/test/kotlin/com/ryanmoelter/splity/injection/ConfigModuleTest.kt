package com.ryanmoelter.splity.injection

import com.ryanmoelter.splity.AccountConfig
import com.ryanmoelter.splity.SentryConfig
import com.sksamuel.hoplite.ConfigException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import org.threeten.bp.LocalDate

class ConfigModuleTest :
  FunSpec({
    test("loads YAML accounts, packaged version, and optional defaults") {
      withConfigFile(configYaml + "\nversion: yaml-version\n") { path ->
        val config = loadConfig(path, emptyMap())
        config.version shouldBe packagedVersion()
        config.ynabToken shouldBe "yaml-token"
        config.firstAccount shouldBe AccountConfig("Élodie's budget", "First split")
        config.secondAccount shouldBe AccountConfig("Second budget", "Second split")
        config.startDate shouldBe LocalDate.of(1900, 1, 1)
        config.sentryConfig shouldBe null
        config.ensureZeroBalanceOnCreditCards shouldBe false
      }
    }

    test("decodes configured date, Sentry configuration, and boolean") {
      val yaml =
        configYaml +
          "\nstartDate: '2026-01-02'\n" +
          "sentryConfig:\n  dsn: example-dsn\n" +
          "ensureZeroBalanceOnCreditCards: true\n"
      withConfigFile(yaml) { path ->
        val config = loadConfig(path, emptyMap())
        config.startDate shouldBe LocalDate.of(2026, 1, 2)
        config.sentryConfig shouldBe SentryConfig("example-dsn")
        config.ensureZeroBalanceOnCreditCards shouldBe true
      }
    }

    test("ignores unprefixed and differently cased environment overrides") {
      val environment =
        mapOf(
          "VERSION" to "ambient-version",
          "YNABTOKEN" to "ambient-token",
          "FIRSTACCOUNT_BUDGETNAME" to "ambient-budget",
          "STARTDATE" to "invalid-date",
          "SENTRYCONFIG_DSN" to "ambient-dsn",
          "ENSUREZEROBALANCEONCREDITCARDS" to "true",
          "CONFIG__OVERRIDE__YNABTOKEN" to "uppercase-prefix-token",
        )
      withConfigFile(configYaml) { path ->
        loadConfig(path, environment) shouldBe loadConfig(path, emptyMap())
      }
    }

    test("preserves dotted and double underscore environment overrides") {
      val environment =
        mapOf(
          "config__override__ynabToken" to "environment-token",
          "config__override__firstAccount__budgetName" to "Overridden first budget",
          "config.override.secondAccount.accountName" to "Overridden second split",
          "config__override__startDate" to "2025-12-31",
          "config.override.sentryConfig.dsn" to "override-dsn",
          "config__override__ensureZeroBalanceOnCreditCards" to "true",
        )
      withConfigFile(configYaml) { path ->
        val config = loadConfig(path, environment)
        config.ynabToken shouldBe "environment-token"
        config.firstAccount shouldBe AccountConfig("Overridden first budget", "First split")
        config.secondAccount shouldBe AccountConfig("Second budget", "Overridden second split")
        config.startDate shouldBe LocalDate.of(2025, 12, 31)
        config.sentryConfig shouldBe SentryConfig("override-dsn")
        config.ensureZeroBalanceOnCreditCards shouldBe true
      }
    }

    test("keeps environment above system properties above YAML") {
      withSystemProperty("config.override.ynabToken", "system-token") {
        withSystemProperty("config.override.firstAccount.accountName", "System split") {
          withConfigFile(configYaml) { path ->
            val config =
              loadConfig(path, mapOf("config__override__ynabToken" to "environment-token"))
            config.ynabToken shouldBe "environment-token"
            config.firstAccount shouldBe AccountConfig("Élodie's budget", "System split")
            loadConfig(path, emptyMap()).ynabToken shouldBe "system-token"
          }
        }
      }
    }

    test("rejects invalid dates before any sync starts") {
      withConfigFile(configYaml + "\nstartDate: not-a-date\n") { path ->
        shouldThrow<ConfigException> { loadConfig(path, emptyMap()) }
      }
    }

    test("rejects missing required configuration") {
      withConfigFile(configYaml.replace("ynabToken: yaml-token\n", "")) { path ->
        shouldThrow<ConfigException> { loadConfig(path, emptyMap()) }
      }
    }

    test("requires the configured file to exist") {
      val directory = Files.createTempDirectory("splity-config-test-")
      try {
        shouldThrow<ConfigException> { loadConfig(directory.resolve("missing.yaml"), emptyMap()) }
      } finally {
        Files.deleteIfExists(directory)
      }
    }
  })

private val configYaml =
  """
  ynabToken: yaml-token
  firstAccount:
    budgetName: "Élodie's budget"
    accountName: First split
  secondAccount:
    budgetName: Second budget
    accountName: Second split
  """.trimIndent()

private fun withConfigFile(
  yaml: String,
  block: (Path) -> Unit,
) {
  val path = Files.createTempFile("splity-config-test-", ".yaml")
  try {
    Files.writeString(path, yaml)
    block(path)
  } finally {
    Files.deleteIfExists(path)
  }
}

private fun packagedVersion(): String {
  val properties = Properties()
  requireNotNull(ConfigModuleTest::class.java.getResourceAsStream("/version.properties")).use {
    properties.load(it)
  }
  return properties.getProperty("version")
}

private fun withSystemProperty(
  name: String,
  value: String,
  block: () -> Unit,
) {
  val previous = System.setProperty(name, value)
  try {
    block()
  } finally {
    if (previous == null) System.clearProperty(name) else System.setProperty(name, previous)
  }
}
