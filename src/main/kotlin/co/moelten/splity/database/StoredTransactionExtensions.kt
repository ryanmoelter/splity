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
      with(transactionDetail) {
        add(
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
            processedState = if (deleted) ProcessedState.DELETED else ProcessedState.CREATED,
            budgetId = budgetId
          )
        )
        if (subtransactions.isNotEmpty()) {
          newSubTransactions +=
            subtransactions.toUnprocessedStoredSubTransactions(budgetId, accountId.toAccountId())
        }
      }
    }
  }

  return TransactionsAndSubTransactions(newTransactions, newSubTransactions)
}

fun List<SubTransaction>.toUnprocessedStoredSubTransactions(
  budgetId: BudgetId,
  accountId: AccountId
): List<StoredSubTransaction> {
  return buildList {
    this@toUnprocessedStoredSubTransactions.forEach { subTransaction ->
      with(subTransaction) {
        add(
          StoredSubTransaction(
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
            processedState = if (deleted) ProcessedState.DELETED else ProcessedState.CREATED,
            accountId = accountId,
            budgetId = budgetId
          )
        )
      }
    }
  }
}

// TODO: Move to a domain model, basically TransactionDetail + ProcessedState + BudgetId
fun List<StoredTransaction>.toApiTransactions(
  subTransactions: Map<TransactionId, List<StoredSubTransaction>>
) =
  map { storedTransaction ->
    with(storedTransaction) {
      TransactionDetail(
        id = id.toString(),
        date = date,
        amount = amount,
        cleared = cleared,
        approved = approved,
        accountId = accountId.id,
        deleted = processedState == ProcessedState.DELETED,
        accountName = accountName,
        subtransactions = subTransactions[id]!!.toApiSubTransactions(),
        memo = memo,
        flagColor = flagColor,
        payeeId = payeeId?.id,
        categoryId = categoryId?.id,
        transferAccountId = transferAccountId?.id
      )
    }
  }

fun List<StoredSubTransaction>.toApiSubTransactions() = map {
  with(it) {
    SubTransaction(
      id = id.toString(),
      transactionId = transactionId.toString(),
      amount = amount,
      deleted = processedState == ProcessedState.DELETED,
      memo = memo,
      payeeId = payeeId?.id,
      payeeName = payeeName,
      categoryId = categoryId?.id,
      categoryName = categoryName,
      transferAccountId = transferAccountId?.id
    )
  }
}
