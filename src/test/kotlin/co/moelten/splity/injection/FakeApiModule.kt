package co.moelten.splity.injection

import co.moelten.splity.Config
import co.moelten.splity.FakeYnabClient
import co.moelten.splity.FakeYnabServerDatabase
import co.moelten.splity.SentryWrapper
import com.youneedabudget.client.YnabClient
import me.tatarka.inject.annotations.Component

@Component
abstract class FakeApiModule(
  val fakeYnabServerDatabase: FakeYnabServerDatabase
) : ApiModule {
  override fun ynabApi(config: Config, sentry: SentryWrapper): YnabClient =
    FakeYnabClient(fakeYnabServerDatabase)
}
