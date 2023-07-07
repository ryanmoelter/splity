package com.ryanmoelter.splity.injection

import com.ryanmoelter.splity.Config
import com.ryanmoelter.splity.FakeYnabServerDatabase
import com.ryanmoelter.splity.database.FakeDatabaseModule
import com.ryanmoelter.splity.database.create
import com.ryanmoelter.splity.fakeConfig

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
