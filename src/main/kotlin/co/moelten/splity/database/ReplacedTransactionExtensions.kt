package co.moelten.splity.database

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

fun StoredTransaction.calculateUpdatedFieldsFrom(replacedTransaction: StoredTransaction): Set<UpdateField> =
  this.toPublicTransactionDetail(emptyList())
    .calculateUpdatedFieldsFrom(replacedTransaction.toPublicTransactionDetail(emptyList()))

fun PublicTransactionDetail.calculateUpdatedFieldsFrom(replaced: PublicTransactionDetail): Set<UpdateField> {
  assert(subTransactions.isEmpty()) { "Cannot update a transaction that has sub-transactions" }

  // Loop over values to make sure we don't forget any
  return UpdateField.values()
    .filter { updateField ->
      when (updateField) {
        UpdateField.CLEAR -> approved && !replaced.approved // Newly approved -> clear complement
        UpdateField.AMOUNT -> amount != replaced.amount
        UpdateField.DATE -> date != replaced.date
      }
    }
    .toSet()
}

enum class UpdateField(val shouldNotify: Boolean) {
  CLEAR(false), AMOUNT(true), DATE(true),
}
