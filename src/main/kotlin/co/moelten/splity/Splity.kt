package co.moelten.splity

import co.moelten.splity.TransactionAction.Approve
import co.moelten.splity.TransactionAction.Create
import co.moelten.splity.TransactionAction.CreateFromTransfer
import co.moelten.splity.TransactionAction.Delete
import co.moelten.splity.TransactionAction.Update
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
    .filter { transactionDetail -> !(transactionDetail.payeeName?.startsWith("Starting Balance") ?: false) }
    .forEach { fromTransaction ->
      if (!toTransactions.containsKey(fromTransaction.importId) && fromTransaction.approved) {
        CompleteTransactionAction(Create(fromTransaction), toAccountAndBudget).apply(ynab)
      }
    }
}

class CompleteTransactionAction(val transactionAction: TransactionAction, val toAccountAndBudget: AccountAndBudget) {
  suspend fun apply(ynab: YnabClient) = transactionAction.apply(ynab, toAccountAndBudget)
}

sealed class TransactionAction {
  data class Create(val fromTransaction: TransactionDetail) : TransactionAction()
  data class CreateFromTransfer(
    val fromTransaction: TransactionDetail,
    val fromTransferTransaction: TransactionDetail
  ) : TransactionAction()
  data class Approve(val transactionId: UUID) : TransactionAction()
  data class Update(val fromTransaction: TransactionDetail) : TransactionAction()
  data class Delete(val transactionId: UUID) : TransactionAction()
}

suspend fun TransactionAction.apply(ynab: YnabClient, toAccountAndBudget: AccountAndBudget) = when (this) {
  is Create -> {
    ynab.transactions.createTransaction(
      toAccountAndBudget.budgetId.toString(),
      SaveTransactionsWrapper(
        SaveTransaction(
          accountId = toAccountAndBudget.accountId,
          date = fromTransaction.date,
          amount = -fromTransaction.amount,
          payeeId = null,
          payeeName = fromTransaction.payeeName,
          categoryId = null,
          memo = fromTransaction.memo,
          cleared = SaveTransaction.ClearedEnum.CLEARED,
          approved = false,
          flagColor = null,
          importId = fromTransaction.id,
          subtransactions = null
        )
      )
    )
  }
  is CreateFromTransfer -> TODO()
  is Approve -> TODO()
  is Update -> TODO()
  is Delete -> TODO()
}

data class TransactionsAndActions(val transactions: List<TransactionDetail>, val actions: List<TransactionAction>)
data class AccountAndBudget(val accountId: UUID, val budgetId: UUID)

fun List<BudgetSummary>.findByName(name: String) =
  find { it.name == name } ?: throw IllegalStateException("Can't find budget: \"$name\"")

fun List<Account>.findByName(name: String) =
  find { it.name == name } ?: throw IllegalStateException("Can't find account: \"$name\"")
