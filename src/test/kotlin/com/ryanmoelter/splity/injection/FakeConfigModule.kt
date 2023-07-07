package com.ryanmoelter.splity.injection

import com.ryanmoelter.splity.Config
import com.ryanmoelter.splity.fakeConfig
import me.tatarka.inject.annotations.Component

@Component
abstract class FakeConfigModule(
  val config: Config = fakeConfig
) : ConfigModule {
  override fun config(): Config = config
}
