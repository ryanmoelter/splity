package co.moelten.splity

import co.moelten.splity.TransactionAction.Create
import co.moelten.splity.TransactionAction.Delete
import co.moelten.splity.TransactionAction.Update
import co.moelten.splity.UpdateField.AMOUNT
import co.moelten.splity.UpdateField.CLEAR
import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.Account
import com.youneedabudget.client.models.BudgetSummary
import com.youneedabudget.client.models.BudgetSummaryResponseData
import com.youneedabudget.client.models.SaveTransaction
import com.youneedabudget.client.models.SaveTransactionWrapper
import com.youneedabudget.client.models.SaveTransactionsWrapper
import com.youneedabudget.client.models.TransactionDetail
import com.youneedabudget.client.models.TransactionDetail.ClearedEnum
import com.youneedabudget.client.models.TransactionDetail.ClearedEnum.CLEARED
import com.youneedabudget.client.models.TransactionDetail.ClearedEnum.RECONCILED
import com.youneedabudget.client.models.TransactionDetail.ClearedEnum.UNCLEARED
import java.util.UUID

suspend fun mirrorTransactions(
  ynab: YnabClient,
  budgetResponse: BudgetSummaryResponseData,
  config: Config
) {

  val (firstTransactions, firstAccountAndBudget) =
    getTransactionsAndIds(ynab, budgetResponse, config.firstAccount)
  val (secondTransactions, secondAccountAndBudget) =
    getTransactionsAndIds(ynab, budgetResponse, config.secondAccount)

  val actions = createActionsFromOneAccount(
    fromTransactions = firstTransactions,
    toTransactions = secondTransactions
  ).map { transactionAction ->
    CompleteTransactionAction(
      transactionAction = transactionAction,
      fromAccountAndBudget = firstAccountAndBudget,
      toAccountAndBudget = secondAccountAndBudget
    )
  } + createActionsFromOneAccount(
    fromTransactions = secondTransactions,
    toTransactions = firstTransactions
  ).map { transactionAction ->
    CompleteTransactionAction(
      transactionAction = transactionAction,
      fromAccountAndBudget = secondAccountAndBudget,
      toAccountAndBudget = firstAccountAndBudget
    )
  }

  applyActions(ynab, actions)
}

private suspend fun getTransactionsAndIds(
  ynab: YnabClient,
  budgetResponse: BudgetSummaryResponseData,
  accoungConfig: AccountConfig
): Pair<List<TransactionDetail>, AccountAndBudget> {
  val budget = budgetResponse.budgets.findByName(accoungConfig.budgetName)
  val splitAccountId = budget.accounts!!.findByName(accoungConfig.accountName).id
  val transactions = ynab.transactions.getTransactionsByAccount(
    budgetId = budget.id.toString(),
    accountId = splitAccountId.toString(),
    sinceDate = null,
    type = null,
    lastKnowledgeOfServer = null
  ).data.transactions
  val accountAndBudget = AccountAndBudget(splitAccountId, budget.id)
  return Pair(transactions, accountAndBudget)
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
          val updates = findDifferences(
            fromTransaction = fromTransaction,
            toTransaction = toTransaction
          )
          if (updates.isNotEmpty()) {
            Update(
              fromTransaction = fromTransaction,
              toTransaction = toTransaction,
              updateFields = updates
            )
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
    // TODO: result.add(CLEAR)
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
        .let { transactionDetail ->
          TransactionDescription(
            "Chicken Butt",
            transactionDetail.memo
          )
        }
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

fun ClearedEnum.toSaveTransactionClearedEnum() = when (this) {
  CLEARED -> SaveTransaction.ClearedEnum.CLEARED
  UNCLEARED -> SaveTransaction.ClearedEnum.UNCLEARED
  RECONCILED -> SaveTransaction.ClearedEnum.RECONCILED
}

fun TransactionDetail.FlagColorEnum.toSaveTransactionFlagColorEnum() = when (this) {
  TransactionDetail.FlagColorEnum.RED -> SaveTransaction.FlagColorEnum.RED
  TransactionDetail.FlagColorEnum.ORANGE -> SaveTransaction.FlagColorEnum.ORANGE
  TransactionDetail.FlagColorEnum.YELLOW -> SaveTransaction.FlagColorEnum.YELLOW
  TransactionDetail.FlagColorEnum.GREEN -> SaveTransaction.FlagColorEnum.GREEN
  TransactionDetail.FlagColorEnum.BLUE -> SaveTransaction.FlagColorEnum.BLUE
  TransactionDetail.FlagColorEnum.PURPLE -> SaveTransaction.FlagColorEnum.PURPLE
}

data class AccountAndBudget(val accountId: UUID, val budgetId: UUID)
data class TransactionDescription(val payeeName: String?, val memo: String?)

val TransactionDetail.transactionDescription get() = TransactionDescription(
  payeeName = payeeName,
  memo = memo
)
fun List<BudgetSummary>.findByName(name: String) =
  find { it.name == name } ?: throw IllegalStateException("Can't find budget: \"$name\"")

fun List<Account>.findByName(name: String) =
  find { it.name == name } ?: throw IllegalStateException("Can't find account: \"$name\"")
