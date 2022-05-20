package co.moelten.splity.database

import co.moelten.splity.AccountAndBudget
import co.moelten.splity.AccountConfig
import co.moelten.splity.Config
import co.moelten.splity.database.ProcessedState.CREATED
import co.moelten.splity.database.ProcessedState.DELETED
import co.moelten.splity.database.ProcessedState.UPDATED
import co.moelten.splity.database.ProcessedState.UP_TO_DATE
import co.moelten.splity.models.PublicTransactionDetail
import co.moelten.splity.models.toPublicTransactionDetail
import com.ryanmoelter.ynab.ReplacedTransaction
import com.ryanmoelter.ynab.StoredSubTransaction
import com.ryanmoelter.ynab.StoredTransaction
import com.ryanmoelter.ynab.SyncData
import com.ryanmoelter.ynab.database.Database
import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.Account
import com.youneedabudget.client.models.BudgetSummary
import com.youneedabudget.client.models.BudgetSummaryResponseData
import com.youneedabudget.client.models.TransactionDetail
import kotlinx.coroutines.coroutineScope
import me.tatarka.inject.annotations.Inject

@Inject
class Repository(
  private val database: Database,
  private val api: YnabClient,
  private val config: Config
) {
  fun getUnprocessedTransactionsByAccount(
    accountAndBudget: AccountAndBudget
  ): List<PublicTransactionDetail> {
    val storedTransactions =
      database.storedTransactionQueries.getUnprocessedByAccount(accountAndBudget.accountId)
        .executeAsList()
    // We don't need to specifically get unprocessed SubTransactions, because the API will always
    // return an updated TransactionDetail with the updated SubTransactions
    val storedSubTransactions =
      database.storedSubTransactionQueries.getByAccount(accountAndBudget.accountId).executeAsList()

    return storedTransactions.toPublicTransactionList(storedSubTransactions)
  }

  /**
   * Search for the complement of [originalTransaction] in the replaced transactions of
   * [inAccountId], finding something only if the complement has been [UPDATED] since the last time
   * splity ran.
   */
  fun findReplacedComplementOf(
    originalTransaction: PublicTransactionDetail,
    inAccountId: AccountId
  ): PublicTransactionDetail? = database.replacedTransactionQueries
    .getByComplement(inAccountId, originalTransaction.date, -originalTransaction.amount)
    .executeAsOneOrNull()
    ?.toPublicTransactionDetail()

  /**
   * Search for the complement of [originalTransaction] in [inAccountId]. Searches replaced
   * transactions first, since that's the transaction that would have been matched.
   */
  fun findComplementOf(
    originalTransaction: PublicTransactionDetail,
    inAccountId: AccountId
  ): PublicTransactionDetail? = database.storedTransactionQueries
    .getByComplement(inAccountId, originalTransaction.date, -originalTransaction.amount)
    .executeAsOneOrNull()
    ?.toPublicTransactionDetail()

  fun getReplacedTransactionById(id: TransactionId): PublicTransactionDetail {
    return database.replacedTransactionQueries.getById(id)
      .executeAsOne()
      .toPublicTransactionDetail()
  }

  fun getTransactionByTransferId(
    transferId: TransactionId
  ): PublicTransactionDetail? {
    val storedTransaction = database.storedTransactionQueries
      .getByTransferId(transferId)
      .executeAsOneOrNull()
    return storedTransaction?.toPublicTransactionDetail(
      database.storedSubTransactionQueries.getByTransactionId(storedTransaction.id).executeAsList()
    )
  }

  fun getTransactionBySubTransactionTransferId(
    transferId: TransactionId
  ): PublicTransactionDetail? {
    val storedTransaction = database.storedTransactionQueries
      .getBySubTransactionTransferId(transferId)
      .executeAsOneOrNull()
    return storedTransaction?.toPublicTransactionDetail(
      database.storedSubTransactionQueries.getByTransactionId(storedTransaction.id).executeAsList()
    )
  }

  fun getTransactionsByAccount(accountId: AccountId): List<PublicTransactionDetail> {
    val storedTransactions =
      database.storedTransactionQueries.getByAccount(accountId).executeAsList()
    val storedSubTransactions =
      database.storedSubTransactionQueries.getByAccount(accountId).executeAsList()

    return storedTransactions.toPublicTransactionList(storedSubTransactions)
  }

  /**
   * Fetch new transactions for processing and store them in database.
   * Also, find the [BudgetId]s and split [AccountId]s if necessary.
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
        shouldMatchTransactions = true
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
      budgetId = syncData.firstBudgetId
    )
    processAndSaveTransactions(
      transactions = secondResponse.data.transactions,
      budgetId = syncData.secondBudgetId
    )

    markAllTransactionsProcessedExceptInAccounts(
      listOf(
        syncData.firstAccountId,
        syncData.secondAccountId
      )
    )

    replaceSyncData(
      syncData.copy(
        firstServerKnowledge = firstResponse.data.serverKnowledge,
        secondServerKnowledge = secondResponse.data.serverKnowledge
      )
    )
  }

  fun addOrUpdateTransaction(
    transactionDetail: TransactionDetail,
    budgetId: BudgetId,
    processedState: ProcessedState
  ) {
    val storedTransaction = transactionDetail.toStoredTransaction(budgetId, processedState)
    database.storedTransactionQueries.replaceSingle(storedTransaction)
  }

  /**
   * Take the new transactions and save them into the database. Mark each transaction with the
   * appropriate [ProcessedState]:
   *
   * - [ProcessedState.UPDATED] if there's an existing [StoredTransaction] with the same
   *   [StoredTransaction.id]
   * - [ProcessedState.DELETED] if the transaction is marked as [TransactionDetail.deleted]
   * - [ProcessedState.CREATED] otherwise
   *
   * Also, for [ProcessedState.UPDATED] and [ProcessedState.DELETED], add to ReplacedTransactions db
   */
  private fun processAndSaveTransactions(
    transactions: List<TransactionDetail>,
    budgetId: BudgetId
  ) {
    val (storedTransactions, storedSubTransactions) = transactions
      .toUnprocessedStoredTransactions(budgetId)

    // Compare against transactions in database, specifically by id
    database.storedTransactionQueries.transaction {
      storedTransactions
        .map { storedTransaction ->
          val existingTransaction =
            database.storedTransactionQueries.getById(storedTransaction.id).executeAsOneOrNull()
          if (existingTransaction != null) {
            when (existingTransaction.processedState) {
              UP_TO_DATE -> {
                database.replacedTransactionQueries
                  .insert(existingTransaction.toReplacedTransaction())

                when (val state = storedTransaction.processedState) {
                  CREATED -> storedTransaction.copy(processedState = UPDATED)
                  DELETED -> storedTransaction
                  UP_TO_DATE, UPDATED -> error("New transactions should never be $state")
                }
              }
              // Treat as CREATED if the old transaction hasn't been processed yet
              CREATED -> storedTransaction
              // Don't overwrite a pending update
              UPDATED -> storedTransaction.copy(processedState = UPDATED)
              DELETED -> error("Trying to update an unprocessed DELETED transaction")
            }
          } else {
            storedTransaction
          }
        }
        .forEach { storedTransaction ->
          database.storedTransactionQueries.replaceSingle(storedTransaction)
        }
    }

    database.storedSubTransactionQueries.transaction {
      storedSubTransactions
        .map { storedSubTransaction ->
          val existingTransaction =
            database.storedSubTransactionQueries.getById(storedSubTransaction.id)
              .executeAsOneOrNull()
          if (existingTransaction != null) {
            database.replacedSubTransactionQueries
              .insert(existingTransaction.toReplacedSubTransaction())

            when (val state = storedSubTransaction.processedState) {
              CREATED -> storedSubTransaction.copy(processedState = UPDATED)
              DELETED -> storedSubTransaction
              UP_TO_DATE, UPDATED -> error("New sub-transactions should never be $state")
            }
          } else {
            storedSubTransaction
          }
        }
        .forEach { storedSubTransaction ->
          database.storedSubTransactionQueries.replaceSingle(storedSubTransaction)
        }
    }
  }

  /**
   * Find the [BudgetId] and [AccountId] specified by the given [accountConfig] in the given
   * [budgetResponse]. Returned as an [AccountAndBudget] object for conciseness.
   */
  private fun findAccountAndBudget(
    budgetResponse: BudgetSummaryResponseData,
    accountConfig: AccountConfig
  ): AccountAndBudget {
    val budget = budgetResponse.budgets.findByName(accountConfig.budgetName)
    val splitAccountId = budget.accounts!!.findByName(accountConfig.accountName).id.toAccountId()
    return AccountAndBudget(splitAccountId, budget.id.toBudgetId())
  }

  fun markProcessed(transaction: PublicTransactionDetail) {
    when (transaction.processedState) {
      UP_TO_DATE -> {
        // Do nothing
      }
      CREATED -> {
        database.storedTransactionQueries
          .replaceSingle(transaction.toStoredTransaction().copy(processedState = UP_TO_DATE))
        transaction.subTransactions.forEach { subTransaction ->
          database.storedSubTransactionQueries
            .replaceSingle(subTransaction.toStoredSubTransaction().copy(processedState = UP_TO_DATE))
        }
      }
      UPDATED -> {
        database.storedTransactionQueries
          .replaceSingle(transaction.toStoredTransaction().copy(processedState = UP_TO_DATE))
        transaction.subTransactions.forEach { subTransaction ->
          database.storedSubTransactionQueries
            .replaceSingle(subTransaction.toStoredSubTransaction().copy(processedState = UP_TO_DATE))
        }
        database.replacedTransactionQueries.deleteById(transaction.id)
        database.replacedSubTransactionQueries.deleteByTransactionId(transaction.id)
      }
      DELETED -> {
        database.storedTransactionQueries.deleteById(transaction.id)
        database.storedSubTransactionQueries.deleteByTransactionId(transaction.id)
        database.replacedTransactionQueries.deleteById(transaction.id)
        database.replacedSubTransactionQueries.deleteByTransactionId(transaction.id)
      }
    }
  }

  fun markAllTransactionsProcessedExceptInAccounts(
    accountIds: List<AccountId>
  ) {
    val storedTransactions = database.storedTransactionQueries
      .getUnprocessedExcept(accountsToExclude = accountIds)
      .executeAsList()
    // Unprocessed transactions should always have unprocessed subTransactions with them
    val storedSubTransactions = database.storedSubTransactionQueries
      .getUnprocessedExcept(accountsToExclude = accountIds)
      .executeAsList()

    val publicTransactions = storedTransactions.toPublicTransactionList(storedSubTransactions)

    publicTransactions.forEach { transaction ->
      markProcessed(transaction)
    }
  }

  // -- SyncDataRepository, could be a separate file -----------------------------------------------

  fun getSyncData(): SyncData? = database.syncDataQueries.getOnly().executeAsOneOrNull()

  private fun replaceSyncData(syncData: SyncData) {
    database.syncDataQueries.replaceOnly(syncData)
  }

  private fun StoredTransaction.toPublicTransactionDetail(): PublicTransactionDetail =
    toPublicTransactionDetail(
      database.storedSubTransactionQueries.getByTransactionId(this.id).executeAsList()
    )

  private fun ReplacedTransaction.toPublicTransactionDetail(): PublicTransactionDetail =
    toPublicTransactionDetail(
      database.replacedSubTransactionQueries.getByTransactionId(this.id).executeAsList()
    )
}

fun List<StoredTransaction>.toPublicTransactionList(
  storedSubTransactions: List<StoredSubTransaction>
): List<PublicTransactionDetail> {
  val subTransactionMap = buildMap<TransactionId, List<StoredSubTransaction>> {
    storedSubTransactions.forEach { storedSubTransaction ->
      put(
        storedSubTransaction.transactionId,
        (get(storedSubTransaction.transactionId) ?: emptyList()) + storedSubTransaction
      )
    }
  }

  return map { storedTransaction ->
    storedTransaction.toPublicTransactionDetail(
      subTransactionMap[storedTransaction.id] ?: emptyList()
    )
  }
}

fun List<BudgetSummary>.findByName(name: String) =
  find { it.name == name } ?: throw IllegalStateException("Can't find budget: \"$name\"")

fun List<Account>.findByName(name: String) =
  find { it.name == name } ?: throw IllegalStateException("Can't find account: \"$name\"")
