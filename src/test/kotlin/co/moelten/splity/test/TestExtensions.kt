package co.moelten.splity.test

import co.moelten.splity.database.AccountId
import co.moelten.splity.database.BudgetId
import co.moelten.splity.database.ProcessedState
import co.moelten.splity.database.toAccountId
import co.moelten.splity.database.toCategoryId
import co.moelten.splity.database.toPayeeId
import co.moelten.splity.database.toSubTransactionId
import co.moelten.splity.database.toTransactionId
import co.moelten.splity.models.PublicSubTransaction
import co.moelten.splity.models.PublicTransactionDetail
import com.ryanmoelter.ynab.StoredSubTransaction
import com.ryanmoelter.ynab.StoredTransaction
import com.ryanmoelter.ynab.database.Database
import com.youneedabudget.client.models.SubTransaction
import com.youneedabudget.client.models.TransactionDetail

typealias Setup<Subject> = (Subject.() -> Unit) -> Unit

fun Database.addTransactions(vararg transactions: PublicTransactionDetail) {
  storedTransactionQueries.transaction {
    transactions.forEach { transaction ->
      storedTransactionQueries.replaceSingle(transaction.toStoredTransaction())
      transaction.subTransactions.forEach { subTransaction ->
        storedSubTransactionQueries.replaceSingle(subTransaction.toStoredSubTransaction())
      }
    }
  }
}

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

fun PublicTransactionDetail.toApiTransaction(): TransactionDetail = TransactionDetail(
  id = id.string,
  date = date,
  amount = amount,
  cleared = cleared,
  approved = approved,
  deleted = processedState == ProcessedState.DELETED,
  accountId = accountId.plainUuid,
  accountName = accountName,
  memo = memo,
  flagColor = flagColor,
  payeeId = payeeId?.plainUuid,
  categoryId = categoryId?.plainUuid,
  transferAccountId = transferAccountId?.plainUuid,
  transferTransactionId = transferTransactionId?.string,
  matchedTransactionId = matchedTransactionId?.string,
  importId = importId,
  payeeName = payeeName,
  categoryName = categoryName,
  subtransactions = subTransactions.map { it.toApiSubTransaction() }
)

fun PublicSubTransaction.toApiSubTransaction(): SubTransaction = SubTransaction(
  id = id.toString(),
  transactionId = transactionId.string,
  amount = amount,
  memo = memo,
  deleted = processedState == ProcessedState.DELETED,
  payeeId = payeeId?.plainUuid,
  payeeName = payeeName,
  categoryId = categoryId?.plainUuid,
  categoryName = categoryName,
  transferAccountId = transferAccountId?.plainUuid,
  transferTransactionId = transferTransactionId?.string
)

fun TransactionDetail.toPublicTransactionDetail(
  processedState: ProcessedState,
  budgetId: BudgetId
) =
  PublicTransactionDetail(
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
    subTransactions = subtransactions.map {
      it.toPublicSubTransaction(
        processedState,
        budgetId,
        accountId.toAccountId()
      )
    },
    processedState = processedState,
    budgetId = budgetId,
  )

fun SubTransaction.toPublicSubTransaction(
  processedState: ProcessedState,
  budgetId: BudgetId,
  accountId: AccountId
) = PublicSubTransaction(
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
  budgetId = budgetId,
)
