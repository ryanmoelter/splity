package com.ryanmoelter.splity

import com.ryanmoelter.splity.database.Repository
import me.tatarka.inject.annotations.Inject

@Inject
class TransactionMirrorer(
  private val repository: Repository,
  private val actionApplier: ActionApplier,
  private val actionCreator: ActionCreator,
  private val transactionSplitter: TransactionSplitter,
  private val sentry: SentryWrapper,
) {
  suspend fun mirrorTransactions() {
    sentry.doInSpan("Fetch new transactions") {
      repository.fetchNewTransactions()
    }

    sentry.doInSpan("Split flagged transactions") {
      transactionSplitter.splitFlaggedTransactions()
    }

    val actions =
      sentry.doInSpan("Create actions") {
        actionCreator.createDifferentialActionsForBothAccounts()
      }
    sentry.doInSpan("Apply actions") {
      actionApplier.applyActions(actions)
    }
  }
}
