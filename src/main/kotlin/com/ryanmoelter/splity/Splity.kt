package com.ryanmoelter.splity

import com.ryanmoelter.splity.database.RealDatabaseModule
import com.ryanmoelter.splity.database.create
import com.ryanmoelter.splity.injection.RealApiModule
import com.ryanmoelter.splity.injection.RealConfigModule
import com.ryanmoelter.splity.injection.SentryModule
import com.ryanmoelter.splity.injection.SplityComponent
import com.ryanmoelter.splity.injection.create
import kotlinx.coroutines.runBlocking

fun main() {
  val component = createSplityComponent()
  val sentry = component.sentry

  sentry.doInTransaction(operation = "runBlocking()", name = "run splity") {
    runBlocking {
      sentry.doInSpan(operation = "mirrorTransactions()") {
        component.transactionMirrorer.mirrorTransactions()
      }
    }
  }
}

private fun createSplityComponent(): SplityComponent {
  val sentryModule = SentryModule::class.create()
  return SplityComponent::class.create(
    RealDatabaseModule::class.create(),
    RealConfigModule::class.create(),
    RealApiModule::class.create(sentryModule),
    sentryModule
  )
}
