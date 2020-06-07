package co.moelten.splity

import co.moelten.splity.TransactionAction.Create
import co.moelten.splity.TransactionAction.Delete
import co.moelten.splity.TransactionAction.Update
import co.moelten.splity.UpdateField.AMOUNT
import co.moelten.splity.UpdateField.CLEAR
import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.Account
import com.youneedabudget.client.models.BudgetSummary
import com.youneedabudget.client.models.SaveTransaction
import com.youneedabudget.client.models.SaveTransactionWrapper
import com.youneedabudget.client.models.SaveTransactionsWrapper
import com.youneedabudget.client.models.TransactionDetail
import com.youneedabudget.client.models.TransactionDetail.ClearedEnum.CLEARED
import com.youneedabudget.client.models.TransactionDetail.ClearedEnum.RECONCILED
import com.youneedabudget.client.models.TransactionDetail.ClearedEnum.UNCLEARED
import com.youneedabudget.client.models.TransactionDetail.FlagColorEnum.BLUE
import com.youneedabudget.client.models.TransactionDetail.FlagColorEnum.GREEN
import com.youneedabudget.client.models.TransactionDetail.FlagColorEnum.ORANGE
import com.youneedabudget.client.models.TransactionDetail.FlagColorEnum.PURPLE
import com.youneedabudget.client.models.TransactionDetail.FlagColorEnum.RED
import com.youneedabudget.client.models.TransactionDetail.FlagColorEnum.YELLOW
import kotlinx.coroutines.runBlocking
import java.util.UUID

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

    val actions = createActionsFromOneAccount(
      fromTransactions = ryansTransactions,
      toTransactions = sarahsTransactions
    ).map { transactionAction ->
      CompleteTransactionAction(
        transactionAction = transactionAction,
        fromAccountAndBudget = ryansAccountAndBudget,
        toAccountAndBudget = sarahsAccountAndBudget
      )
    } + createActionsFromOneAccount(
      fromTransactions = sarahsTransactions,
      toTransactions = ryansTransactions
    ).map { transactionAction ->
      CompleteTransactionAction(
        transactionAction = transactionAction,
        fromAccountAndBudget = sarahsAccountAndBudget,
        toAccountAndBudget = ryansAccountAndBudget
      )
    }

    applyActions(ynab, actions)
  }
}

internal suspend fun applyActions(
  ynab: YnabClient,
  actions: List<CompleteTransactionAction>,
  otherTransactions: MutableMap<UUID, List<TransactionDetail>> = mutableMapOf()
) {
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
        fromTransaction.existsIn(idMap = toTransactionsMap, importIdMap = toTransactionsImportMap) -> {
          val toTransaction = fromTransaction.findIn(idMap = toTransactionsMap, importIdMap = toTransactionsImportMap)!!
          val updates = findDifferences(fromTransaction = fromTransaction, toTransaction = toTransaction)
          if (updates.isNotEmpty()) {
            Update(fromTransaction = fromTransaction, toTransaction = toTransaction, updateFields = updates)
          } else {
            null
          }
        }
        else -> null
      }
    }
}

private fun findDifferences(fromTransaction: TransactionDetail, toTransaction: TransactionDetail): Set<UpdateField> {
  val result = mutableSetOf<UpdateField>()
  if (fromTransaction.approved && toTransaction.cleared == UNCLEARED) {
    result.add(CLEAR)
  }
  // TODO: amount
  return result
}

private fun TransactionDetail.doesNotExistIn(
  idMap: Map<String, TransactionDetail>,
  importIdMap: Map<String, TransactionDetail>
) = !existsIn(idMap, importIdMap)

private fun TransactionDetail.existsIn(
  idMap: Map<String, TransactionDetail>,
  importIdMap: Map<String, TransactionDetail>
) = idMap.containsKey(importId) || importIdMap.containsKey(id)

private fun TransactionDetail.findIn(
  idMap: Map<String, TransactionDetail>,
  importIdMap: Map<String, TransactionDetail>
) = idMap[importId] ?: importIdMap[id]

data class CompleteTransactionAction(
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
  data class Update(
    val fromTransaction: TransactionDetail,
    val toTransaction: TransactionDetail,
    val updateFields: Set<UpdateField>
  ) : TransactionAction()
  data class Delete(val transactionId: UUID) : TransactionAction()
}

enum class UpdateField {
  CLEAR, AMOUNT
}

