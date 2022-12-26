package co.moelten.splity.injection

import co.moelten.splity.ActionApplier
import co.moelten.splity.ActionCreator
import co.moelten.splity.Config
import co.moelten.splity.SentryWrapper
import co.moelten.splity.TransactionMirrorer
import co.moelten.splity.database.DatabaseModule
import com.ryanmoelter.ynab.database.Database
import com.youneedabudget.client.YnabClient
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Scope

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class Singleton

@Component
@Singleton
abstract class SplityComponent(
  @Component val databaseModule: DatabaseModule,
  @Component val configModule: ConfigModule,
  @Component val apiModule: ApiModule,
  @Component val sentryModule: SentryModule
) {
  abstract val config: Config
  abstract val database: Database
  abstract val api: YnabClient
  abstract val sentry: SentryWrapper
  abstract val transactionMirrorer: TransactionMirrorer
  abstract val actionApplier: ActionApplier
  abstract val actionCreator: ActionCreator
}
