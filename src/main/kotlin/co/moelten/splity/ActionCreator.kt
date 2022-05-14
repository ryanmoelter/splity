package co.moelten.splity

import co.moelten.splity.UpdateField.AMOUNT
import co.moelten.splity.UpdateField.CLEAR
import co.moelten.splity.UpdateField.DATE
import co.moelten.splity.UpdateField.values
import co.moelten.splity.database.ProcessedState.CREATED
import co.moelten.splity.database.ProcessedState.DELETED
import co.moelten.splity.database.ProcessedState.UPDATED
import co.moelten.splity.database.ProcessedState.UP_TO_DATE
import co.moelten.splity.database.Repository
import co.moelten.splity.database.firstAccountAndBudget
import co.moelten.splity.database.secondAccountAndBudget
import co.moelten.splity.models.PublicTransactionDetail
import com.youneedabudget.client.models.TransactionDetail
import me.tatarka.inject.annotations.Inject

@Inject
class ActionCreator(
  val repository: Repository,
  val config: Config
) {

  fun createDifferentialActionsForBothAccounts(): List<TransactionAction> {
    val syncData = repository.getSyncData()!!
    val firstAccountAndBudget = syncData.firstAccountAndBudget
    val secondAccountAndBudget = syncData.secondAccountAndBudget
    val startDate = config.startDate

    val unprocessedFirstTransactions = repository
      .getUnprocessedTransactionsByAccount(firstAccountAndBudget)
      .filter { it.date.isAfter(startDate.minusDays(1)) }
    var unprocessedSecondTransactions = repository
      .getUnprocessedTransactionsByAccount(secondAccountAndBudget)
      .filter { it.date.isAfter(startDate.minusDays(1)) }

    val complementPairs =
      emptyList<Pair<PublicTransactionDetail, PublicTransactionDetail?>>().toMutableList()

    unprocessedFirstTransactions
      .forEach { transactionDetail ->
        // Match on the replaced transaction, since that's what was matched last time
        val transactionToMatch = when (transactionDetail.processedState) {
          CREATED, DELETED -> transactionDetail
          UPDATED -> repository.getReplacedTransactionById(transactionDetail.id)
          UP_TO_DATE -> error("Unprocessed transaction is ${transactionDetail.processedState}")
        }
        // Match with replaced transaction, if it exists, since that's what was matched last time
        val replacedMatch = repository.findReplacedComplementOf(
          transactionToMatch,
          secondAccountAndBudget.accountId
        )
        val complement = if (replacedMatch == null) {
          // Complement is not UPDATED or DELETED, so it's either CREATED or UP_TO_DATE
          unprocessedSecondTransactions
            .find {
              it.date == transactionToMatch.date &&
                it.amount == -transactionToMatch.amount &&
                it.processedState != DELETED
            }
            ?.also { foundComplement ->
              unprocessedSecondTransactions = unprocessedSecondTransactions - foundComplement
            }
            ?: repository.findComplementOf(transactionToMatch, secondAccountAndBudget.accountId)
        } else {
          // Match using the replaced transaction, since that's what was matched last time
          unprocessedSecondTransactions
            .find { it.id == replacedMatch.id }
            ?: error("Cannot find updated transaction for replaced: $replacedMatch")
        }

        complementPairs += transactionDetail to complement
      }

    unprocessedSecondTransactions
      .forEach { transactionDetail ->
        // Complement isn't in the unprocesed transactions, otherwise it would have been matched
        // by one of the unprocessedFirstTransactions. It's also not UPDATED, for the same reason.
        val complement =
          repository.findComplementOf(transactionDetail, firstAccountAndBudget.accountId)

        complementPairs += transactionDetail to complement
      }

    return complementPairs
      .filter { (transaction, complement) ->
        val complementNeedsProcessing = when (complement?.processedState) {
          UP_TO_DATE, null -> false
          CREATED, UPDATED, DELETED -> true
        }
        when {
          transaction.flagColor == TransactionDetail.FlagColorEnum.RED ||
            complement?.flagColor == TransactionDetail.FlagColorEnum.RED -> false
          !transaction.approved && !complementNeedsProcessing -> false
          else -> true
        }
      }
      .mapNotNull { (fromTransaction, toTransaction) ->
        createAction(
          fromTransaction = fromTransaction,
          complement = toTransaction,
          toAccountAndBudget = if (fromTransaction.budgetId == firstAccountAndBudget.budgetId) {
            firstAccountAndBudget
          } else {
            secondAccountAndBudget
          }
        )
      }
  }

  private fun createAction(
    fromTransaction: PublicTransactionDetail,
    complement: PublicTransactionDetail?,
    toAccountAndBudget: AccountAndBudget
  ) = when (val state = fromTransaction.processedState) {
    CREATED -> when (complement?.processedState) {
      UP_TO_DATE, CREATED -> null  // Already exists
      UPDATED -> TransactionAction.UpdateComplement(
        fromTransaction = complement,
        complement = fromTransaction,
        updateFields = complement.compareWithReplaced()
      )
      DELETED -> TransactionAction.DeleteComplement(
        fromTransaction = complement,
        complement = fromTransaction
      )
      null -> TransactionAction.CreateComplement(
        fromTransaction = fromTransaction,
        toAccountAndBudget = toAccountAndBudget
      )
    }
    UPDATED -> when (complement?.processedState) {
      UP_TO_DATE -> TransactionAction.UpdateComplement(
        fromTransaction = fromTransaction,
        complement = complement,
        updateFields = fromTransaction.compareWithReplaced(),
      )
      CREATED -> TODO()
      UPDATED -> TODO()
      DELETED -> TODO()
      null -> TODO("Error")
    }
    DELETED -> when (complement?.processedState) {
      UP_TO_DATE, CREATED, UPDATED -> TransactionAction.DeleteComplement(
        fromTransaction = fromTransaction,
        complement = complement
      )
      DELETED -> null  // Already deleted
      null -> TODO("Error")
    }
    UP_TO_DATE -> error("State should never be $state")
  }

  private fun PublicTransactionDetail.compareWithReplaced(): Set<UpdateField> {
    val replaced = repository.getReplacedTransactionById(id)!!
    assert(subTransactions.isEmpty()) { "Cannot update a transaction that has sub-transactions" }

    // Loop over values to make sure we don't forget any
    return values()
      .filter { updateField ->
        when (updateField) {
          CLEAR -> approved && !replaced.approved // Newly approved -> clear complement
          AMOUNT -> amount != replaced.amount
          DATE -> date != replaced.date
        }
      }
      .toSet()
  }
}
