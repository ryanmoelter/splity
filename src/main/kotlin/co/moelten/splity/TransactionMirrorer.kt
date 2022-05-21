package co.moelten.splity

import co.moelten.splity.database.Repository
import me.tatarka.inject.annotations.Inject

@Inject
class TransactionMirrorer(
  private val repository: Repository,
  private val actionApplier: ActionApplier,
  private val actionCreator: ActionCreator,
  private val sentry: SentryWrapper
) {
  suspend fun mirrorTransactions() {
    sentry.doInSpan("Fetch new transactions") {
      repository.fetchNewTransactions()
    }

    val actions = sentry.doInSpan("Create actions") {
      actionCreator.createDifferentialActionsForBothAccounts()
    }
    sentry.doInSpan("Apply actions") {
      actionApplier.applyActions(actions)
    }
  }
}
