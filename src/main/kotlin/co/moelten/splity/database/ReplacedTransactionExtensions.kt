package co.moelten.splity.database

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
