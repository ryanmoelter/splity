package co.moelten.splity

import co.moelten.splity.database.Repository
import me.tatarka.inject.annotations.Inject

@Inject
class TransactionMirrorer(
  private val repository: Repository,
  private val actionApplier: ActionApplier,
  private val actionCreator: ActionCreator
) {
  suspend fun mirrorTransactions() {
    repository.fetchNewTransactions()

    actionApplier.applyActions(actionCreator.createDifferentialActionsForBothAccounts())
  }
}
