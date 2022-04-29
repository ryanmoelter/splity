package co.moelten.splity.database

import com.ryanmoelter.ynab.StoredSubTransaction
import com.ryanmoelter.ynab.StoredTransaction
import com.youneedabudget.client.models.SubTransaction
import com.youneedabudget.client.models.TransactionDetail

data class TransactionsAndSubTransactions(
  val transactions: List<StoredTransaction>,
  val subTransactions: List<StoredSubTransaction>
)

fun List<TransactionDetail>.toUnprocessedStoredTransactions(
  budgetId: BudgetId
): TransactionsAndSubTransactions {
  val newSubTransactions = mutableListOf<StoredSubTransaction>()
  val newTransactions = buildList {
    this@toUnprocessedStoredTransactions.forEach { transactionDetail ->
      add(transactionDetail.toStoredTransaction(budgetId))
      val subTransactions = transactionDetail.subtransactions
      if (subTransactions.isNotEmpty()) {
        newSubTransactions += subTransactions.toUnprocessedStoredSubTransactions(
          budgetId,
          transactionDetail.accountId.toAccountId()
        )
      }
    }
  }

  return TransactionsAndSubTransactions(newTransactions, newSubTransactions)
}

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

fun List<SubTransaction>.toUnprocessedStoredSubTransactions(
  budgetId: BudgetId,
  accountId: AccountId
): List<StoredSubTransaction> {
  return buildList {
    this@toUnprocessedStoredSubTransactions.forEach { subTransaction ->
      add(subTransaction.toStoredSubTransaction(accountId, budgetId))
    }
  }
}

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
