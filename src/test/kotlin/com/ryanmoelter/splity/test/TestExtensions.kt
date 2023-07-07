package com.ryanmoelter.splity.test

import com.ryanmoelter.splity.FakeYnabServerDatabase
import com.ryanmoelter.splity.database.ProcessedState
import com.ryanmoelter.splity.database.TransactionId
import com.ryanmoelter.splity.database.replaceOnly
import com.ryanmoelter.splity.database.toPublicTransactionList
import com.ryanmoelter.splity.database.toStoredSubTransaction
import com.ryanmoelter.splity.database.toStoredTransaction
import com.ryanmoelter.splity.models.PublicSubTransaction
import com.ryanmoelter.splity.models.PublicTransactionDetail
import com.ryanmoelter.splity.models.toPublicTransactionDetail
import com.ryanmoelter.ynab.ReplacedSubTransaction
import com.ryanmoelter.ynab.ReplacedTransaction
import com.ryanmoelter.ynab.database.Database
import com.ynab.client.models.SubTransaction
import com.ynab.client.models.TransactionDetail

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

fun FakeYnabServerDatabase.syncServerKnowledge(setUpLocalDatabase: Setup<Database>) {
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
