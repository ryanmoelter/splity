package co.moelten.splity

import co.moelten.splity.database.PayeeId
import co.moelten.splity.database.Repository
import co.moelten.splity.models.PublicTransactionDetail
import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.SaveSubTransaction
import com.youneedabudget.client.models.SaveTransactionWrapper
import com.youneedabudget.client.models.TransactionDetail.FlagColorEnum.PURPLE
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

    unprocessedFlaggedTransactions
      .forEach {
        it.splitIntoSplitAccount(
          splitAccountPayeeId = if (it.budgetId == syncData.firstBudgetId) {
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
      data = SaveTransactionWrapper(
        toSaveTransaction().copy(
          categoryId = null,
          flagColor = null,
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