suspend fun TransactionAction.apply(
  ynab: YnabClient,
  fromAccountAndBudget: AccountAndBudget,
  toAccountAndBudget: AccountAndBudget,
  getOtherAccountTransactions: suspend (AccountAndBudget) -> List<TransactionDetail>
): Unit = when (this) {
  is Create -> applyCreate(
    action = this,
    getOtherAccountTransactions = getOtherAccountTransactions,
    fromAccountAndBudget = fromAccountAndBudget,
    ynab = ynab,
    toAccountAndBudget = toAccountAndBudget
  )
  is Update -> applyUpdate(
    ynab = ynab,
    toAccountAndBudget = toAccountAndBudget
  )
  is Delete -> TODO()
}

private suspend fun Update.applyUpdate(
  ynab: YnabClient,
  toAccountAndBudget: AccountAndBudget
) {
  var cleared = toTransaction.cleared
  updateFields.forEach { updateField ->
    when (updateField) {
      CLEAR -> cleared = if (fromTransaction.approved) CLEARED else UNCLEARED
      AMOUNT -> TODO()
    }
  }
  ynab.transactions.updateTransaction(
    toAccountAndBudget.budgetId.toString(),
    toTransaction.id,
    SaveTransactionWrapper(
      SaveTransaction(
        accountId = toTransaction.accountId,
        date = toTransaction.date,
        amount = toTransaction.amount,
        payeeId = toTransaction.payeeId,
        payeeName = null,
        categoryId = toTransaction.categoryId,
        memo = toTransaction.memo,
        cleared = cleared.toSaveTransactionClearedEnum(),
        approved = toTransaction.approved,
        flagColor = toTransaction.flagColor?.toSaveTransactionFlagColorEnum(),
        importId = toTransaction.importId,
        subtransactions = null
      )
    )
  )
}

private suspend fun applyCreate(
  action: Create,
  getOtherAccountTransactions: suspend (AccountAndBudget) -> List<TransactionDetail>,
  fromAccountAndBudget: AccountAndBudget,
  ynab: YnabClient,
  toAccountAndBudget: AccountAndBudget
) {
  val transactionDescription = if (action.fromTransaction.transferAccountId != null) {
    val otherAccountTransactions = getOtherAccountTransactions(
      AccountAndBudget(
        action.fromTransaction.transferAccountId!!,
        fromAccountAndBudget.budgetId
      )
    )
    otherAccountTransactions
      .find { transactionDetail ->
        transactionDetail.subtransactions.any { it.transferTransactionId == action.fromTransaction.id }
      }
      ?.transactionDescription
      ?: otherAccountTransactions
        .find { transactionDetail -> transactionDetail.transferTransactionId == action.fromTransaction.id }!!
        .let { transactionDetail -> TransactionDescription("Chicken Butt", transactionDetail.memo) }
  } else {
    action.fromTransaction.transactionDescription
  }
  ynab.transactions.createTransaction(
    toAccountAndBudget.budgetId.toString(),
    SaveTransactionsWrapper(
      SaveTransaction(
        accountId = toAccountAndBudget.accountId,
        date = action.fromTransaction.date,
        amount = -action.fromTransaction.amount,
        payeeId = null,
        payeeName = transactionDescription.payeeName,
        categoryId = null,
        memo = transactionDescription.memo,
        cleared = SaveTransaction.ClearedEnum.CLEARED,
        approved = false,
        flagColor = null,
        importId = action.fromTransaction.id,
        subtransactions = null
      )
    )
  )
}

fun TransactionDetail.ClearedEnum.toSaveTransactionClearedEnum() = when (this) {
  CLEARED -> SaveTransaction.ClearedEnum.CLEARED
  UNCLEARED -> SaveTransaction.ClearedEnum.UNCLEARED
  RECONCILED -> SaveTransaction.ClearedEnum.RECONCILED
}

fun TransactionDetail.FlagColorEnum.toSaveTransactionFlagColorEnum() = when (this) {
  RED -> SaveTransaction.FlagColorEnum.RED
  ORANGE -> SaveTransaction.FlagColorEnum.ORANGE
  YELLOW -> SaveTransaction.FlagColorEnum.YELLOW
  GREEN -> SaveTransaction.FlagColorEnum.GREEN
  BLUE -> SaveTransaction.FlagColorEnum.BLUE
  PURPLE -> SaveTransaction.FlagColorEnum.PURPLE
}

data class AccountAndBudget(val accountId: UUID, val budgetId: UUID)
data class TransactionDescription(val payeeName: String?, val memo: String?)

val TransactionDetail.transactionDescription get() = TransactionDescription(payeeName = payeeName, memo = memo)

fun List<BudgetSummary>.findByName(name: String) =
  find { it.name == name } ?: throw IllegalStateException("Can't find budget: \"$name\"")

fun List<Account>.findByName(name: String) =
  find { it.name == name } ?: throw IllegalStateException("Can't find account: \"$name\"")
