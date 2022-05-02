package co.moelten.splity

import co.moelten.splity.TransactionAction.Create
import co.moelten.splity.TransactionAction.Delete
import co.moelten.splity.TransactionAction.Update
import co.moelten.splity.database.Repository
import co.moelten.splity.models.PublicTransactionDetail
import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.SaveTransaction
import com.youneedabudget.client.models.SaveTransactionWrapper
import com.youneedabudget.client.models.SaveTransactionsWrapper
import com.youneedabudget.client.models.TransactionDetail
import me.tatarka.inject.annotations.Inject
import java.util.UUID
import kotlin.math.absoluteValue

@Inject
class ActionApplier(
  val repository: Repository,
  val ynab: YnabClient
) {

  suspend fun applyActions(
    actions: List<CompleteTransactionAction>,
  ) {
    actions.forEach { action ->
      println("Apply: $action")
      action.apply(actionApplier = this)
    }
  }

  suspend fun applyUpdate(
    action: Update,
    toAccountAndBudget: AccountAndBudget
  ) {
    var cleared = action.toTransaction.cleared
    action.updateFields.forEach { updateField ->
      when (updateField) {
        UpdateField.CLEAR -> cleared =
          if (action.fromTransaction.approved) TransactionDetail.ClearedEnum.CLEARED else TransactionDetail.ClearedEnum.UNCLEARED
        UpdateField.AMOUNT -> TODO()
      }
    }
    ynab.transactions.updateTransaction(
      toAccountAndBudget.budgetId.toString(),
      action.toTransaction.id.string,
      SaveTransactionWrapper(
        SaveTransaction(
          accountId = action.toTransaction.accountId.plainUuid,
          date = action.toTransaction.date,
          amount = action.toTransaction.amount,
          payeeId = action.toTransaction.payeeId?.plainUuid,
          payeeName = null,
          categoryId = action.toTransaction.categoryId?.plainUuid,
          memo = action.toTransaction.memo,
          cleared = cleared.toSaveTransactionClearedEnum(),
          approved = action.toTransaction.approved,
          flagColor = action.toTransaction.flagColor?.toSaveTransactionFlagColorEnum(),
          importId = action.toTransaction.importId,
          subtransactions = null
        )
      )
    )
  }

  suspend fun applyCreate(
    action: Create,
    fromAccountAndBudget: AccountAndBudget,
    toAccountAndBudget: AccountAndBudget
  ) {
    val transactionDescription = if (action.fromTransaction.transferAccountId != null) {
      val parentOfSplitTransaction =
        repository.getTransactionBySubTransactionTransferId(action.fromTransaction.id)

      parentOfSplitTransaction
        ?.transactionDescription
        ?: repository.getTransactionByTransferId(
          action.fromTransaction.id
        )!!
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
          accountId = toAccountAndBudget.accountId.plainUuid,
          date = action.fromTransaction.date,
          amount = -action.fromTransaction.amount,
          payeeId = null,
          payeeName = transactionDescription.payeeName,
          categoryId = null,
          memo = (transactionDescription.memo + getExtraDetailsForMemo(
            transactionDescription.totalAmount,
            action.fromTransaction.amount,
            transactionDescription.memo.isNullOrEmpty()
          )).trim(),
          cleared = SaveTransaction.ClearedEnum.CLEARED,
          approved = false,
          flagColor = null,
          importId = "splity:${-action.fromTransaction.amount}:${action.fromTransaction.date}:1",
          subtransactions = null
        )
      )
    )
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
