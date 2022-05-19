package co.moelten.splity.test

import co.moelten.splity.FakeYnabServerDatabase
import co.moelten.splity.database.AccountId
import co.moelten.splity.database.BudgetId
import co.moelten.splity.database.ProcessedState
import co.moelten.splity.database.TransactionId
import co.moelten.splity.database.replaceOnly
import co.moelten.splity.database.toAccountId
import co.moelten.splity.database.toCategoryId
import co.moelten.splity.database.toPayeeId
import co.moelten.splity.database.toPublicTransactionList
import co.moelten.splity.database.toStoredSubTransaction
import co.moelten.splity.database.toStoredTransaction
import co.moelten.splity.database.toSubTransactionId
import co.moelten.splity.database.toTransactionId
import co.moelten.splity.models.PublicSubTransaction
import co.moelten.splity.models.PublicTransactionDetail
import co.moelten.splity.models.toPublicTransactionDetail
import com.ryanmoelter.ynab.ReplacedSubTransaction
import com.ryanmoelter.ynab.ReplacedTransaction
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

fun Database.addReplacedTransactions(vararg transactions: PublicTransactionDetail) {
  replacedTransactionQueries.transaction {
    transactions.forEach { transaction ->
      replacedTransactionQueries.insert(transaction.toReplacedTransaction())
      transaction.subTransactions.forEach { subTransaction ->
        replacedSubTransactionQueries.insert(subTransaction.toReplacedSubTransaction())
      }
    }
  }
}

fun Database.getAllTransactions(): List<PublicTransactionDetail> =
  storedTransactionQueries.getAll().executeAsList().toPublicTransactionList(
    storedSubTransactionQueries.getAll().executeAsList()
  )

fun Database.getAllReplacedTransactions(): List<PublicTransactionDetail> =
  replacedTransactionQueries.getAll().executeAsList().toPublicTransactionList(
    replacedSubTransactionQueries.getAll().executeAsList()
  )

fun FakeYnabServerDatabase.syncServerKnowledge(localDatabase: Database) {
  syncServerKnowledge(localDatabase::also)
}

fun FakeYnabServerDatabase.syncServerKnowledge(setUpLocalDatabase: (Database.() -> Unit) -> Unit) {
  setUpLocalDatabase {
    syncDataQueries.replaceOnly(
      syncDataQueries.getOnly().executeAsOne()
        .copy(
          firstServerKnowledge = currentServerKnowledge,
          secondServerKnowledge = currentServerKnowledge
        )
    )
  }
}

fun List<ReplacedTransaction>.toPublicTransactionList(
  replacedSubTransactions: List<ReplacedSubTransaction>
): List<PublicTransactionDetail> {
  val subTransactionMap = buildMap<TransactionId, List<ReplacedSubTransaction>> {
    replacedSubTransactions.forEach { replacedSubTransaction ->
      put(
        replacedSubTransaction.transactionId,
        (get(replacedSubTransaction.transactionId) ?: emptyList()) + replacedSubTransaction
      )
    }
  }

  return map { replacedTransaction ->
    replacedTransaction.toPublicTransactionDetail(
      subTransactionMap[replacedTransaction.id] ?: emptyList()
    )
  }
}

fun PublicTransactionDetail.toReplacedTransaction(): ReplacedTransaction = ReplacedTransaction(
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

fun PublicSubTransaction.toReplacedSubTransaction(): ReplacedSubTransaction =
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

fun List<TransactionDetail>.toPublicTransactionDetailList(
  budgetId: BudgetId,
  processedState: ProcessedState = ProcessedState.CREATED
) = this.map { it.toPublicTransactionDetail(budgetId, processedState) }

fun TransactionDetail.toPublicTransactionDetail(
  budgetId: BudgetId,
  processedState: ProcessedState = if (deleted) ProcessedState.DELETED else ProcessedState.CREATED
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
    processedState = processedState,
    budgetId = budgetId,
    subTransactions = this.subtransactions.map {
      it.toPublicSubTransaction(accountId.toAccountId(), budgetId, processedState)
    }
  )

fun SubTransaction.toPublicSubTransaction(
  accountId: AccountId,
  budgetId: BudgetId,
  processedState: ProcessedState = if (deleted) ProcessedState.DELETED else ProcessedState.CREATED
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
  budgetId = budgetId
)
