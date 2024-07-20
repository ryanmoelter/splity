package com.ryanmoelter.splity.database

import com.ryanmoelter.splity.FROM_ACCOUNT
import com.ryanmoelter.splity.FROM_ACCOUNT_ID
import com.ryanmoelter.splity.FROM_ACCOUNT_PAYEE_ID
import com.ryanmoelter.splity.FROM_BUDGET_ID
import com.ryanmoelter.splity.FROM_TRANSFER_SOURCE_ACCOUNT
import com.ryanmoelter.splity.FROM_TRANSFER_SOURCE_ACCOUNT_ID
import com.ryanmoelter.splity.FakeYnabServerDatabase
import com.ryanmoelter.splity.TO_ACCOUNT
import com.ryanmoelter.splity.TO_ACCOUNT_ID
import com.ryanmoelter.splity.TO_ACCOUNT_PAYEE_ID
import com.ryanmoelter.splity.TO_BUDGET_ID
import com.ryanmoelter.splity.database.ProcessedState.CREATED
import com.ryanmoelter.splity.database.ProcessedState.UPDATED
import com.ryanmoelter.splity.database.ProcessedState.UP_TO_DATE
import com.ryanmoelter.splity.fromBudget
import com.ryanmoelter.splity.injection.createFakeSplityComponent
import com.ryanmoelter.splity.manuallyAddedTransaction
import com.ryanmoelter.splity.manuallyAddedTransactionComplement
import com.ryanmoelter.splity.test.Setup
import com.ryanmoelter.splity.test.addReplacedTransactions
import com.ryanmoelter.splity.test.addTransactions
import com.ryanmoelter.splity.test.shouldHaveAllTransactionsProcessed
import com.ryanmoelter.splity.test.shouldHaveAllTransactionsProcessedExcept
import com.ryanmoelter.splity.test.shouldHaveNoReplacedTransactions
import com.ryanmoelter.splity.test.toApiTransaction
import com.ryanmoelter.splity.toBudget
import com.ryanmoelter.splity.unremarkableTransactionInTransferSource
import com.ryanmoelter.ynab.SyncData
import com.ryanmoelter.ynab.database.Database
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe

class RepositoryTest :
  FunSpec({
    val serverDatabase = FakeYnabServerDatabase()
    val component = createFakeSplityComponent(serverDatabase)
    val localDatabase = component.database
    val repository = Repository(localDatabase, component.api, component.config)

    val setUpServerDatabase: Setup<FakeYnabServerDatabase> = { setUp -> serverDatabase.also(setUp) }

    val setUpLocalDatabase: Setup<Database> = { setUp -> localDatabase.also(setUp) }

    setUpServerDatabase {
      setUpBudgetsAndAccounts(
        fromBudget to listOf(FROM_ACCOUNT, FROM_TRANSFER_SOURCE_ACCOUNT),
        toBudget to listOf(TO_ACCOUNT),
      )
    }

    context("with filled SyncData") {
      setUpLocalDatabase {
        syncDataQueries.replaceOnly(
          SyncData(
            firstServerKnowledge = 0,
            firstBudgetId = FROM_BUDGET_ID,
            firstAccountId = FROM_ACCOUNT_ID,
            firstAccountPayeeId = FROM_ACCOUNT_PAYEE_ID,
            secondServerKnowledge = 0,
            secondBudgetId = TO_BUDGET_ID,
            secondAccountId = TO_ACCOUNT_ID,
            secondAccountPayeeId = TO_ACCOUNT_PAYEE_ID,
          ),
        )
      }

      fetchTransactionsPullsDataProperly(
        setUpServerDatabase,
        setUpLocalDatabase,
        localDatabase,
        repository,
      )
    }

    context("with no SyncData") {
      fetchTransactionsPullsDataProperly(
        setUpServerDatabase,
        setUpLocalDatabase,
        localDatabase,
        repository,
      )
    }
  })

