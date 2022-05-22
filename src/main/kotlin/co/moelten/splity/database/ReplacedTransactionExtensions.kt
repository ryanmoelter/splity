package co.moelten.splity.database

import co.moelten.splity.database.UpdateField.AMOUNT
import co.moelten.splity.database.UpdateField.DATE
import co.moelten.splity.database.UpdateField.values
import co.moelten.splity.models.PublicTransactionDetail
import co.moelten.splity.models.toPublicTransactionDetail
import com.ryanmoelter.ynab.ReplacedSubTransaction
import com.ryanmoelter.ynab.ReplacedTransaction
import com.ryanmoelter.ynab.StoredSubTransaction
import com.ryanmoelter.ynab.StoredTransaction

fun StoredTransaction.toReplacedTransaction() = ReplacedTransaction(
  id = id,
  date = date,
  amount = amount,
  cleared = cleared,
  approved = approved,
  accountId = accountId,
  accountName = accountName,
  memo = memo,
  flagColor = flagColor,
  payeeId = payeeId,
  categoryId = categoryId,
  transferAccountId = transferAccountId,
  transferTransactionId = transferTransactionId,
  matchedTransactionId = matchedTransactionId,
  importId = importId,
  payeeName = payeeName,
  categoryName = categoryName,
  budgetId = budgetId
)

fun StoredSubTransaction.toReplacedSubTransaction() = ReplacedSubTransaction(
  id = id,
  transactionId = transactionId,
  amount = amount,
  memo = memo,
  payeeId = payeeId,
  payeeName = payeeName,
  categoryId = categoryId,
  categoryName = categoryName,
  transferAccountId = transferAccountId,
  transferTransactionId = transferTransactionId,
  accountId = accountId,
  budgetId = budgetId
)

fun StoredTransaction.calculateUpdatedFieldsFrom(
  replacedTransaction: StoredTransaction
): Set<UpdateField> =
  this.toPublicTransactionDetail(emptyList())
    .calculateUpdatedFieldsFrom(replacedTransaction.toPublicTransactionDetail(emptyList()))

fun PublicTransactionDetail.calculateUpdatedFieldsFrom(
  replaced: PublicTransactionDetail,
  complement: PublicTransactionDetail? = null
): Set<UpdateField> {
  assert(subTransactions.isEmpty()) { "Cannot update a transaction that has sub-transactions" }

  // Loop over values to make sure we don't forget any
  return values()
    .filter { updateField ->
      when (updateField) {
        AMOUNT -> amount != replaced.amount
        DATE -> date != replaced.date
      }
    }
    .filter { updateField ->
      complement == null || when (updateField) {
        AMOUNT -> complement.amount != -amount
        DATE -> complement.date != date
      }
    }
    .toSet()
}

enum class UpdateField(val shouldNotify: Boolean) {
  AMOUNT(true), DATE(true),
}
