package co.moelten.splity.injection

import co.moelten.splity.Config
import co.moelten.splity.SentryWrapper
import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.YnabClientImpl
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Singleton
interface ApiModule {
  @Provides
  @Singleton
  fun ynabApi(config: Config, sentry: SentryWrapper): YnabClient
}

@Component
abstract class RealApiModule : ApiModule {
  override fun ynabApi(config: Config, sentry: SentryWrapper): YnabClient =
    YnabClientImpl(config.ynabToken, sentry::doInImmediateSpan)
}
