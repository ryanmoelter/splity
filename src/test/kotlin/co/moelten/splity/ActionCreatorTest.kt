package co.moelten.splity

import co.moelten.splity.database.ProcessedState
import co.moelten.splity.database.ProcessedState.UPDATED
import co.moelten.splity.database.ProcessedState.UP_TO_DATE
import co.moelten.splity.database.plus
import co.moelten.splity.database.toTransactionId
import co.moelten.splity.injection.createFakeSplityComponent
import co.moelten.splity.test.Setup
import co.moelten.splity.test.addTransactions
import com.ryanmoelter.ynab.SyncData
import com.ryanmoelter.ynab.database.Database
import com.youneedabudget.client.models.TransactionDetail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import java.util.UUID

class ActionCreatorTest : FunSpec({
  context("with normal config") {
    val serverDatabase = FakeYnabServerDatabase()
    val component = createFakeSplityComponent(serverDatabase)
    val localDatabase = component.database
    val actionCreater = component.actionCreator

    val setUpLocalDatabase: Setup<Database> = { setUp -> localDatabase.also(setUp) }

    setUpLocalDatabase {
      syncDataQueries.insert(
        SyncData(
          0,
          FROM_BUDGET_ID,
          FROM_ACCOUNT_ID,
          0,
          TO_BUDGET_ID,
          TO_ACCOUNT_ID,
          shouldMatchTransactions = true
        )
      )
    }

    test("add") {
      setUpLocalDatabase {
        addTransactions(manuallyAddedTransaction)
      }
      val actions = actionCreater.createDifferentialActionsForBothAccounts()

      actions.shouldContainExactly(
        TransactionAction.CreateComplement(
          fromTransaction = manuallyAddedTransaction,
          fromAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
          toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
        )
      )
    }

    test("ignore unapproved") {
      val unapprovedTransaction = manuallyAddedTransaction.copy(approved = false)
      setUpLocalDatabase { addTransactions(unapprovedTransaction) }
      val actions = actionCreater.createDifferentialActionsForBothAccounts()

      actions.shouldBeEmpty()
    }

    test("ignore already added") {
      setUpLocalDatabase {
        addTransactions(
          manuallyAddedTransaction.copy(processedState = ProcessedState.CREATED),
          manuallyAddedTransactionComplement.copy(processedState = ProcessedState.CREATED)
        )
      }
      val actions = actionCreater.createDifferentialActionsForBothAccounts()

      actions.shouldBeEmpty()
    }

    test("ignore complement") {
      setUpLocalDatabase {
        addTransactions(
          manuallyAddedTransaction.copy(processedState = ProcessedState.CREATED),
          manuallyAddedTransactionComplement.copy(processedState = ProcessedState.CREATED)
        )
      }
      val actions = actionCreater.createDifferentialActionsForBothAccounts()

      actions.shouldBeEmpty()
    }

    test("ignore complement with recurring split") {
      val transactionAddedFromTransferWithLongId = transactionAddedFromTransfer.copy(
        id = transactionAddedFromTransfer.id + "_st_1_2020-06-20"
      )
      val transactionAddedFromTransferWithLongIdComplement =
        transactionAddedFromTransferWithLongId.copy(
          id = UUID.randomUUID().toTransactionId(),
          amount = -transactionAddedFromTransferWithLongId.amount,
          importId = subTransactionTransferSplitSource.id.toString(),
          cleared = TransactionDetail.ClearedEnum.CLEARED,
          approved = false,
          accountId = TO_ACCOUNT_ID,
          budgetId = TO_BUDGET_ID
        )
      setUpLocalDatabase {
        addTransactions(
          transactionAddedFromTransferWithLongId,
          transactionAddedFromTransferWithLongIdComplement,
          transactionTransferSplitSource.copy(
            subTransactions = listOf(
              subTransactionNonTransferSplitSource,
              subTransactionTransferSplitSource.copy(
                transferTransactionId = subTransactionTransferSplitSource
                  .transferTransactionId!! + "_st_1_2020-06-20"
              )
            )
          )
        )
      }
      val actions = actionCreater.createDifferentialActionsForBothAccounts()

      actions.shouldBeEmpty()
    }

    test("ignore already added with no importId") {
      val manuallyAddedTransactionComplementWithoutImportId =
        manuallyAddedTransactionComplement.copy(
          importId = null,
          memo = "I'm a different memo"
        )
      setUpLocalDatabase {
        addTransactions(manuallyAddedTransaction, manuallyAddedTransactionComplementWithoutImportId)
      }
      val actions = actionCreater.createDifferentialActionsForBothAccounts()

      actions.shouldBeEmpty()
    }

    test("clear transaction on complement approved") {
      val manuallyAddedTransactionComplementApproved =
        manuallyAddedTransactionComplement.copy(approved = true, processedState = UPDATED)
      val existingManuallyAddedTransaction =
        manuallyAddedTransaction.copy(processedState = UP_TO_DATE)

      setUpLocalDatabase {
        addTransactions(manuallyAddedTransactionComplementApproved, existingManuallyAddedTransaction)
      }

      val actions = actionCreater.createDifferentialActionsForBothAccounts()

      actions.shouldContainExactly(
        TransactionAction.UpdateComplement(
          fromTransaction = manuallyAddedTransactionComplementApproved,
          complement = existingManuallyAddedTransaction,
          updateFields = setOf(UpdateField.CLEAR),
          fromAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
          toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
        )
      )
    }
  }

  context("with later startDate") {
    val serverDatabase = FakeYnabServerDatabase()
    val component = createFakeSplityComponent(
      serverDatabase,
      fakeConfig.copy(startDate = manuallyAddedTransaction.date.plusDays(1))
    )
    val localDatabase = component.database
    val actionCreater = component.actionCreator

    val setUpLocalDatabase: Setup<Database> = { setUp -> localDatabase.also(setUp) }

    setUpLocalDatabase {
      syncDataQueries.insert(
        SyncData(
          0,
          FROM_BUDGET_ID,
          FROM_ACCOUNT_ID,
          0,
          TO_BUDGET_ID,
          TO_ACCOUNT_ID,
          shouldMatchTransactions = true
        )
      )
    }

    test("ignore before start date") {
      setUpLocalDatabase { addTransactions(manuallyAddedTransaction) }
      val actions = actionCreater.createDifferentialActionsForBothAccounts()

      actions.shouldBeEmpty()
    }
  }
})
