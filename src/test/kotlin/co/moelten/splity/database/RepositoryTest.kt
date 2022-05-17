package co.moelten.splity.database

import co.moelten.splity.FROM_ACCOUNT
import co.moelten.splity.FROM_ACCOUNT_ID
import co.moelten.splity.FROM_BUDGET_ID
import co.moelten.splity.FROM_TRANSFER_SOURCE_ACCOUNT
import co.moelten.splity.FROM_TRANSFER_SOURCE_ACCOUNT_ID
import co.moelten.splity.FakeYnabServerDatabase
import co.moelten.splity.TO_ACCOUNT
import co.moelten.splity.TO_ACCOUNT_ID
import co.moelten.splity.TO_BUDGET_ID
import co.moelten.splity.database.ProcessedState.CREATED
import co.moelten.splity.database.ProcessedState.UPDATED
import co.moelten.splity.database.ProcessedState.UP_TO_DATE
import co.moelten.splity.fromBudget
import co.moelten.splity.injection.createFakeSplityComponent
import co.moelten.splity.manuallyAddedTransaction
import co.moelten.splity.manuallyAddedTransactionComplement
import co.moelten.splity.publicUnremarkableTransactionInTransferSource
import co.moelten.splity.test.Setup
import co.moelten.splity.test.addReplacedTransactions
import co.moelten.splity.test.addTransactions
import co.moelten.splity.test.shouldHaveAllTransactionsProcessed
import co.moelten.splity.test.toApiTransaction
import co.moelten.splity.toBudget
import co.moelten.splity.unremarkableTransactionInTransferSource
import com.ryanmoelter.ynab.SyncData
import com.ryanmoelter.ynab.database.Database
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe

class RepositoryTest : FunSpec({
  val serverDatabase = FakeYnabServerDatabase()
  val component = createFakeSplityComponent(serverDatabase)
  val localDatabase = component.database
  val repository = Repository(localDatabase, component.api, component.config)

  val setUpServerDatabase: Setup<FakeYnabServerDatabase> = { setUp -> serverDatabase.also(setUp) }

  val setUpLocalDatabase: Setup<Database> = { setUp -> localDatabase.also(setUp) }

  setUpServerDatabase {
    setUpBudgetsAndAccounts(
      fromBudget to listOf(FROM_ACCOUNT, FROM_TRANSFER_SOURCE_ACCOUNT),
      toBudget to listOf(TO_ACCOUNT)
    )
  }

  context("with filled SyncData") {
    setUpLocalDatabase {
      syncDataQueries.insert(
        SyncData(
          firstServerKnowledge = 0,
          firstBudgetId = FROM_BUDGET_ID,
          firstAccountId = FROM_ACCOUNT_ID,
          secondServerKnowledge = 0,
          secondBudgetId = TO_BUDGET_ID,
          secondAccountId = TO_ACCOUNT_ID,
          shouldMatchTransactions = false
        )
      )
    }

    fetchTransactionsPullsDataProperly(
      setUpServerDatabase,
      setUpLocalDatabase,
      localDatabase,
      repository
    )
  }

  context("with no SyncData") {
    fetchTransactionsPullsDataProperly(
      setUpServerDatabase,
      setUpLocalDatabase,
      localDatabase,
      repository
    )
  }
})

private suspend fun FunSpecContainerScope.fetchTransactionsPullsDataProperly(
  setUpServerDatabase: Setup<FakeYnabServerDatabase>,
  setUpLocalDatabase: Setup<Database>,
  localDatabase: Database,
  repository: Repository
) = context("fetchNewTransactions pulls data properly") {
  context("with empty server") {
    test("fetchNewTransactions does nothing") {
      repository.fetchNewTransactions()
      repository.getTransactionsByAccount(FROM_ACCOUNT_ID).shouldBeEmpty()
    }
  }

  context("with one unremarkable transaction on the server") {
    setUpServerDatabase {
      addTransactionsForAccount(
        FROM_TRANSFER_SOURCE_ACCOUNT_ID,
        listOf(unremarkableTransactionInTransferSource)
      )
    }

    test("fetchNewTransactions finds a new transaction") {
      repository.fetchNewTransactions()
      repository.getTransactionsByAccount(FROM_TRANSFER_SOURCE_ACCOUNT_ID) shouldHaveSingleElement
        publicUnremarkableTransactionInTransferSource(UP_TO_DATE)

      localDatabase.shouldHaveAllTransactionsProcessed()
    }

    context("when that transaction is an update") {
      setUpLocalDatabase {
        storedTransactionQueries.replaceSingle(
          unremarkableTransactionInTransferSource.toStoredTransaction(
            FROM_BUDGET_ID,
            UP_TO_DATE
          )
        )
      }

      test("fetchNewTransactions recognizes an updated transaction") {
        repository.fetchNewTransactions()
        repository.getTransactionsByAccount(FROM_TRANSFER_SOURCE_ACCOUNT_ID)
          .shouldHaveSingleElement(
            publicUnremarkableTransactionInTransferSource(UP_TO_DATE)
          )

        localDatabase.shouldHaveAllTransactionsProcessed()
      }
    }
  }

  context("with manually added complement transactions") {
    setUpServerDatabase {
      addTransactionsForAccount(
        FROM_ACCOUNT_ID,
        listOf(manuallyAddedTransaction().toApiTransaction())
      )
      addTransactionsForAccount(
        TO_ACCOUNT_ID,
        listOf(manuallyAddedTransactionComplement().toApiTransaction())
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
      setUpLocalDatabase {
        addTransactions(
          manuallyAddedTransaction(UP_TO_DATE),
          manuallyAddedTransactionComplement(UP_TO_DATE)
        )
      }

      test("fetchNewTransactions recognizes updated transactions") {
        repository.fetchNewTransactions()
        repository.getTransactionsByAccount(FROM_ACCOUNT_ID)
          .shouldHaveSingleElement(manuallyAddedTransaction(UPDATED))
        repository.getReplacedTransactionById(manuallyAddedTransaction().id) shouldBe
          manuallyAddedTransaction(UP_TO_DATE)
        repository.getTransactionsByAccount(TO_ACCOUNT_ID)
          .shouldHaveSingleElement(manuallyAddedTransactionComplement(UPDATED))
        repository.getReplacedTransactionById(manuallyAddedTransactionComplement().id) shouldBe
          manuallyAddedTransactionComplement(UP_TO_DATE)
      }
    }

    context("when those transactions are updates to unprocessed transactions") {
      setUpLocalDatabase {
        addTransactions(
          manuallyAddedTransaction(UPDATED),
          manuallyAddedTransactionComplement(CREATED)
        )
        addReplacedTransactions(manuallyAddedTransaction())
      }

      test("fetchNewTransactions recognizes updated transactions") {
        repository.fetchNewTransactions()

        repository.getTransactionsByAccount(FROM_ACCOUNT_ID)
          .shouldHaveSingleElement(manuallyAddedTransaction(UPDATED))
        repository.getReplacedTransactionById(manuallyAddedTransaction().id) shouldBe
          manuallyAddedTransaction(UP_TO_DATE)

        repository.getTransactionsByAccount(TO_ACCOUNT_ID)
          .shouldHaveSingleElement(manuallyAddedTransactionComplement(CREATED))
        shouldThrowAny {
          repository.getReplacedTransactionById(manuallyAddedTransactionComplement().id)
        }
      }
    }
  }
}
