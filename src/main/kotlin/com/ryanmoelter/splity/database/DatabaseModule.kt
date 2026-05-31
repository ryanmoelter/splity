package com.ryanmoelter.splity.database

import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ryanmoelter.ynab.ReplacedSubTransaction
import com.ryanmoelter.ynab.ReplacedTransaction
import com.ryanmoelter.ynab.StoredSubTransaction
import com.ryanmoelter.ynab.StoredTransaction
import com.ryanmoelter.ynab.SyncData
import com.ryanmoelter.ynab.database.Database
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class DatabaseSingleton

@DatabaseSingleton
interface DatabaseModule {
  @Provides
  @DatabaseSingleton
  fun sqlDriver(): SqlDriver

  @Provides
  @DatabaseSingleton
  fun database(sqlDriver: SqlDriver): Database {
    try {
      Database.Schema.create(sqlDriver)
    } catch (error: Throwable) {
      // This throws an error if the database is already created, but it's safe to ignore
      println(error.message)
    }

    return Database(
      sqlDriver,
      storedTransactionAdapter =
        StoredTransaction.Adapter(
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
          processedStateAdapter = EnumColumnAdapter(),
        ),
      storedSubTransactionAdapter =
        StoredSubTransaction.Adapter(
          idAdapter = subTransactionIdAdapter,
          transactionIdAdapter = transactionIdAdapter,
          payeeIdAdapter = payeeIdAdapter,
          categoryIdAdapter = categoryIdAdapter,
          transferAccountIdAdapter = accountIdAdapter,
          transferTransactionIdAdapter = transactionIdAdapter,
          accountIdAdapter = accountIdAdapter,
          budgetIdAdapter = budgetIdAdapter,
          processedStateAdapter = EnumColumnAdapter(),
        ),
      syncDataAdapter =
        SyncData.Adapter(
          firstBudgetIdAdapter = budgetIdAdapter,
          firstAccountIdAdapter = accountIdAdapter,
          firstAccountPayeeIdAdapter = payeeIdAdapter,
          secondBudgetIdAdapter = budgetIdAdapter,
          secondAccountIdAdapter = accountIdAdapter,
          secondAccountPayeeIdAdapter = payeeIdAdapter,
        ),
      replacedTransactionAdapter =
        ReplacedTransaction.Adapter(
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
        ),
      replacedSubTransactionAdapter =
        ReplacedSubTransaction.Adapter(
          idAdapter = subTransactionIdAdapter,
          transactionIdAdapter = transactionIdAdapter,
          payeeIdAdapter = payeeIdAdapter,
          categoryIdAdapter = categoryIdAdapter,
          transferAccountIdAdapter = accountIdAdapter,
          transferTransactionIdAdapter = transactionIdAdapter,
          accountIdAdapter = accountIdAdapter,
          budgetIdAdapter = budgetIdAdapter,
        ),
    )
  }
}

@Component
abstract class RealDatabaseModule : DatabaseModule {
  override fun sqlDriver(): SqlDriver = JdbcSqliteDriver("jdbc:sqlite:incrementalSyncCache.db")
}
