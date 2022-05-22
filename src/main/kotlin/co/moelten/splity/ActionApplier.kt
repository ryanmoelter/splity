package co.moelten.splity

import co.moelten.splity.TransactionAction.CreateComplement
import co.moelten.splity.TransactionAction.DeleteComplement
import co.moelten.splity.TransactionAction.MarkError
import co.moelten.splity.TransactionAction.MarkProcessed
import co.moelten.splity.TransactionAction.UpdateComplement
import co.moelten.splity.database.ProcessedState.UP_TO_DATE
import co.moelten.splity.database.Repository
import co.moelten.splity.database.UpdateField
import co.moelten.splity.database.UpdateField.AMOUNT
import co.moelten.splity.database.UpdateField.DATE
import co.moelten.splity.models.PublicTransactionDetail
import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.SaveTransaction
import com.youneedabudget.client.models.SaveTransactionWrapper
import com.youneedabudget.client.models.SaveTransactionsWrapper
import me.tatarka.inject.annotations.Inject
import kotlin.math.absoluteValue

@Inject
class ActionApplier(
  val repository: Repository,
  val ynab: YnabClient
) {

  suspend fun applyActions(
    actions: List<TransactionAction>,
  ) = applyActions(*actions.toTypedArray())

  suspend fun applyActions(vararg actions: TransactionAction) {
    actions.forEach { action ->
      println("Apply: $action")
      when (action) {
        is CreateComplement -> applyCreate(action)
        is UpdateComplement -> applyUpdate(action)
        is DeleteComplement -> applyDelete(action)
        is MarkError -> applyError(action)
        is MarkProcessed -> applyProcessed(action)
      }
    }
  }

  private suspend fun applyCreate(action: CreateComplement) = with(action) {
    val transactionDescription = if (fromTransaction.transferAccountId != null) {
      val parentOfSplitTransaction =
        repository.getTransactionBySubTransactionTransferId(fromTransaction.id)

      parentOfSplitTransaction
        ?.transactionDescription
        ?: repository.getTransactionByTransferId(
          fromTransaction.id
        )!!
          .let { transactionDetail ->
            TransactionDescription(
              "Chicken Butt",
              transactionDetail.memo,
              transactionDetail.amount
            )
          }
    } else {
      fromTransaction.transactionDescription
    }

    val response = ynab.transactions.createTransaction(
      toAccountAndBudget.budgetId.toString(),
      SaveTransactionsWrapper(
        SaveTransaction(
          accountId = toAccountAndBudget.accountId.plainUuid,
          date = fromTransaction.date,
          amount = -fromTransaction.amount,
          payeeId = null,
          payeeName = transactionDescription.payeeName,
          categoryId = null,
          memo = (transactionDescription.memo + getExtraDetailsForMemo(
            transactionDescription.totalAmount,
            fromTransaction.amount,
            transactionDescription.memo.isNullOrEmpty()
          )).trim(),
          cleared = SaveTransaction.ClearedEnum.CLEARED,
          approved = false,
          flagColor = null,
          importId = "splity:${-fromTransaction.amount}:${fromTransaction.date}:1",
          subtransactions = null
        )
      )
    )
    repository.addOrUpdateTransaction(
      response.data.transaction!!,
      toAccountAndBudget.budgetId,
      UP_TO_DATE
    )
    repository.markProcessed(fromTransaction)
  }

  private suspend fun applyUpdate(action: UpdateComplement): Unit = with(action) {
    if (updateFields.isNotEmpty()) {
      var amount = complement.amount
      var date = complement.date
      val shouldNotify = updateFields.any { it.shouldNotify }
      updateFields.forEach { updateField ->
        when (updateField) {
          AMOUNT -> amount = -fromTransaction.amount
          DATE -> date = fromTransaction.date
        }
      }

      val response = ynab.transactions.updateTransaction(
        complement.budgetId.toString(),
        complement.id.string,
        SaveTransactionWrapper(
          SaveTransaction(
            accountId = complement.accountId.plainUuid,
            date = date,
            amount = amount,
            payeeId = complement.payeeId?.plainUuid,
            payeeName = complement.payeeName,
            categoryId = complement.categoryId?.plainUuid,
            memo = complement.memo,
            cleared = complement.cleared.toSaveTransactionClearedEnum(),
            approved = if (shouldNotify) {
              false
            } else {
              complement.approved
            },
            flagColor = if (shouldNotify) {
              SaveTransaction.FlagColorEnum.BLUE
            } else {
              complement.flagColor?.toSaveTransactionFlagColorEnum()
            },
            importId = complement.importId,
            subtransactions = if (action.complement.subTransactions.isEmpty()) {
              null
            } else {
              TODO("Handle subTransactions")
            }
          )
        )
      )

      repository.addOrUpdateTransaction(
        response.data.transaction,
        complement.budgetId,
        UP_TO_DATE
      )

      repository.markProcessed(fromTransaction)
    } else {
      repository.markProcessed(fromTransaction)
      repository.markProcessed(complement)
    }
  }

  private suspend fun applyDelete(action: DeleteComplement) = with(action) {
    val response = ynab.transactions.updateTransaction(
      complement.budgetId.toString(),
      complement.id.string,
      SaveTransactionWrapper(
        complement.toSaveTransaction().copy(
          flagColor = SaveTransaction.FlagColorEnum.RED,
          approved = false
        )
      )
    )

    repository.addOrUpdateTransaction(
      response.data.transaction,
      complement.budgetId,
      UP_TO_DATE
    )

    repository.markProcessed(fromTransaction)
  }

  private suspend fun applyError(action: MarkError): Unit = with(action) {
    var amount = action.fromTransaction.amount
    var date = action.fromTransaction.date
    if (action.revertFromTransactionUpdatedFieldsTo != null) {
      UpdateField.values().forEach {
        when (it) {
          AMOUNT -> amount = action.revertFromTransactionUpdatedFieldsTo.amount
          DATE -> date = action.revertFromTransactionUpdatedFieldsTo.date
        }
      }
    }

    val firstResponse = ynab.transactions.updateTransaction(
      fromTransaction.budgetId.toString(),
      fromTransaction.id.string,
      SaveTransactionWrapper(
        fromTransaction.toSaveTransaction().copy(
          flagColor = SaveTransaction.FlagColorEnum.RED,
          amount = amount,
          date = date,
          approved = false,
          memo = "ERROR: " + message + " • " + fromTransaction.memo
        )
      )
    )

    repository.addOrUpdateTransaction(
      firstResponse.data.transaction,
      fromTransaction.budgetId,
      fromTransaction.processedState
    )

    if (complement != null) {
      val complementResponse = ynab.transactions.updateTransaction(
        complement.budgetId.toString(),
        complement.id.string,
        SaveTransactionWrapper(
          complement.toSaveTransaction().copy(
            flagColor = SaveTransaction.FlagColorEnum.RED,
            approved = false,
            memo = "ERROR: " + message + " • " + complement.memo
          )
        )
      )

      repository.addOrUpdateTransaction(
        complementResponse.data.transaction,
        complement.budgetId,
        complement.processedState
      )
    }
  }

  private fun applyProcessed(action: MarkProcessed) = with(action) {
    repository.markProcessed(fromTransaction)
    if (complement != null) {
      repository.markProcessed(complement)
    }
  }
}

