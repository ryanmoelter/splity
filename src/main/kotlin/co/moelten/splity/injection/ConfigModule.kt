package co.moelten.splity.injection

import co.moelten.splity.Config
import co.moelten.splity.DateDecoder
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import java.io.File

@Singleton
interface ConfigModule {
  @Provides
  @Singleton
  fun config(): Config
}

@Component
abstract class RealConfigModule : ConfigModule {

  override fun config(): Config = ConfigLoader.Builder()
    .addDecoder(DateDecoder())
    .addSource(PropertySource.resource("/version.properties"))
    .build()
    .loadConfigOrThrow(File("./config.yaml"))
}
