package co.moelten.splity.injection

import co.moelten.splity.Config
import co.moelten.splity.FakeYnabServerDatabase
import co.moelten.splity.database.FakeDatabaseModule
import co.moelten.splity.database.create
import co.moelten.splity.fakeConfig

fun createFakeSplityComponent(
  serverDatabase: FakeYnabServerDatabase,
  config: Config = fakeConfig
): SplityComponent {
  val sentryModule = SentryModule::class.create()
  return SplityComponent::class.create(
    databaseModule = FakeDatabaseModule::class.create(),
    configModule = FakeConfigModule::class.create(config),
    apiModule = FakeApiModule::class.create(sentryModule, serverDatabase),
    sentryModule
  )
}
