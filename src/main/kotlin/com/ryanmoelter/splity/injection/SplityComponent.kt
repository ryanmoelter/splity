package com.ryanmoelter.splity.injection

import com.ryanmoelter.splity.ActionApplier
import com.ryanmoelter.splity.ActionCreator
import com.ryanmoelter.splity.Config
import com.ryanmoelter.splity.SentryWrapper
import com.ryanmoelter.splity.TransactionMirrorer
import com.ryanmoelter.splity.database.DatabaseModule
import com.ryanmoelter.ynab.database.Database
import com.ynab.client.YnabClient
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
