package co.moelten.splity.injection

import co.moelten.splity.Config
import co.moelten.splity.DateDecoder
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
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

  override fun config(): Config = ConfigLoaderBuilder.default()
    .addDecoder(DateDecoder())
    .addSource(PropertySource.resource("/version.properties"))
    .build()
    .loadConfigOrThrow("./config.yaml")
}
