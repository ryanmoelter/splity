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
import org.threeten.bp.LocalDate
import java.util.UUID
import kotlin.math.absoluteValue

suspend fun mirrorTransactions(
  ynab: YnabClient,
  budgetResponse: BudgetSummaryResponseData,
  config: Config
) {

  val (firstTransactions, firstAccountAndBudget) =
    getTransactionsAndIds(ynab, budgetResponse, config.firstAccount)
  val (secondTransactions, secondAccountAndBudget) =
    getTransactionsAndIds(ynab, budgetResponse, config.secondAccount)

  val otherAccountTransactionsCache = OtherAccountTransactionsCache(ynab)

  val actions = createActionsForBothAccounts(
    firstTransactions = firstTransactions,
    firstAccountAndBudget = firstAccountAndBudget,
    secondTransactions = secondTransactions,
    secondAccountAndBudget = secondAccountAndBudget,
    startDate = config.startDate
  )

  applyActions(ynab, actions, otherAccountTransactionsCache)
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
  otherAccountTransactionsCache: OtherAccountTransactionsCache
) {
  actions.forEach { action ->
    println("Apply: $action")
    action.apply(ynab, otherAccountTransactionsCache)
  }
}

fun createActionsForBothAccounts(
  firstTransactions: List<TransactionDetail>,
  firstAccountAndBudget: AccountAndBudget,
  secondTransactions: List<TransactionDetail>,
  secondAccountAndBudget: AccountAndBudget,
  startDate: LocalDate
): List<CompleteTransactionAction> {
  var filteredFirstTransactions = firstTransactions
    .filter { it.date.isAfter(startDate.minusDays(1)) }
  var filteredSecondTransactions = secondTransactions
    .filter { it.date.isAfter(startDate.minusDays(1)) }

  firstTransactions.forEach { transactionDetail ->
    val complement = secondTransactions
      .find { it.date == transactionDetail.date && it.amount == -transactionDetail.amount }

    if (complement != null) {
      filteredFirstTransactions = filteredFirstTransactions - transactionDetail
      filteredSecondTransactions = filteredSecondTransactions - complement
    }
  }

  return filteredFirstTransactions
    .filter { it.approved }
    .map { transactionDetail ->
      CompleteTransactionAction(
        transactionAction = Create(transactionDetail),
        fromAccountAndBudget = firstAccountAndBudget,
        toAccountAndBudget = secondAccountAndBudget
      )
    } + filteredSecondTransactions
    .filter { it.approved }
    .map { transactionDetail ->
      CompleteTransactionAction(
        transactionAction = Create(transactionDetail),
        fromAccountAndBudget = secondAccountAndBudget,
        toAccountAndBudget = firstAccountAndBudget
      )
    }
}

data class CompleteTransactionAction(
  val transactionAction: TransactionAction,
  val fromAccountAndBudget: AccountAndBudget,
  val toAccountAndBudget: AccountAndBudget
) {
  suspend fun apply(
    ynab: YnabClient,
    otherAccountTransactionsCache: OtherAccountTransactionsCache
  ) = transactionAction.apply(
    ynab = ynab,
    fromAccountAndBudget = fromAccountAndBudget,
    toAccountAndBudget = toAccountAndBudget,
    otherAccountTransactionsCache = otherAccountTransactionsCache
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
  otherAccountTransactionsCache: OtherAccountTransactionsCache
): Unit = when (this) {
  is Create -> applyCreate(
    action = this,
    otherAccountTransactionsCache = otherAccountTransactionsCache,
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
  otherAccountTransactionsCache: OtherAccountTransactionsCache,
  fromAccountAndBudget: AccountAndBudget,
  ynab: YnabClient,
  toAccountAndBudget: AccountAndBudget
) {
  val transactionDescription = if (action.fromTransaction.transferAccountId != null) {
    val otherAccountTransactions = otherAccountTransactionsCache.getOtherAccountTransactions(
      AccountAndBudget(
        action.fromTransaction.transferAccountId!!,
        fromAccountAndBudget.budgetId
      )
    )

    val parentOfSplitTransaction = otherAccountTransactions
      .find { transactionDetail ->
        transactionDetail.subtransactions.any { it.transferTransactionId == action.fromTransaction.id }
      }

    parentOfSplitTransaction
      ?.transactionDescription
      ?: otherAccountTransactions
        .find { transactionDetail -> transactionDetail.transferTransactionId == action.fromTransaction.id }!!
        .let { transactionDetail ->
          TransactionDescription(
            "Chicken Butt",
            transactionDetail.memo,
            transactionDetail.amount
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
        memo = transactionDescription.memo + getExtraDetailsForMemo(
          transactionDescription.totalAmount,
          action.fromTransaction.amount
        ),
        cleared = SaveTransaction.ClearedEnum.CLEARED,
        approved = false,
        flagColor = null,
        importId = "splity:${-action.fromTransaction.amount}:${action.fromTransaction.date}:1",
        subtransactions = null
      )
    )
  )
}

fun getExtraDetailsForMemo(totalAmount: Long, paidAmount: Long): String {
  return """


    Out of ${totalAmount.absoluteValue.toMoneyString()}, you paid ${paidAmount.absolutePercentageOf(totalAmount).toPercentageString()}
  """.trimIndent()
}

/** Convert a YNAB amount (int representing value * 1000) into a $X.XX string, avoiding precision loss */
fun Long.toMoneyString() = "\$${"%,d".format(this / 1000)}.${"%02d".format((this / 10) % 100)}"

/** Calculate this as a percentage of [total], represented as a double from 0-100 */
fun Long.absolutePercentageOf(total: Long) = ((this * 100).toDouble() / total).absoluteValue

/** Convert a percentage (double representing value in percent, i.e. 0-100) into a percentage string */
fun Double.toPercentageString() = "%.1f".format(this) + "%"

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
data class TransactionDescription(val payeeName: String?, val memo: String?, val totalAmount: Long)

val TransactionDetail.transactionDescription get() = TransactionDescription(
  payeeName = payeeName,
  memo = memo,
  totalAmount = amount
)
fun List<BudgetSummary>.findByName(name: String) =
  find { it.name == name } ?: throw IllegalStateException("Can't find budget: \"$name\"")

fun List<Account>.findByName(name: String) =
  find { it.name == name } ?: throw IllegalStateException("Can't find account: \"$name\"")
