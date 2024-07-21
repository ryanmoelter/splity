package com.ryanmoelter.splity.injection

import com.ryanmoelter.splity.Config
import com.ryanmoelter.splity.FakeYnabClient
import com.ryanmoelter.splity.FakeYnabServerDatabase
import com.ryanmoelter.splity.SentryWrapper
import com.ynab.client.YnabClient
import me.tatarka.inject.annotations.Component

@Component
abstract class FakeApiModule(
  sentryModule: SentryModule,
  val fakeYnabServerDatabase: FakeYnabServerDatabase,
) : ApiModule(sentryModule) {
  override fun ynabApi(
    config: Config,
    sentry: SentryWrapper,
  ): YnabClient = FakeYnabClient(fakeYnabServerDatabase)
}
