package co.moelten.splity

import co.moelten.splity.TransactionAction.Create
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
    val ryansTransactions = ynab.transactions.getTransactionsByAccount(
      budgetId = ryansBudget.id.toString(),
      accountId = ryansSplitwiseAccountId.toString(),
      sinceDate = null,
      type = null,
      lastKnowledgeOfServer = null
    ).data.transactions
    val ryansAccountAndBudget = AccountAndBudget(ryansSplitwiseAccountId, ryansBudget.id)

    val sarahsBudget = budgetResponse.budgets.findByName(SARAHS_BUDGET_NAME)
    val sarahsSplitwiseAccountId = sarahsBudget.accounts!!.findByName(SARAHS_SPLITWISE_ACCOUNT_NAME).id
    val sarahsTransactions = ynab.transactions.getTransactionsByAccount(
      budgetId = sarahsBudget.id.toString(),
      accountId = sarahsSplitwiseAccountId.toString(),
      sinceDate = null,
      type = null,
      lastKnowledgeOfServer = null
    ).data.transactions
    val sarahsAccountAndBudget = AccountAndBudget(sarahsSplitwiseAccountId, sarahsBudget.id)

    var actions = createActionsFromOneAccount(
      fromTransactions = ryansTransactions,
      toTransactions = sarahsTransactions
    ).map { transactionAction ->
      CompleteTransactionAction(
        transactionAction = transactionAction,
        fromAccountAndBudget = ryansAccountAndBudget,
        toAccountAndBudget = sarahsAccountAndBudget
      )
    }
    actions = actions + createActionsFromOneAccount(
      fromTransactions = sarahsTransactions,
      toTransactions = ryansTransactions
    ).map { transactionAction ->
      CompleteTransactionAction(
        transactionAction = transactionAction,
        fromAccountAndBudget = sarahsAccountAndBudget,
        toAccountAndBudget = ryansAccountAndBudget
      )
    }

    val otherTransactions = mutableMapOf<UUID, List<TransactionDetail>>()

    actions.forEach { action ->
      println("Apply: $action")
      action.apply(ynab, getOtherAccountTransactions = { accountAndBudget ->
        otherTransactions[accountAndBudget.accountId] ?: ynab.transactions.getTransactionsByAccount(
          accountAndBudget.budgetId.toString(),
          accountAndBudget.accountId.toString(),
          null,
          null,
          null
        )
          .data
          .transactions
          .also { transactions -> otherTransactions[accountAndBudget.accountId] = transactions }
      })
    }
  }
}

fun createActionsFromOneAccount(
  fromTransactions: List<TransactionDetail>,
  toTransactions: List<TransactionDetail>
): List<TransactionAction> {
  val toTransactionsMap = toTransactions
    .map { it.id to it }
    .toMap()
  val toTransactionsImportMap = toTransactions
    .filter { it.importId != null }
    .map { it.importId!! to it }
    .toMap()

  return fromTransactions
    .filter { transactionDetail -> !(transactionDetail.payeeName?.startsWith("Starting Balance") ?: false) }
    .mapNotNull { fromTransaction ->
      when {
        fromTransaction.doesNotExistIn(idMap = toTransactionsMap, importIdMap = toTransactionsImportMap) && fromTransaction.approved -> {
          Create(fromTransaction)
        }
        else -> null
      }
    }
}

private fun TransactionDetail.doesNotExistIn(
  idMap: Map<String, TransactionDetail>,
  importIdMap: Map<String, TransactionDetail>
) = !existsIn(idMap, importIdMap)

private fun TransactionDetail.existsIn(
  idMap: Map<String, TransactionDetail>,
  importIdMap: Map<String, TransactionDetail>
) = idMap.containsKey(importId) || importIdMap.containsKey(id)

class CompleteTransactionAction(
  val transactionAction: TransactionAction,
  val fromAccountAndBudget: AccountAndBudget,
  val toAccountAndBudget: AccountAndBudget
) {
  suspend fun apply(
    ynab: YnabClient,
    getOtherAccountTransactions: suspend (AccountAndBudget) -> List<TransactionDetail>
  ) = transactionAction.apply(
    ynab = ynab,
    fromAccountAndBudget = fromAccountAndBudget,
    toAccountAndBudget = toAccountAndBudget,
    getOtherAccountTransactions = getOtherAccountTransactions
  )
}

sealed class TransactionAction {
  data class Create(val fromTransaction: TransactionDetail) : TransactionAction()
  data class Update(val fromTransaction: TransactionDetail, val toTransaction: TransactionDetail) : TransactionAction()
  data class Delete(val transactionId: UUID) : TransactionAction()
}

suspend fun TransactionAction.apply(
  ynab: YnabClient,
  fromAccountAndBudget: AccountAndBudget,
  toAccountAndBudget: AccountAndBudget,
  getOtherAccountTransactions: suspend (AccountAndBudget) -> List<TransactionDetail>
) = when (this) {
  is Create -> {
    val transactionDescription = if (fromTransaction.transferAccountId != null) {
      val otherAccountTransactions = getOtherAccountTransactions(AccountAndBudget(fromTransaction.transferAccountId!!, fromAccountAndBudget.budgetId))
      otherAccountTransactions
        .find { transactionDetail ->
          transactionDetail.subtransactions.any { it.transferTransactionId == fromTransaction.id }
        }
        ?.transactionDescription
        ?: otherAccountTransactions
          .find { transactionDetail -> transactionDetail.transferTransactionId == fromTransaction.id }!!
          .let { transactionDetail -> TransactionDescription("Chicken Butt", transactionDetail.memo) }
    } else {
      fromTransaction.transactionDescription
    }
    ynab.transactions.createTransaction(
      toAccountAndBudget.budgetId.toString(),
      SaveTransactionsWrapper(
        SaveTransaction(
          accountId = toAccountAndBudget.accountId,
          date = fromTransaction.date,
          amount = -fromTransaction.amount,
          payeeId = null,
          payeeName = transactionDescription.payeeName,
          categoryId = null,
          memo = transactionDescription.memo,
          cleared = SaveTransaction.ClearedEnum.CLEARED,
          approved = false,
          flagColor = null,
          importId = fromTransaction.id,
          subtransactions = null
        )
      )
    )
  }
  is Update -> TODO()
  is Delete -> TODO()
}

data class AccountAndBudget(val accountId: UUID, val budgetId: UUID)
data class TransactionDescription(val payeeName: String?, val memo: String?)

val TransactionDetail.transactionDescription get() = TransactionDescription(payeeName = payeeName, memo = memo)

fun List<BudgetSummary>.findByName(name: String) =
  find { it.name == name } ?: throw IllegalStateException("Can't find budget: \"$name\"")

fun List<Account>.findByName(name: String) =
  find { it.name == name } ?: throw IllegalStateException("Can't find account: \"$name\"")
