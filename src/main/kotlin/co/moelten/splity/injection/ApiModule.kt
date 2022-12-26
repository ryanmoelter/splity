package co.moelten.splity.injection

import co.moelten.splity.Config
import co.moelten.splity.SentryWrapper
import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.YnabClientImpl
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class ApiSingleton

@ApiSingleton
abstract class ApiModule(@Component val sentryModule: SentryModule) {
  @Provides
  @ApiSingleton
  abstract fun ynabApi(config: Config, sentry: SentryWrapper): YnabClient
}

@Component
abstract class RealApiModule(sentryModule: SentryModule) : ApiModule(sentryModule) {
  override fun ynabApi(config: Config, sentry: SentryWrapper): YnabClient =
    YnabClientImpl(config.ynabToken, sentry::doInImmediateSpan)
}
