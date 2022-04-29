package co.moelten.splity.database

import co.moelten.splity.FROM_ACCOUNT_ID
import co.moelten.splity.FROM_BUDGET_ID
import co.moelten.splity.FakeYnabServerDatabase
import co.moelten.splity.TO_ACCOUNT_ID
import co.moelten.splity.TO_BUDGET_ID
import co.moelten.splity.injection.createFakeSplityComponent
import com.ryanmoelter.ynab.SyncData
import com.ryanmoelter.ynab.database.Database
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty

class RepositoryTest : FunSpec({
  val serverDatabase = FakeYnabServerDatabase()
  val component = createFakeSplityComponent(serverDatabase)
  val localDatabase = component.database
  val repository = Repository(localDatabase, component.api, component.config)

  val setUpServerDatabase = { setUp: FakeYnabServerDatabase.() -> Unit ->
    serverDatabase.apply(setUp)
  }

  val setUpLocalDatabase = { setUp: Database.() -> Unit ->
    localDatabase.apply(setUp)
  }

  context("with filled SyncData") {
    setUpLocalDatabase {
      syncDataQueries.insert(
        SyncData(
          0,
          FROM_BUDGET_ID,
          FROM_ACCOUNT_ID,
          0,
          TO_BUDGET_ID,
          TO_ACCOUNT_ID
        )
      )
    }

    context("with empty server") {
      setUpServerDatabase {
        budgetToAccountsMap = mapOf(FROM_BUDGET_ID to emptyList(), TO_BUDGET_ID to emptyList())
      }

      test("fetchNewTransactions does nothing") {
        repository.fetchNewTransactions()
        repository.getTransactionsByAccount(FROM_ACCOUNT_ID).shouldBeEmpty()
      }
    }
  }
})
