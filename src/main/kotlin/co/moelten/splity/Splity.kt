package co.moelten.splity

import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.Account
import com.youneedabudget.client.models.BudgetSummary
import com.youneedabudget.client.models.SaveTransaction
import com.youneedabudget.client.models.SaveTransactionsWrapper
import com.youneedabudget.client.models.TransactionDetail
import kotlinx.coroutines.runBlocking
import java.util.UUID

// Copy these directly from YNAB, since some characters look similar but aren't (like apostrophes and dashes)
const val RYANS_BUDGET_NAME = "2019.1"
const val RYANS_SPLITWISE_ACCOUNT_NAME = "Split - Sarah"
const val SARAHS_BUDGET_NAME = "Sarah’s Budget 2020"
const val SARAHS_SPLITWISE_ACCOUNT_NAME = "Split - Ryan"

fun main() {
  runBlocking {
    val ynab = YnabClient()

    val budgetResponse = ynab.budgets.getBudgets(includeAccounts = true).data
    val ryansBudget = budgetResponse.budgets.findByName(RYANS_BUDGET_NAME)
    val ryansSplitwiseAccountId = ryansBudget.accounts!!.findByName(RYANS_SPLITWISE_ACCOUNT_NAME).id

    val sarahsBudget = budgetResponse.budgets.findByName(SARAHS_BUDGET_NAME)
    val sarahsSplitwiseAccountId = sarahsBudget.accounts!!.findByName(SARAHS_SPLITWISE_ACCOUNT_NAME).id

    copyFromRyanToSarah(
      ynab = ynab,
      ryansAccountAndBudget = AccountAndBudget(ryansSplitwiseAccountId, ryansBudget.id),
      sarahsAccountAndBudget = AccountAndBudget(sarahsSplitwiseAccountId, sarahsBudget.id)
    )
//    copyFromSarahToRyan(ynab)
  }
}

suspend fun copyFromRyanToSarah(
  ynab: YnabClient,
  ryansAccountAndBudget: AccountAndBudget,
  sarahsAccountAndBudget: AccountAndBudget
) {
  copyFromOneBudgetToAnother(
    ynab = ynab,
    fromAccountAndBudget = ryansAccountAndBudget,
    toAccountAndBudget = sarahsAccountAndBudget
  )
}

suspend fun copyFromSarahToRyan(
  ynabClient: YnabClient,
  ryansAccountAndBudget: AccountAndBudget,
  sarahsAccountAndBudget: AccountAndBudget
) {
}

suspend fun copyFromOneBudgetToAnother(
  ynab: YnabClient,
  fromAccountAndBudget: AccountAndBudget,
  toAccountAndBudget: AccountAndBudget
) {
  val fromTransactions = ynab.transactions.getTransactionsByAccount(
    budgetId = fromAccountAndBudget.budgetId.toString(),
    accountId = fromAccountAndBudget.accountId.toString(),
    sinceDate = null,
    type = null,
    lastKnowledgeOfServer = null
  ).data.transactions
    .map { it.id to it }
    .toMap()
  val toTransactions = ynab.transactions.getTransactionsByAccount(
    budgetId = toAccountAndBudget.budgetId.toString(),
    accountId = toAccountAndBudget.accountId.toString(),
    sinceDate = null,
    type = null,
    lastKnowledgeOfServer = null
  ).data.transactions
    .map { it.id to it }
    .toMap()

  fromTransactions.values
    .filter { transactionDetail ->
      !(transactionDetail.payeeName?.startsWith("Starting Balance") ?: false)
    }
    .forEach { transactionDetail ->
      if (!toTransactions.containsKey(transactionDetail.importId) && transactionDetail.approved) {
        ynab.transactions.createTransaction(
          toAccountAndBudget.budgetId.toString(),
          SaveTransactionsWrapper(
            SaveTransaction(
              accountId = toAccountAndBudget.accountId,
              date = transactionDetail.date,
              amount = -transactionDetail.amount,
              payeeId = null,
              payeeName = transactionDetail.payeeName,
              categoryId = null,
              memo = transactionDetail.memo,
              cleared = SaveTransaction.ClearedEnum.CLEARED,
              approved = false,
              flagColor = null,
              importId = transactionDetail.id,
              subtransactions = null
            )
          )
        )
      }
    }
}

sealed class TransactionAction(open val transactionDetail: TransactionDetail) {
  data class Create(override val transactionDetail: TransactionDetail) : TransactionAction(transactionDetail)
  data class Approve(override val transactionDetail: TransactionDetail) : TransactionAction(transactionDetail)
  data class Update(override val transactionDetail: TransactionDetail) : TransactionAction(transactionDetail)
  data class Delete(override val transactionDetail: TransactionDetail) : TransactionAction(transactionDetail)
  data class Replace(override val transactionDetail: TransactionDetail) : TransactionAction(transactionDetail)
}

data class TransactionsAndActions(val transactions: List<TransactionDetail>, val actions: List<TransactionAction>)
data class AccountAndBudget(val accountId: UUID, val budgetId: UUID)

fun List<BudgetSummary>.findByName(name: String) =
  find { it.name == name } ?: throw IllegalStateException("Can't find budget: \"$name\"")

fun List<Account>.findByName(name: String) =
  find { it.name == name } ?: throw IllegalStateException("Can't find account: \"$name\"")
