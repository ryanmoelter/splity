package com.ryanmoelter.splity.database

import com.ryanmoelter.splity.database.UpdateField.AMOUNT
import com.ryanmoelter.splity.database.UpdateField.DATE
import com.ryanmoelter.splity.database.UpdateField.FLAG
import com.ryanmoelter.splity.database.UpdateField.values
import com.ryanmoelter.splity.models.PublicTransactionDetail
import com.ryanmoelter.ynab.ReplacedSubTransaction
import com.ryanmoelter.ynab.ReplacedTransaction
import com.ryanmoelter.ynab.StoredSubTransaction
import com.ryanmoelter.ynab.StoredTransaction

fun StoredTransaction.toReplacedTransaction() =
  ReplacedTransaction(
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
    budgetId = budgetId,
  )

fun StoredSubTransaction.toReplacedSubTransaction() =
  ReplacedSubTransaction(
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
    budgetId = budgetId,
  )

fun PublicTransactionDetail.calculateUpdatedFieldsFrom(
  replaced: PublicTransactionDetail,
  complement: PublicTransactionDetail? = null,
): Set<UpdateField> {
  // Loop over values to make sure we don't forget any
  return values()
    .filter { updateField ->
      when (updateField) {
        AMOUNT -> amount != replaced.amount
        DATE -> date != replaced.date
        FLAG -> flagColor != replaced.flagColor
      }
    }.filter { updateField ->
      complement == null ||
        when (updateField) {
          AMOUNT -> complement.amount != -amount
          DATE -> complement.date != date
          FLAG -> true
        }
    }.toSet()
}

enum class UpdateField(
  val updatesComplement: Boolean,
) {
  AMOUNT(true),
  DATE(true),
  FLAG(false),
}
