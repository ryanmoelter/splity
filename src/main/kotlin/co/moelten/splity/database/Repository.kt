package co.moelten.splity.database

import co.moelten.splity.Config
import com.ryanmoelter.ynab.StoredSubTransaction
import com.ryanmoelter.ynab.StoredTransaction
import com.ryanmoelter.ynab.SyncData
import com.ryanmoelter.ynab.database.Database
import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.SubTransaction
import com.youneedabudget.client.models.TransactionDetail
import me.tatarka.inject.annotations.Inject

@Inject
class Repository(
  val database: Database,
  val api: YnabClient,
  val config: Config
) {
  fun getTransactionsByAccount(accountId: AccountId): List<TransactionDetail> {
    val storedTransactions = database.storedTransactionQueries.getByAccount(accountId).executeAsList()
    val storedSubTransactions = database.storedSubTransactionQueries.getByAccount(accountId).executeAsList()
    val subTransactionMap = buildMap<TransactionId, List<StoredSubTransaction>> {
      storedSubTransactions.forEach { storedSubTransaction ->
        put(
          storedSubTransaction.transactionId,
          (get(storedSubTransaction.transactionId) ?: emptyList()) + storedSubTransaction
        )
      }
    }

    return storedTransactions.toApiTransactions(subTransactionMap)
  }

  /** Fetch new [TransactionDetail]s for processing, but don't store in database right now. */
  suspend fun fetchNewTransactions(): Pair<List<TransactionDetail>, SyncData> {
    val syncData = database.syncDataQueries.getOnly().executeAsOneOrNull()

    return if (syncData != null) {
      val firstResponse = api.transactions.getTransactions(
        syncData.firstBudgetId.toString(),
        sinceDate = config.startDate,
        type = null,
        lastKnowledgeOfServer = syncData.firstServerKnowledge
      )
      val secondResponse = api.transactions.getTransactions(
        syncData.firstBudgetId.toString(),
        sinceDate = config.startDate,
        type = null,
        lastKnowledgeOfServer = syncData.secondServerKnowledge
      )

      // TODO: attach budgetId to transactions, perhaps in wrapper object
      firstResponse.data.transactions + secondResponse.data.transactions to
        syncData.copy(
          firstServerKnowledge = firstResponse.data.serverKnowledge,
          secondServerKnowledge = secondResponse.data.serverKnowledge
        )
    } else {
      TODO("Find budget + account, save to SyncData, and get transactions")
    }
  }
}

fun List<StoredTransaction>.toApiTransactions(subTransactions: Map<TransactionId, List<StoredSubTransaction>>) =
  map { storedTransaction ->
    with(storedTransaction) {
      TransactionDetail(
        id = id.toString(),
        date = date,
        amount = amount,
        cleared = cleared,
        approved = approved,
        accountId = accountId.id,
        deleted = deleted,
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
      deleted = deleted,
      memo = memo,
      payeeId = payeeId?.id,
      payeeName = payeeName,
      categoryId = categoryId?.id,
      categoryName = categoryName,
      transferAccountId = transferAccountId?.id
    )
  }
}
