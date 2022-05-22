package co.moelten.splity

import co.moelten.splity.database.ProcessedState.CREATED
import co.moelten.splity.database.ProcessedState.UP_TO_DATE
import co.moelten.splity.database.plus
import co.moelten.splity.database.replaceOnly
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

class ActionCreatorTest : FunSpec({
  context("with normal config") {
    val serverDatabase = FakeYnabServerDatabase()
    val component = createFakeSplityComponent(serverDatabase)
    val localDatabase = component.database
    val actionCreater = component.actionCreator

    val setUpLocalDatabase: Setup<Database> = { setUp -> localDatabase.also(setUp) }

    setUpLocalDatabase {
      syncDataQueries.replaceOnly(
        SyncData(
          firstServerKnowledge = 0,
          firstBudgetId = FROM_BUDGET_ID,
          firstAccountId = FROM_ACCOUNT_ID,
          secondServerKnowledge = 0,
          secondBudgetId = TO_BUDGET_ID,
          secondAccountId = TO_ACCOUNT_ID,
          shouldMatchTransactions = true
        )
      )
    }

    test("add") {
      setUpLocalDatabase {
        addTransactions(manuallyAddedTransaction(CREATED))
      }
      val actions = actionCreater.createDifferentialActionsForBothAccounts()

      actions.shouldContainExactly(
        TransactionAction.CreateComplement(
          fromTransaction = manuallyAddedTransaction(CREATED),
          toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
        )
      )
    }

    test("ignore unapproved") {
      val unapprovedTransaction = manuallyAddedTransaction().copy(approved = false)
      setUpLocalDatabase { addTransactions(unapprovedTransaction) }
      val actions = actionCreater.createDifferentialActionsForBothAccounts()

      actions.shouldBeEmpty()
    }

    test("ignore already added") {
      setUpLocalDatabase {
        addTransactions(
          manuallyAddedTransaction(CREATED),
          manuallyAddedTransactionComplement(CREATED)
        )
      }
      val actions = actionCreater.createDifferentialActionsForBothAccounts()

      actions.shouldContainExactly(
        TransactionAction.MarkProcessed(
          manuallyAddedTransaction(CREATED),
          manuallyAddedTransactionComplement(CREATED)
        )
      )
    }

    test("ignore complement") {
      setUpLocalDatabase {
        addTransactions(
          manuallyAddedTransaction(CREATED),
          manuallyAddedTransactionComplement(CREATED)
        )
      }
      val actions = actionCreater.createDifferentialActionsForBothAccounts()

      actions.shouldContainExactly(
        TransactionAction.MarkProcessed(
          manuallyAddedTransaction(CREATED),
          manuallyAddedTransactionComplement(CREATED)
        )
      )
    }

    test("ignore complement with recurring split") {
      val transactionAddedFromTransferWithLongId = transactionAddedFromTransfer(isFromSplitSource = true).copy(
        id = transactionAddedFromTransfer(isFromSplitSource = true).id + "_st_1_2020-06-20"
      )
      val transactionAddedFromTransferWithLongIdComplement =
        transactionAddedFromTransferWithLongId.copy(
          id = "085a3584-2cc0-4570-b0f2-087eb61c3944".toTransactionId(),
          amount = -transactionAddedFromTransferWithLongId.amount,
          importId = subTransactionTransferSplitSource().id.toString(),
          cleared = TransactionDetail.ClearedEnum.CLEARED,
          approved = false,
          accountId = TO_ACCOUNT_ID,
          budgetId = TO_BUDGET_ID
        )
      setUpLocalDatabase {
        addTransactions(
          transactionAddedFromTransferWithLongId,
          transactionAddedFromTransferWithLongIdComplement,
          transactionTransferSplitSource(UP_TO_DATE).copy(
            subTransactions = listOf(
              subTransactionNonTransferSplitSource(UP_TO_DATE),
              subTransactionTransferSplitSource(UP_TO_DATE).copy(
                transferTransactionId = subTransactionTransferSplitSource(UP_TO_DATE)
                  .transferTransactionId!! + "_st_1_2020-06-20"
              )
            )
          )
        )
      }
      val actions = actionCreater.createDifferentialActionsForBothAccounts()

      actions.shouldContainExactly(
        TransactionAction.MarkProcessed(
          transactionAddedFromTransferWithLongId,
          transactionAddedFromTransferWithLongIdComplement
        )
      )
    }

    test("ignore already added with no importId") {
      val manuallyAddedTransactionComplementWithoutImportId =
        manuallyAddedTransactionComplement(UP_TO_DATE).copy(
          importId = null,
          memo = "I'm a different memo"
        )
      setUpLocalDatabase {
        addTransactions(
          manuallyAddedTransaction(),
          manuallyAddedTransactionComplementWithoutImportId
        )
      }
      val actions = actionCreater.createDifferentialActionsForBothAccounts()

      actions.shouldContainExactly(
        TransactionAction.MarkProcessed(
          manuallyAddedTransaction(CREATED),
          manuallyAddedTransactionComplementWithoutImportId
        )
      )
    }
  }

  context("with later startDate") {
    val serverDatabase = FakeYnabServerDatabase()
    val component = createFakeSplityComponent(
      serverDatabase,
      fakeConfig.copy(startDate = manuallyAddedTransaction().date.plusDays(1))
    )
    val localDatabase = component.database
    val actionCreater = component.actionCreator

    val setUpLocalDatabase: Setup<Database> = { setUp -> localDatabase.also(setUp) }

    setUpLocalDatabase {
      syncDataQueries.replaceOnly(
        SyncData(
          firstServerKnowledge = 0,
          firstBudgetId = FROM_BUDGET_ID,
          firstAccountId = FROM_ACCOUNT_ID,
          secondServerKnowledge = 0,
          secondBudgetId = TO_BUDGET_ID,
          secondAccountId = TO_ACCOUNT_ID,
          shouldMatchTransactions = true
        )
      )
    }

    test("ignore before start date") {
      setUpLocalDatabase { addTransactions(manuallyAddedTransaction()) }
      val actions = actionCreater.createDifferentialActionsForBothAccounts()

      actions.shouldBeEmpty()
    }
  }
})
