package co.moelten.splity

import co.moelten.splity.database.toAccountId
import com.youneedabudget.client.MAX_IMPORT_ID_LENGTH
import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.TransactionDetail
import java.util.UUID

data class OtherAccountTransactionsCache(
  val ynab: YnabClient,
  private var otherAccountTransactions: MutableMap<UUID, List<TransactionDetail>> = mutableMapOf()
) {

  suspend fun getOtherAccountTransactions(accountAndBudget: AccountAndBudget): List<TransactionDetail> =
    otherAccountTransactions[accountAndBudget.accountId.id]
      ?: ynab.transactions.getTransactionsByAccount(
        accountAndBudget.budgetId.toString(),
        accountAndBudget.accountId.toString(),
        null,
        null,
        null
      )
        .data
        .transactions
        .also { transactions -> otherAccountTransactions[accountAndBudget.accountId.id] = transactions }

  suspend fun getAssociatedImportId(
    fromTransaction: TransactionDetail,
    fromAccountAndBudget: AccountAndBudget
  ): String {
    return if (fromTransaction.id.length <= MAX_IMPORT_ID_LENGTH) {
      fromTransaction.id
    } else {
      val otherAccountTransactions = getOtherAccountTransactions(
        AccountAndBudget(
          fromTransaction.transferAccountId!!.toAccountId(),
          fromAccountAndBudget.budgetId
        )
      )

      val parentOfSplitTransaction = otherAccountTransactions
        .find { transactionDetail ->
          transactionDetail.subtransactions.any { it.transferTransactionId == fromTransaction.id }
        }

      val id = parentOfSplitTransaction
        ?.subtransactions
        ?.find { it.transferTransactionId == fromTransaction.id }
        ?.id
        ?: throw IllegalStateException("Found a recurring transfer without a matching split transaction:\n${fromTransaction}")

      require(id.length <= MAX_IMPORT_ID_LENGTH) { "Cannot find an id that's within the $MAX_IMPORT_ID_LENGTH-character limit. Found $id" }
      id
    }
  }
}
