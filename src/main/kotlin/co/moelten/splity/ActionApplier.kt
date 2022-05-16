package co.moelten.splity

import co.moelten.splity.TransactionAction.CreateComplement
import co.moelten.splity.TransactionAction.DeleteComplement
import co.moelten.splity.TransactionAction.MarkError
import co.moelten.splity.TransactionAction.UpdateComplement
import co.moelten.splity.UpdateField.AMOUNT
import co.moelten.splity.UpdateField.CLEAR
import co.moelten.splity.UpdateField.DATE
import co.moelten.splity.database.Repository
import co.moelten.splity.models.PublicTransactionDetail
import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.SaveTransaction
import com.youneedabudget.client.models.SaveTransactionWrapper
import com.youneedabudget.client.models.SaveTransactionsWrapper
import com.youneedabudget.client.models.TransactionDetail
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

    ynab.transactions.createTransaction(
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
  }

  private suspend fun applyUpdate(action: UpdateComplement) = with(action) {
    var cleared = complement.cleared
    var amount = complement.amount
    var date = complement.date
    val shouldNotify = updateFields.any { it.shouldNotify }
    updateFields.forEach { updateField ->
      when (updateField) {
        CLEAR -> cleared = TransactionDetail.ClearedEnum.CLEARED
        AMOUNT -> amount = -fromTransaction.amount
        DATE -> date = fromTransaction.date
      }
    }
    ynab.transactions.updateTransaction(
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
          cleared = cleared.toSaveTransactionClearedEnum(),
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
          subtransactions = null  // TODO: handle subTransactions
        )
      )
    )
  }

  private suspend fun applyDelete(action: DeleteComplement) = with(action) {
    ynab.transactions.updateTransaction(
      fromTransaction.budgetId.toString(),
      fromTransaction.id.string,
      SaveTransactionWrapper(
        fromTransaction.toSaveTransaction().copy(
          flagColor = SaveTransaction.FlagColorEnum.RED,
          approved = false
        )
      )
    )
  }

  private suspend fun applyError(action: MarkError): Unit = with(action) {
    TODO()
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
  subtransactions = null // TODO: Support updating sub-transactions
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
    val message: String
  ) : TransactionAction {
    companion object ErrorMessages {
      const val BOTH_UPDATED =
        "Both this and its complement have been updated; delete one to bring them back in sync."
      const val UPDATED_WITHOUT_COMPLEMENT =
        "This updated transaction has no complement; this shouldn't be possible, but manually " +
          "create a complement transaction in the other account to bring them back in sync."
    }
  }
}

enum class UpdateField(val shouldNotify: Boolean) {
  CLEAR(false), AMOUNT(true), DATE(true),
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
