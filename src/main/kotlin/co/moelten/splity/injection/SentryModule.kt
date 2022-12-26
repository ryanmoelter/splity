package co.moelten.splity.injection

import co.moelten.splity.Config
import co.moelten.splity.SentryWrapper
import co.moelten.splity.setUpSentry
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class SentrySingleton

@Component
@SentrySingleton
abstract class SentryModule {
  @Provides
  @SentrySingleton
  fun sentryWrapper(config: Config): SentryWrapper =
    setUpSentry(config.sentryConfig, config.version)
}
