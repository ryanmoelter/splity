package co.moelten.splity

import co.moelten.splity.TransactionAction.Create
import co.moelten.splity.TransactionAction.Delete
import co.moelten.splity.TransactionAction.Update
import co.moelten.splity.database.AccountId
import co.moelten.splity.database.BudgetId
import co.moelten.splity.database.Repository
import co.moelten.splity.database.firstAccountAndBudget
import co.moelten.splity.database.secondAccountAndBudget
import co.moelten.splity.models.PublicTransactionDetail
import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.Account
import com.youneedabudget.client.models.BudgetSummary
import com.youneedabudget.client.models.SaveTransaction
import com.youneedabudget.client.models.TransactionDetail
import com.youneedabudget.client.models.TransactionDetail.ClearedEnum
import com.youneedabudget.client.models.TransactionDetail.ClearedEnum.CLEARED
import com.youneedabudget.client.models.TransactionDetail.ClearedEnum.RECONCILED
import com.youneedabudget.client.models.TransactionDetail.ClearedEnum.UNCLEARED
import me.tatarka.inject.annotations.Inject
import org.threeten.bp.LocalDate
import java.util.UUID
import kotlin.math.absoluteValue

@Inject
class TransactionMirrorer(
  val config: Config,
  val repository: Repository,
  val ynab: YnabClient,
  val actionApplier: ActionApplier
) {
  suspend fun mirrorTransactions() {
    repository.fetchNewTransactions()

    val syncData = repository.getSyncData()!!

    val firstTransactions =
      repository.getTransactionsByAccount(syncData.firstAccountId)
    val secondTransactions =
      repository.getTransactionsByAccount(syncData.secondAccountId)

    val actions = createDifferentialActionsForBothAccounts(
      firstTransactions = firstTransactions,
      firstAccountAndBudget = syncData.firstAccountAndBudget,
      secondTransactions = secondTransactions,
      secondAccountAndBudget = syncData.secondAccountAndBudget,
      startDate = config.startDate
    )

    applyActions(actions)
  }

  fun createDifferentialActionsForBothAccounts(
    firstTransactions: List<PublicTransactionDetail>,
    firstAccountAndBudget: AccountAndBudget,
    secondTransactions: List<PublicTransactionDetail>,
    secondAccountAndBudget: AccountAndBudget,
    startDate: LocalDate
  ): List<CompleteTransactionAction> {
    // TODO: Match against all transactions, not just new ones
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

  internal suspend fun applyActions(
    actions: List<CompleteTransactionAction>,
  ) {
    actions.forEach { action ->
      println("Apply: $action")
      action.apply(actionApplier = actionApplier)
    }
  }
}

data class CompleteTransactionAction(
  val transactionAction: TransactionAction,
  val fromAccountAndBudget: AccountAndBudget,
  val toAccountAndBudget: AccountAndBudget
) {
  suspend fun apply(
    actionApplier: ActionApplier
  ) = transactionAction.apply(
    fromAccountAndBudget = fromAccountAndBudget,
    toAccountAndBudget = toAccountAndBudget,
    actionApplier = actionApplier
  )
}

sealed class TransactionAction {
  data class Create(val fromTransaction: PublicTransactionDetail) : TransactionAction()
  data class Update(
    val fromTransaction: PublicTransactionDetail,
    val toTransaction: PublicTransactionDetail,
    val updateFields: Set<UpdateField>
  ) : TransactionAction()

  data class Delete(val transactionId: UUID) : TransactionAction()
}

enum class UpdateField {
  CLEAR, AMOUNT
}

suspend fun TransactionAction.apply(
  fromAccountAndBudget: AccountAndBudget,
  toAccountAndBudget: AccountAndBudget,
  actionApplier: ActionApplier
): Unit = when (this) {
  is Create -> actionApplier.applyCreate(
    action = this,
    fromAccountAndBudget = fromAccountAndBudget,
    toAccountAndBudget = toAccountAndBudget
  )
  is Update -> actionApplier.applyUpdate(
    action = this,
    toAccountAndBudget = toAccountAndBudget
  )
  is Delete -> TODO()
}

fun getExtraDetailsForMemo(totalAmount: Long, paidAmount: Long, isBaseEmpty: Boolean): String {
  return if (isBaseEmpty) {
    ""
  } else {
    " • "
  } +
    "Out of ${totalAmount.absoluteValue.toMoneyString()}, " +
    "you paid ${paidAmount.absolutePercentageOf(totalAmount).toPercentageString()}"
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

data class AccountAndBudget(val accountId: AccountId, val budgetId: BudgetId)
data class TransactionDescription(val payeeName: String?, val memo: String?, val totalAmount: Long)

val PublicTransactionDetail.transactionDescription
  get() = TransactionDescription(
    payeeName = payeeName,
    memo = memo,
    totalAmount = amount
  )

fun List<BudgetSummary>.findByName(name: String) =
  find { it.name == name } ?: throw IllegalStateException("Can't find budget: \"$name\"")

fun List<Account>.findByName(name: String) =
  find { it.name == name } ?: throw IllegalStateException("Can't find account: \"$name\"")
