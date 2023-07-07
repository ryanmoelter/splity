package com.ryanmoelter.splity

import com.ryanmoelter.splity.database.PayeeId
import com.ryanmoelter.splity.database.ProcessedState
import com.ryanmoelter.splity.database.Repository
import com.ryanmoelter.splity.models.PublicTransactionDetail
import com.ynab.client.YnabClient
import com.ynab.client.models.PutTransactionWrapper
import com.ynab.client.models.SaveSubTransaction
import com.ynab.client.models.SaveTransaction
import com.ynab.client.models.TransactionDetail.FlagColorEnum.PURPLE
import me.tatarka.inject.annotations.Inject

@Inject
class TransactionSplitter(
  private val repository: Repository,
  private val ynabClient: YnabClient
) {

  suspend fun splitFlaggedTransactions() {
    val syncData = repository.getSyncData()!!

    val unprocessedFlaggedTransactions =
      repository.getUnprocessedAndFlaggedTransactionsInAccountsExcept(
        listOf(syncData.firstAccountId, syncData.secondAccountId)
      )
        .filter { it.flagColor == PURPLE }
        .filterNot { transaction ->
          transaction.subTransactions
            .any { subTransaction ->
              subTransaction.processedState != ProcessedState.DELETED
            }
            .also { hasNonDeletedSubTransactions ->
              if (hasNonDeletedSubTransactions) {
                println(
                  "Not splitting purple-flagged transaction to avoid duplicating " +
                    "sub-transactions: $transaction"
                )
              }
            }
        }

    unprocessedFlaggedTransactions
      .forEach { transaction ->
        println("Splitting transaction: $transaction")
        transaction.splitIntoSplitAccount(
          splitAccountPayeeId = if (transaction.budgetId == syncData.firstBudgetId) {
            syncData.firstAccountPayeeId
          } else {
            syncData.secondAccountPayeeId
          }
        )
      }

    repository.markAllTransactionsProcessedExceptInAccounts(
      listOf(
        syncData.firstAccountId,
        syncData.secondAccountId
      )
    )

    if (unprocessedFlaggedTransactions.isNotEmpty()) {
      repository.fetchNewTransactions()
      unprocessedFlaggedTransactions
        .map { repository.getTransactionById(it.id)!! }
        .forEach {
          println("Marking transaction as processed: $it")
          repository.markProcessed(it)
        }
    }
  }

  private suspend fun PublicTransactionDetail.splitIntoSplitAccount(
    splitAccountPayeeId: PayeeId,
  ) {
    // No haypennies here, and payer gets the extra cent on transactions with odd cents
    val payerAmount = amount / 2 / 10 * 10
    val splitAmount = amount - payerAmount
    val subTransactions = listOf(
      SaveSubTransaction(
        amount = payerAmount,
        payeeId = null,
        payeeName = null,
        categoryId = categoryId?.plainUuid,
        memo = null
      ),
      SaveSubTransaction(
        amount = splitAmount,  // The receiver of the split pays the extra cent
        payeeId = splitAccountPayeeId.plainUuid,
        payeeName = null,
        categoryId = null,
        memo = null
      )
    )

    val response = ynabClient.transactions.updateTransaction(
      budgetId = budgetId.plainUuid.toString(),
      transactionId = id.string,
      data = PutTransactionWrapper(
        toSaveTransaction().copy(
          categoryId = null,
          flagColor = SaveTransaction.FlagColorEnum.GREEN,
          subtransactions = subTransactions
        )
      )
    )

    repository.addOrUpdateTransaction(
      response.data.transaction,
      budgetId,
      processedState
    )
  }
}
