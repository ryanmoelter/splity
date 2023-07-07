package com.ryanmoelter.splity

import com.ryanmoelter.splity.TransactionAction.CreateComplement
import com.ryanmoelter.splity.TransactionAction.DeleteComplement
import com.ryanmoelter.splity.TransactionAction.MarkError
import com.ryanmoelter.splity.TransactionAction.MarkProcessed
import com.ryanmoelter.splity.TransactionAction.UpdateComplement
import com.ryanmoelter.splity.database.ProcessedState.UP_TO_DATE
import com.ryanmoelter.splity.database.Repository
import com.ryanmoelter.splity.database.UpdateField
import com.ryanmoelter.splity.database.UpdateField.AMOUNT
import com.ryanmoelter.splity.database.UpdateField.DATE
import com.ryanmoelter.splity.database.UpdateField.FLAG
import com.ryanmoelter.splity.database.UpdateField.values
import com.ryanmoelter.splity.models.PublicSubTransaction
import com.ryanmoelter.splity.models.PublicTransactionDetail
import com.ynab.client.YnabClient
import com.ynab.client.models.PostTransactionsWrapper
import com.ynab.client.models.PutTransactionWrapper
import com.ynab.client.models.SaveSubTransaction
import com.ynab.client.models.SaveTransaction
import me.tatarka.inject.annotations.Inject
import kotlin.math.absoluteValue

const val MAX_MEMO_LENGTH = 200

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
      val otherSideOfTransferParent =
        repository.getTransactionBySubTransactionTransferId(fromTransaction.id)
      val otherSideOfTransferChild =
        repository.getSubTransactionByTransferId(fromTransaction.id)

      when {
        otherSideOfTransferParent != null && !otherSideOfTransferChild?.memo.isNullOrBlank() -> {
          otherSideOfTransferParent.transactionDescription
            .copy(memo = otherSideOfTransferChild!!.memo.orEmpty())
        }
        otherSideOfTransferParent != null -> {
          otherSideOfTransferParent.transactionDescription
        }
        else -> {
          val otherSideOfTransfer =
            repository.getTransactionByTransferTransactionId(fromTransaction.id)!!
          TransactionDescription(
            payeeName = "Chicken Butt",
            memo = otherSideOfTransfer.memo.orEmpty(),
            totalAmount = otherSideOfTransfer.amount
          )
        }
      }
    } else {
      fromTransaction.transactionDescription
    }

    val response = ynab.transactions.createTransaction(
      toAccountAndBudget.budgetId.toString(),
      PostTransactionsWrapper(
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
            transactionDescription.memo.isEmpty()
          )).trim().trimEndToMeetLengthRequirements(),
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
    if (updateFields.any { it.updatesComplement }) {
      var amount = complement.amount
      var date = complement.date
      updateFields.forEach { updateField ->
        when (updateField) {
          AMOUNT -> amount = -fromTransaction.amount
          DATE -> date = fromTransaction.date
          FLAG -> {}
        }
      }

      val response = ynab.transactions.updateTransaction(
        complement.budgetId.toString(),
        complement.id.string,
        PutTransactionWrapper(
          SaveTransaction(
            accountId = complement.accountId.plainUuid,
            date = date,
            amount = amount,
            payeeId = complement.payeeId?.plainUuid,
            payeeName = complement.payeeName,
            categoryId = complement.categoryId?.plainUuid,
            memo = complement.memo,
            cleared = complement.cleared.toSaveTransactionClearedEnum(),
            approved = false,
            flagColor = SaveTransaction.FlagColorEnum.BLUE,
            importId = complement.importId,
            subtransactions = action.complement.subTransactions.map { it.toSaveSubTransaction() }
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
      PutTransactionWrapper(
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
      values().forEach {
        when (it) {
          AMOUNT -> amount = action.revertFromTransactionUpdatedFieldsTo.amount
          DATE -> date = action.revertFromTransactionUpdatedFieldsTo.date
          FLAG -> {}
        }
      }
    }

    val firstResponse = ynab.transactions.updateTransaction(
      fromTransaction.budgetId.toString(),
      fromTransaction.id.string,
      PutTransactionWrapper(
        fromTransaction.toSaveTransaction().copy(
          flagColor = SaveTransaction.FlagColorEnum.RED,
          amount = amount,
          date = date,
          approved = false,
          memo = ("ERROR: " + message + " • " + fromTransaction.memo)
            .trimEndToMeetLengthRequirements()
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
        PutTransactionWrapper(
          complement.toSaveTransaction().copy(
            flagColor = SaveTransaction.FlagColorEnum.RED,
            approved = false,
            memo = ("ERROR: " + message + " • " + complement.memo).trimEndToMeetLengthRequirements()
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
  memo = memo?.trimEndToMeetLengthRequirements(),
  cleared = cleared.toSaveTransactionClearedEnum(),
  approved = approved,
  flagColor = flagColor?.toSaveTransactionFlagColorEnum(),
  importId = importId,
  subtransactions = subTransactions.map { it.toSaveSubTransaction() }
)

fun PublicSubTransaction.toSaveSubTransaction(): SaveSubTransaction = SaveSubTransaction(
  amount = amount,
  payeeId = payeeId?.plainUuid,
  payeeName = payeeName,
  categoryId = categoryId?.plainUuid,
  memo = memo?.trimEndToMeetLengthRequirements()
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

fun String.trimEndToMeetLengthRequirements() = take(MAX_MEMO_LENGTH)
