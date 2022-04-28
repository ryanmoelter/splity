package co.moelten.splity.database

import co.moelten.splity.AccountAndBudget
import co.moelten.splity.AccountConfig
import co.moelten.splity.Config
import co.moelten.splity.findByName
import com.ryanmoelter.ynab.StoredSubTransaction
import com.ryanmoelter.ynab.StoredTransaction
import com.ryanmoelter.ynab.SyncData
import com.ryanmoelter.ynab.database.Database
import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.BudgetSummaryResponseData
import com.youneedabudget.client.models.SubTransaction
import com.youneedabudget.client.models.TransactionDetail
import kotlinx.coroutines.coroutineScope
import me.tatarka.inject.annotations.Inject

@Inject
class Repository(
  private val database: Database,
  private val api: YnabClient,
  private val config: Config
) {
  fun getTransactionsByAccount(accountId: AccountId): List<TransactionDetail> {
    val storedTransactions =
      database.storedTransactionQueries.getByAccount(accountId).executeAsList()
    val storedSubTransactions =
      database.storedSubTransactionQueries.getByAccount(accountId).executeAsList()
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

  /**
   * Fetch new [TransactionDetail]s for processing and store them in database.
   * Also, find the budget / split account IDs if necessary.
   */
  suspend fun fetchNewTransactions() {
    val syncData = getSyncData() ?: coroutineScope {
      val budgetResponse = api.budgets.getBudgets(includeAccounts = true).data
      val firstAccountAndBudget = findAccountAndBudget(budgetResponse, config.firstAccount)
      val secondAccountAndBudget = findAccountAndBudget(budgetResponse, config.secondAccount)

      SyncData(
        firstServerKnowledge = null,
        firstBudgetId = firstAccountAndBudget.budgetId,
        firstAccountId = firstAccountAndBudget.accountId,
        secondServerKnowledge = null,
        secondBudgetId = secondAccountAndBudget.budgetId,
        secondAccountId = secondAccountAndBudget.accountId,
      )
    }

    val firstResponse = api.transactions.getTransactions(
      syncData.firstBudgetId.toString(),
      sinceDate = config.startDate,
      type = null,
      lastKnowledgeOfServer = syncData.firstServerKnowledge
    )
    val secondResponse = api.transactions.getTransactions(
      syncData.secondBudgetId.toString(),
      sinceDate = config.startDate,
      type = null,
      lastKnowledgeOfServer = syncData.secondServerKnowledge
    )

    processAndSaveTransactions(
      transactions = firstResponse.data.transactions,
      budgetId = syncData.firstBudgetId,
      accountId = syncData.firstAccountId
    )

    replaceSyncData(
      syncData.copy(
        firstServerKnowledge = firstResponse.data.serverKnowledge,
        secondServerKnowledge = secondResponse.data.serverKnowledge
      )
    )
  }

  private fun processAndSaveTransactions(
    transactions: List<TransactionDetail>,
    budgetId: BudgetId,
    accountId: AccountId
  ) {
    var (firstTransactions, firstSubTransactions) = transactions
      .toUnprocessedStoredTransactions(budgetId)

    // Compare against transactions in database, specifically by id
    database.storedTransactionQueries.transaction {
     firstTransactions = firstTransactions.map { transaction ->
        val existingTransaction =
          database.storedTransactionQueries.getById(transaction.id).executeAsOneOrNull()
        if (existingTransaction != null) {
          TODO("Copy into other database")
          database.storedTransactionQueries.replaceSingle(transaction)

          transaction.copy(processedState = ProcessedState.UPDATED)
        } else {
          transaction
        }
      }
    }
  }

  private fun findAccountAndBudget(
    budgetResponse: BudgetSummaryResponseData,
    accountConfig: AccountConfig
  ): AccountAndBudget {
    val budget = budgetResponse.budgets.findByName(accountConfig.budgetName)
    val splitAccountId = budget.accounts!!.findByName(accountConfig.accountName).id.toAccountId()
    return AccountAndBudget(splitAccountId, budget.id.toBudgetId())
  }

  // -- SyncDataRepository, could be a separate file -----------------------------------------------

  private fun getSyncData(): SyncData? = database.syncDataQueries.getOnly().executeAsOneOrNull()

  private fun replaceSyncData(syncData: SyncData) {
    database.syncDataQueries.transaction {
      database.syncDataQueries.clear()
      database.syncDataQueries.insert(syncData)
    }
  }
}

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
