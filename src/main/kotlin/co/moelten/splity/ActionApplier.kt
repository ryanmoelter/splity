package co.moelten.splity

import co.moelten.splity.database.Repository
import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.SaveTransaction
import com.youneedabudget.client.models.SaveTransactionWrapper
import com.youneedabudget.client.models.SaveTransactionsWrapper
import com.youneedabudget.client.models.TransactionDetail
import me.tatarka.inject.annotations.Inject

@Inject
class ActionApplier(
  val repository: Repository,
  val ynab: YnabClient
) {
  suspend fun applyUpdate(
    action: TransactionAction.Update,
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
    action: TransactionAction.Create,
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
