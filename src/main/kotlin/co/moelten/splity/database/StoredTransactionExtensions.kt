package co.moelten.splity.database

import co.moelten.splity.models.PublicSubTransaction
import co.moelten.splity.models.PublicTransactionDetail
import com.ryanmoelter.ynab.GetUnprocessedAndFlaggedExcept
import com.ryanmoelter.ynab.StoredSubTransaction
import com.ryanmoelter.ynab.StoredTransaction
import com.youneedabudget.client.models.SubTransaction
import com.youneedabudget.client.models.TransactionDetail

fun TransactionDetail.toStoredTransaction(
  budgetId: BudgetId,
  processedState: ProcessedState = if (deleted) ProcessedState.DELETED else ProcessedState.CREATED
) =
  StoredTransaction(
    id = id.toTransactionId(),
    date = date,
    amount = amount,
    cleared = cleared,
    approved = approved,
    accountId = accountId.toAccountId(),
    accountName = accountName,
    memo = memo,
    flagColor = flagColor,
    payeeId = payeeId?.toPayeeId(),
    categoryId = categoryId?.toCategoryId(),
    transferAccountId = transferAccountId?.toAccountId(),
    transferTransactionId = transferTransactionId?.toTransactionId(),
    matchedTransactionId = matchedTransactionId?.toTransactionId(),
    importId = importId,
    payeeName = payeeName,
    categoryName = categoryName,
    processedState = processedState,
    budgetId = budgetId
  )

fun SubTransaction.toStoredSubTransaction(
  accountId: AccountId,
  budgetId: BudgetId,
  processedState: ProcessedState = if (deleted) ProcessedState.DELETED else ProcessedState.CREATED
) = StoredSubTransaction(
  id = id.toSubTransactionId(),
  transactionId = transactionId.toTransactionId(),
  amount = amount,
  memo = memo,
  payeeId = payeeId?.toPayeeId(),
  payeeName = payeeName,
  categoryId = categoryId?.toCategoryId(),
  categoryName = categoryName,
  transferAccountId = transferAccountId?.toAccountId(),
  transferTransactionId = transferTransactionId?.toTransactionId(),
  processedState = processedState,
  accountId = accountId,
  budgetId = budgetId
)

fun PublicTransactionDetail.toStoredTransaction(): StoredTransaction = StoredTransaction(
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
  processedState = processedState,
  budgetId = budgetId
)

fun PublicSubTransaction.toStoredSubTransaction(): StoredSubTransaction = StoredSubTransaction(
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
  processedState = processedState,
  accountId = accountId,
  budgetId = budgetId
)

fun GetUnprocessedAndFlaggedExcept.toStoredTransaction(): StoredTransaction = StoredTransaction(
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
  processedState = processedState,
  budgetId = budgetId,
)

fun List<TransactionDetail>.toPublicTransactionDetailList(
  budgetId: BudgetId,
  overrideProcessedState: ProcessedState? = null
) = this.map { it.toPublicTransactionDetail(budgetId, overrideProcessedState) }

fun TransactionDetail.toPublicTransactionDetail(
  budgetId: BudgetId,
  overrideProcessedState: ProcessedState? = null
): PublicTransactionDetail {
  val processedState = overrideProcessedState ?: if (deleted) {
    ProcessedState.DELETED
  } else {
    ProcessedState.CREATED
  }
  return PublicTransactionDetail(
    id = id.toTransactionId(),
    date = date,
    amount = amount,
    cleared = cleared,
    approved = approved,
    accountId = accountId.toAccountId(),
    accountName = accountName,
    memo = memo,
    flagColor = flagColor,
    payeeId = payeeId?.toPayeeId(),
    categoryId = categoryId?.toCategoryId(),
    transferAccountId = transferAccountId?.toAccountId(),
    transferTransactionId = transferTransactionId?.toTransactionId(),
    matchedTransactionId = matchedTransactionId?.toTransactionId(),
    importId = importId,
    payeeName = payeeName,
    categoryName = categoryName,
    processedState = processedState,
    budgetId = budgetId,
    subTransactions = this.subtransactions.map {
      it.toPublicSubTransaction(accountId.toAccountId(), budgetId, overrideProcessedState)
    }
  )
}

fun SubTransaction.toPublicSubTransaction(
  accountId: AccountId,
  budgetId: BudgetId,
  overrideProcessedState: ProcessedState? = null
): PublicSubTransaction {
  val processedState = overrideProcessedState ?: if (deleted) {
    ProcessedState.DELETED
  } else {
    ProcessedState.CREATED
  }
  return PublicSubTransaction(
    id = id.toSubTransactionId(),
    transactionId = transactionId.toTransactionId(),
    amount = amount,
    memo = memo,
    payeeId = payeeId?.toPayeeId(),
    payeeName = payeeName,
    categoryId = categoryId?.toCategoryId(),
    categoryName = categoryName,
    transferAccountId = transferAccountId?.toAccountId(),
    transferTransactionId = transferTransactionId?.toTransactionId(),
    processedState = processedState,
    accountId = accountId,
    budgetId = budgetId
  )
}
