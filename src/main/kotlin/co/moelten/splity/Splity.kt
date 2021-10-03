package co.moelten.splity

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import com.youneedabudget.client.YnabClientImpl
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() {
  val configLoader = ConfigLoader.Builder()
    .addDecoder(DateDecoder())
    .addSource(PropertySource.resource("/version.properties"))
    .build()
  val config = configLoader.loadConfigOrThrow<Config>(File("./config.yaml"))
  if (config.sentryConfig != null) {
    setUpSentry(config.sentryConfig, config.version)
  }

  doInTransaction(operation = "runBlocking()", name = "run splity") {
    runBlocking {
      val ynab = YnabClientImpl(config.ynabToken, ::doInSpan)

      doInSpan(operation = "run (suspended)") {
        val budgetResponse = ynab.budgets.getBudgets(includeAccounts = true).data

        doInSpan(operation = "mirrorTransactions()") {
          mirrorTransactions(ynab = ynab, budgetResponse = budgetResponse, config = config)
        }
        if (config.ensureZeroBalanceOnCreditCards) {
          doInSpan(operation = "ensureZeroBalanceOnCreditCards()") {
            ensureZeroBalanceOnCreditCards(ynab = ynab, config = config, budgetResponse = budgetResponse)
          }
        }
      }
    }
  }
}
