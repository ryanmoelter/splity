package co.moelten.splity

import co.moelten.splity.database.RealDatabaseModule
import co.moelten.splity.database.create
import co.moelten.splity.injection.RealApiModule
import co.moelten.splity.injection.RealConfigModule
import co.moelten.splity.injection.SplityComponent
import co.moelten.splity.injection.create
import kotlinx.coroutines.runBlocking

fun main() {
  val component = SplityComponent::class.create(
    RealDatabaseModule::class.create(),
    RealConfigModule::class.create(),
    RealApiModule::class.create()
  )
  val sentry = component.sentry

  sentry.doInTransaction(operation = "runBlocking()", name = "run splity") {
    runBlocking {
      sentry.doInSpan(operation = "mirrorTransactions()") {
        component.transactionMirrorer.mirrorTransactions()
      }
    }
  }
}
