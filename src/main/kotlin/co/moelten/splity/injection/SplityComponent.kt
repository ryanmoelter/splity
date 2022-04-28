package co.moelten.splity.injection

import co.moelten.splity.Config
import co.moelten.splity.DateDecoder
import co.moelten.splity.database.accountIdAdapter
import co.moelten.splity.database.budgetIdAdapter
import co.moelten.splity.database.categoryIdAdapter
import co.moelten.splity.database.localDateAdapter
import co.moelten.splity.database.payeeIdAdapter
import co.moelten.splity.database.subTransactionIdAdapter
import co.moelten.splity.database.transactionIdAdapter
import com.ryanmoelter.ynab.StoredAccount
import com.ryanmoelter.ynab.StoredBudget
import com.ryanmoelter.ynab.StoredSubTransaction
import com.ryanmoelter.ynab.StoredTransaction
import com.ryanmoelter.ynab.SyncData
import com.ryanmoelter.ynab.database.Database
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import java.io.File

@Component
@Singleton
interface SplityComponent {
  val config: Config

  @Provides
  @Singleton
  fun configLoader(): ConfigLoader = ConfigLoader.Builder()
    .addDecoder(DateDecoder())
    .addSource(PropertySource.resource("/version.properties"))
    .build()

  @Provides
  @Singleton
  fun config(configLoader: ConfigLoader): Config = configLoader.loadConfigOrThrow(File("./config.yaml"))

  @Provides
  fun driver(): SqlDriver = JdbcSqliteDriver("jdbc:sqlite:incrementalSyncCache.db")

  @Provides
  @Singleton
  fun database(driver: SqlDriver) = Database(
    driver,
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
    )
  )
}