private suspend fun FunSpecContainerScope.fetchTransactionsPullsDataProperly(
  setUpServerDatabase: Setup<FakeYnabServerDatabase>,
  setUpLocalDatabase: Setup<Database>,
  localDatabase: Database,
  repository: Repository,
) = context("fetchNewTransactions pulls data properly") {
  context("with empty server") {
    test("fetchNewTransactions does nothing") {
      repository.fetchNewTransactions()
      repository.getTransactionsByAccount(FROM_ACCOUNT_ID).shouldBeEmpty()
    }
  }

  context("with one unremarkable transaction on the server") {
    setUpServerDatabase {
      addOrUpdateTransactionsForAccount(
        FROM_TRANSFER_SOURCE_ACCOUNT_ID,
        listOf(unremarkableTransactionInTransferSource().toApiTransaction()),
      )
    }

    test("fetchNewTransactions finds a new transaction") {
      repository.fetchNewTransactions()
      repository.getTransactionsByAccount(FROM_TRANSFER_SOURCE_ACCOUNT_ID).shouldContainExactly(
        unremarkableTransactionInTransferSource(CREATED),
      )

      localDatabase.shouldHaveAllTransactionsProcessedExcept(
        setOf(unremarkableTransactionInTransferSource().id),
      )
    }

    context("when that transaction is an update") {
      setUpLocalDatabase {
        storedTransactionQueries.replaceSingle(
          unremarkableTransactionInTransferSource(UP_TO_DATE).toStoredTransaction(),
        )
      }

      test("fetchNewTransactions recognizes an updated transaction") {
        repository.fetchNewTransactions()
        repository
          .getTransactionsByAccount(FROM_TRANSFER_SOURCE_ACCOUNT_ID)
          .shouldHaveSingleElement(
            unremarkableTransactionInTransferSource(UP_TO_DATE),
          )

        localDatabase.shouldHaveAllTransactionsProcessed()
      }
    }
  }

  context("with manually added complement transactions") {
    setUpServerDatabase {
      addOrUpdateTransactionsForAccount(
        FROM_ACCOUNT_ID,
        listOf(manuallyAddedTransaction().toApiTransaction()),
      )
      addOrUpdateTransactionsForAccount(
        TO_ACCOUNT_ID,
        listOf(manuallyAddedTransactionComplement().toApiTransaction()),
      )
    }

    test("fetchNewTransactions finds new transactions") {
      repository.fetchNewTransactions()
      repository.getTransactionsByAccount(FROM_ACCOUNT_ID) shouldHaveSingleElement
        manuallyAddedTransaction(CREATED)
      repository.getTransactionsByAccount(TO_ACCOUNT_ID) shouldHaveSingleElement
        manuallyAddedTransactionComplement(CREATED)
    }

    context("when those transactions are updates") {
      val replacedTransaction = manuallyAddedTransaction(UP_TO_DATE).copy(amount = 100_000)
      val replacedTransactionComplement =
        manuallyAddedTransactionComplement(UP_TO_DATE).copy(amount = 100_000)
      setUpLocalDatabase {
        addTransactions(
          replacedTransaction,
          replacedTransactionComplement,
        )
      }

      test("fetchNewTransactions recognizes updated transactions") {
        repository.fetchNewTransactions()
        repository
          .getTransactionsByAccount(FROM_ACCOUNT_ID)
          .shouldHaveSingleElement(manuallyAddedTransaction(UPDATED))
        repository.getReplacedTransactionById(manuallyAddedTransaction().id) shouldBe
          replacedTransaction
        repository
          .getTransactionsByAccount(TO_ACCOUNT_ID)
          .shouldHaveSingleElement(manuallyAddedTransactionComplement(UPDATED))
        repository.getReplacedTransactionById(manuallyAddedTransactionComplement().id) shouldBe
          replacedTransactionComplement
      }
    }

    context("when those transactions are updates to identical transactions") {
      setUpLocalDatabase {
        addTransactions(
          manuallyAddedTransaction(UP_TO_DATE),
          manuallyAddedTransactionComplement(UP_TO_DATE),
        )
      }

      test("fetchNewTransactions ignores the updated transactions") {
        repository.fetchNewTransactions()
        repository
          .getTransactionsByAccount(FROM_ACCOUNT_ID)
          .shouldHaveSingleElement(manuallyAddedTransaction(UP_TO_DATE))
        repository
          .getTransactionsByAccount(TO_ACCOUNT_ID)
          .shouldHaveSingleElement(manuallyAddedTransactionComplement(UP_TO_DATE))
        localDatabase.shouldHaveNoReplacedTransactions()
      }
    }

    context("when those transactions are updates to unprocessed transactions") {
      setUpLocalDatabase {
        addTransactions(
          manuallyAddedTransaction(UPDATED),
          manuallyAddedTransactionComplement(CREATED),
        )
        addReplacedTransactions(manuallyAddedTransaction())
      }

      test("fetchNewTransactions recognizes updated transactions") {
        repository.fetchNewTransactions()

        repository
          .getTransactionsByAccount(FROM_ACCOUNT_ID)
          .shouldHaveSingleElement(manuallyAddedTransaction(UPDATED))
        repository.getReplacedTransactionById(manuallyAddedTransaction().id) shouldBe
          manuallyAddedTransaction(UP_TO_DATE)

        repository
          .getTransactionsByAccount(TO_ACCOUNT_ID)
          .shouldHaveSingleElement(manuallyAddedTransactionComplement(CREATED))
        shouldThrowAny {
          repository.getReplacedTransactionById(manuallyAddedTransactionComplement().id)
        }
      }
    }
  }
}