fun PublicTransactionDetail.toSaveTransaction(): SaveTransaction = SaveTransaction(
  accountId = accountId.plainUuid,
  date = date,
  amount = amount,
  payeeId = payeeId?.plainUuid,
  payeeName = payeeName,
  categoryId = categoryId?.plainUuid,
  memo = memo,
  cleared = cleared.toSaveTransactionClearedEnum(),
  approved = approved,
  flagColor = flagColor?.toSaveTransactionFlagColorEnum(),
  importId = importId,
  subtransactions = if (subTransactions.isEmpty()) {
    null
  } else {
    error("Unsupported: subTransactions on a SaveTransaction")
  }
)

sealed interface TransactionAction {
  val fromTransaction: PublicTransactionDetail

  data class CreateComplement(
    override val fromTransaction: PublicTransactionDetail,
    val toAccountAndBudget: AccountAndBudget,
  ) : TransactionAction

  data class UpdateComplement(
    override val fromTransaction: PublicTransactionDetail,
    val complement: PublicTransactionDetail,
    val updateFields: Set<UpdateField>,
  ) : TransactionAction

  data class DeleteComplement(
    override val fromTransaction: PublicTransactionDetail,
    val complement: PublicTransactionDetail,
  ) : TransactionAction

  data class MarkError(
    override val fromTransaction: PublicTransactionDetail,
    val complement: PublicTransactionDetail? = null,
    val message: String,
    val revertFromTransactionUpdatedFieldsTo: PublicTransactionDetail? = null
  ) : TransactionAction {
    companion object ErrorMessages {
      const val BOTH_UPDATED = "Both this and its complement have been updated; update both to " +
        "the same amount and date to bring them back in sync."
      const val UPDATED_TRANSFER_FROM_SPLIT = "Mirrors of transfers from split transactions " +
        "can't be updated, so the change has been undone. Change the mirror of this transaction " +
        "in the other budget instead."
      const val UPDATED_WITHOUT_COMPLEMENT = "This updated transaction has no complement; this " +
        "shouldn't be possible, so delete this transaction and re-create it to get things back " +
        "in sync."
    }
  }

  data class MarkProcessed(
    override val fromTransaction: PublicTransactionDetail,
    val complement: PublicTransactionDetail? = null
  ) : TransactionAction
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
