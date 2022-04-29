package co.moelten.splity.injection

import co.moelten.splity.FakeYnabServerDatabase
import co.moelten.splity.database.FakeDatabaseModule
import co.moelten.splity.database.create

fun createFakeSplityComponent(serverDatabase: FakeYnabServerDatabase): SplityComponent =
  SplityComponent::class.create(
    databaseModule = FakeDatabaseModule::class.create(),
    configModule = FakeConfigModule::class.create(),
    apiModule = FakeApiModule::class.create(serverDatabase)
  )
