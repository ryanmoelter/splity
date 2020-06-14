package co.moelten.splity

import com.sksamuel.hoplite.ConfigLoader
import com.youneedabudget.client.YnabClientImpl
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() {
  runBlocking {
    val config = ConfigLoader().loadConfigOrThrow<Config>(File("./config.yaml"))
    val ynab = YnabClientImpl(config.ynabToken)

    val budgetResponse = ynab.budgets.getBudgets(includeAccounts = true).data

    mirrorTransactions(ynab = ynab, budgetResponse = budgetResponse, config = config)
    ensureZeroBalanceOnCreditCards(ynab = ynab, config = config, budgetResponse = budgetResponse)
  }
}
