package co.moelten.splity

import co.moelten.splity.TransactionAction.CreateComplement
import co.moelten.splity.TransactionAction.DeleteComplement
import co.moelten.splity.TransactionAction.MarkError
import co.moelten.splity.TransactionAction.MarkError.ErrorMessages.BOTH_UPDATED
import co.moelten.splity.TransactionAction.MarkError.ErrorMessages.UPDATED_WITHOUT_COMPLEMENT
import co.moelten.splity.TransactionAction.MarkProcessed
import co.moelten.splity.TransactionAction.UpdateComplement
import co.moelten.splity.database.ProcessedState.CREATED
import co.moelten.splity.database.ProcessedState.DELETED
import co.moelten.splity.database.ProcessedState.UPDATED
import co.moelten.splity.database.ProcessedState.UP_TO_DATE
import co.moelten.splity.database.Repository
import co.moelten.splity.database.UpdateField
import co.moelten.splity.database.calculateUpdatedFieldsFrom
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
            ?.also { foundComplement ->
              unprocessedSecondTransactions = unprocessedSecondTransactions - foundComplement
            }
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
        val isDeleted = when (transaction.processedState) {
          UP_TO_DATE, CREATED, UPDATED -> false
          DELETED -> true
        }
        isDeleted || when {
          transaction.flagColor == TransactionDetail.FlagColorEnum.RED ||
            complement?.flagColor == TransactionDetail.FlagColorEnum.RED -> false
          !transaction.approved && !complementNeedsProcessing -> false
          else -> true
        }
      }
      .map { (fromTransaction, complement) ->
        createAction(
          fromTransaction = fromTransaction,
          complement = complement,
          toAccountAndBudget = if (fromTransaction.budgetId == firstAccountAndBudget.budgetId) {
            secondAccountAndBudget
          } else {
            firstAccountAndBudget
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
      UP_TO_DATE, CREATED -> MarkProcessed(fromTransaction, complement)
      UPDATED -> calculateUpdateAction(
        fromTransaction = complement,
        complement = fromTransaction
      )
      DELETED -> DeleteComplement(
        fromTransaction = complement,
        complement = fromTransaction
      )
      null -> CreateComplement(
        fromTransaction = fromTransaction,
        toAccountAndBudget = toAccountAndBudget
      )
    }
    UPDATED -> when (complement?.processedState) {
      UP_TO_DATE, CREATED -> {
        calculateUpdateAction(
          fromTransaction = fromTransaction,
          complement = complement
        )
      }
      UPDATED -> if (fromTransaction.getUpdatedFields(complement).isEmpty() && complement.getUpdatedFields(fromTransaction).isEmpty()) {
        MarkProcessed(
          fromTransaction = fromTransaction,
          complement = complement
        )
      } else {
        MarkError(
          fromTransaction = fromTransaction,
          complement = complement,
          message = BOTH_UPDATED
        )
      }
      DELETED -> DeleteComplement(
        fromTransaction = complement,
        complement = fromTransaction
      )
      // TODO: report soft error
      null -> MarkError(
        fromTransaction = fromTransaction,
        message = UPDATED_WITHOUT_COMPLEMENT
      )
    }
    DELETED -> when (complement?.processedState) {
      UP_TO_DATE, CREATED, UPDATED -> DeleteComplement(
        fromTransaction = fromTransaction,
        complement = complement
      )
      // Already deleted
      DELETED -> MarkProcessed(fromTransaction, complement)
      // Never existed? Can't throw an error, else we'd never be able to fix it
      // TODO: report soft error
      null -> MarkProcessed(fromTransaction)
    }
    UP_TO_DATE -> error("State should never be $state")
  }

  private fun calculateUpdateAction(
    fromTransaction: PublicTransactionDetail,
    complement: PublicTransactionDetail
  ): TransactionAction {
    val updatedFields = fromTransaction.getUpdatedFields(complement)
    return if (updatedFields.isEmpty()) {
      MarkProcessed(
        fromTransaction = fromTransaction,
        complement = complement
      )
    } else {
      UpdateComplement(
        fromTransaction = fromTransaction,
        complement = complement,
        updateFields = updatedFields,
      )
    }
  }

  private fun PublicTransactionDetail.getUpdatedFields(
    complement: PublicTransactionDetail? = null
  ): Set<UpdateField> {
    val replaced = repository.getReplacedTransactionById(id)
    return calculateUpdatedFieldsFrom(replaced, complement)
  }
}
