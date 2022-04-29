package co.moelten.splity.database

import co.moelten.splity.injection.Singleton
import com.ryanmoelter.ynab.ReplacedSubTransaction
import com.ryanmoelter.ynab.ReplacedTransaction
import com.ryanmoelter.ynab.StoredAccount
import com.ryanmoelter.ynab.StoredBudget
import com.ryanmoelter.ynab.StoredSubTransaction
import com.ryanmoelter.ynab.StoredTransaction
import com.ryanmoelter.ynab.SyncData
import com.ryanmoelter.ynab.database.Database
import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Singleton
interface DatabaseModule {
  @Provides
  @Singleton
  fun sqlDriver(): SqlDriver

  @Provides
  @Singleton
  fun database(sqlDriver: SqlDriver): Database {

    Database.Schema.create(sqlDriver)

    return Database(
      sqlDriver,
      storedAccountAdapter = StoredAccount.Adapter(
        idAdapter = accountIdAdapter,
        typeAdapter = EnumColumnAdapter(),
        transferPayeeIdAdapter = payeeIdAdapter,
        budgetIdAdapter = budgetIdAdapter
      ),
      storedBudgetAdapter = StoredBudget.Adapter(
        idAdapter = budgetIdAdapter,
        lastModifiedOnAdapter = localDateAdapter
      ),
      storedTransactionAdapter = StoredTransaction.Adapter(
        idAdapter = transactionIdAdapter,
        dateAdapter = localDateAdapter,
        clearedAdapter = EnumColumnAdapter(),
        accountIdAdapter = accountIdAdapter,
        flagColorAdapter = EnumColumnAdapter(),
        payeeIdAdapter = payeeIdAdapter,
        categoryIdAdapter = categoryIdAdapter,
        transferAccountIdAdapter = accountIdAdapter,
        transferTransactionIdAdapter = transactionIdAdapter,
        matchedTransactionIdAdapter = transactionIdAdapter,
        budgetIdAdapter = budgetIdAdapter,
        processedStateAdapter = EnumColumnAdapter()
      ),
      storedSubTransactionAdapter = StoredSubTransaction.Adapter(
        idAdapter = subTransactionIdAdapter,
        transactionIdAdapter = transactionIdAdapter,
        payeeIdAdapter = payeeIdAdapter,
        categoryIdAdapter = categoryIdAdapter,
        transferAccountIdAdapter = accountIdAdapter,
        transferTransactionIdAdapter = transactionIdAdapter,
        accountIdAdapter = accountIdAdapter,
        budgetIdAdapter = budgetIdAdapter,
        processedStateAdapter = EnumColumnAdapter()
      ),
      syncDataAdapter = SyncData.Adapter(
        firstBudgetIdAdapter = budgetIdAdapter,
        firstAccountIdAdapter = accountIdAdapter,
        secondBudgetIdAdapter = budgetIdAdapter,
        secondAccountIdAdapter = accountIdAdapter
      ),
      replacedTransactionAdapter = ReplacedTransaction.Adapter(
        idAdapter = transactionIdAdapter,
        dateAdapter = localDateAdapter,
        clearedAdapter = EnumColumnAdapter(),
        accountIdAdapter = accountIdAdapter,
        flagColorAdapter = EnumColumnAdapter(),
        payeeIdAdapter = payeeIdAdapter,
        categoryIdAdapter = categoryIdAdapter,
        transferAccountIdAdapter = accountIdAdapter,
        transferTransactionIdAdapter = transactionIdAdapter,
        matchedTransactionIdAdapter = transactionIdAdapter,
        budgetIdAdapter = budgetIdAdapter
      ),
      replacedSubTransactionAdapter = ReplacedSubTransaction.Adapter(
        idAdapter = subTransactionIdAdapter,
        transactionIdAdapter = transactionIdAdapter,
        payeeIdAdapter = payeeIdAdapter,
        categoryIdAdapter = categoryIdAdapter,
        transferAccountIdAdapter = accountIdAdapter,
        transferTransactionIdAdapter = transactionIdAdapter,
        accountIdAdapter = accountIdAdapter,
        budgetIdAdapter = budgetIdAdapter
      ),
    )
  }
}

@Component
abstract class RealDatabaseModule : DatabaseModule {
  override fun sqlDriver(): SqlDriver = JdbcSqliteDriver("jdbc:sqlite:incrementalSyncCache.db")
}
