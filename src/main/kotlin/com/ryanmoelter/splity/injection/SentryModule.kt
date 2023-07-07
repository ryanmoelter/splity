package com.ryanmoelter.splity.injection

import com.ryanmoelter.splity.Config
import com.ryanmoelter.splity.SentryWrapper
import com.ryanmoelter.splity.setUpSentry
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
