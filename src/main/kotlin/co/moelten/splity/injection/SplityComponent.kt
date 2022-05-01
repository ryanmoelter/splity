package co.moelten.splity.injection

import co.moelten.splity.Config
import co.moelten.splity.SentryWrapper
import co.moelten.splity.database.DatabaseModule
import co.moelten.splity.setUpSentry
import com.ryanmoelter.ynab.database.Database
import com.youneedabudget.client.YnabClient
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Component
@Singleton
abstract class SplityComponent(
  @Component val databaseModule: DatabaseModule,
  @Component val configModule: ConfigModule,
  @Component val apiModule: ApiModule
) {
  abstract val config: Config
  abstract val database: Database
  abstract val api: YnabClient
  abstract val sentry: SentryWrapper

  @Provides
  @Singleton
  fun sentryWrapper(config: Config): SentryWrapper = setUpSentry(config.sentryConfig, config.version)
}
