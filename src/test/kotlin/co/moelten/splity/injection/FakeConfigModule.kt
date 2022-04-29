package co.moelten.splity.injection

import co.moelten.splity.Config
import co.moelten.splity.fakeConfig
import me.tatarka.inject.annotations.Component

@Component
abstract class FakeConfigModule : ConfigModule {
  override fun config(): Config = fakeConfig
}
