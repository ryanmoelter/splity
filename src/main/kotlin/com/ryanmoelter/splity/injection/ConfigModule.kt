package com.ryanmoelter.splity.injection

import com.ryanmoelter.splity.Config
import com.ryanmoelter.splity.DateDecoder
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.emptyByDefaultPropertySources
import com.sksamuel.hoplite.sources.MapPropertySource
import java.nio.file.Path
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class ConfigSingleton

@ConfigSingleton
interface ConfigModule {
  @Provides
  @ConfigSingleton
  fun config(): Config
}

@Component
abstract class RealConfigModule : ConfigModule {
  override fun config(): Config = loadConfig()
}

internal fun loadConfig(
  configFile: Path = Path.of("./config.yaml"),
  environmentVariables: Map<String, String> = System.getenv(),
): Config {
  // Preserve Hoplite 2's explicit overrides instead of importing every environment variable.
  val overrides =
    environmentVariables
      .mapKeys { it.key.replace("__", ".") }
      .filterKeys { it.startsWith("config.override.") }
      .mapKeys { it.key.removePrefix("config.override.") }

  return ConfigLoaderBuilder
    .defaultWithoutPropertySources()
    .addSource(MapPropertySource(overrides))
    .addPropertySources(emptyByDefaultPropertySources())
    .addDecoder(DateDecoder())
    .addSource(PropertySource.resource("/version.properties"))
    .addSource(PropertySource.path(configFile))
    .build()
    .loadConfigOrThrow()
}
