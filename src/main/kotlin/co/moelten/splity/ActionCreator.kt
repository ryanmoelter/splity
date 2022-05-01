package co.moelten.splity

import co.moelten.splity.database.Repository
import co.moelten.splity.database.firstAccountAndBudget
import co.moelten.splity.database.secondAccountAndBudget
import me.tatarka.inject.annotations.Inject

@Inject
class ActionCreator(
  val repository: Repository,
  val config: Config
) {

  fun createDifferentialActionsForBothAccounts(): List<CompleteTransactionAction> {
    val syncData = repository.getSyncData()!!
    val firstAccountAndBudget = syncData.firstAccountAndBudget
    val secondAccountAndBudget = syncData.secondAccountAndBudget
    val startDate = config.startDate

    val firstTransactions =
      repository.getTransactionsByAccount(syncData.firstAccountId)
    val secondTransactions =
      repository.getTransactionsByAccount(syncData.secondAccountId)

    var filteredFirstTransactions = firstTransactions
      .filter { it.date.isAfter(startDate.minusDays(1)) }
    var filteredSecondTransactions = secondTransactions
      .filter { it.date.isAfter(startDate.minusDays(1)) }

    firstTransactions.forEach { transactionDetail ->
      val complement = secondTransactions
        .find { it.date == transactionDetail.date && it.amount == -transactionDetail.amount }

      if (complement != null) {
        filteredFirstTransactions = filteredFirstTransactions - transactionDetail
        filteredSecondTransactions = filteredSecondTransactions - complement
      }
    }

    return filteredFirstTransactions
      .filter { it.approved }
      .map { transactionDetail ->
        CompleteTransactionAction(
          transactionAction = TransactionAction.Create(transactionDetail),
          fromAccountAndBudget = firstAccountAndBudget,
          toAccountAndBudget = secondAccountAndBudget
        )
      } + filteredSecondTransactions
      .filter { it.approved }
      .map { transactionDetail ->
        CompleteTransactionAction(
          transactionAction = TransactionAction.Create(transactionDetail),
          fromAccountAndBudget = secondAccountAndBudget,
          toAccountAndBudget = firstAccountAndBudget
        )
      }
  }
}
