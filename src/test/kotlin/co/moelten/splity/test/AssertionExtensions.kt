package co.moelten.splity.test

import com.ryanmoelter.ynab.database.Database
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty

fun Database.shouldHaveAllTransactionsProcessed() {
  withClue("Database should have transactions processed") {
    storedTransactionQueries.getUnprocessedExcept(emptyList()).executeAsList().shouldBeEmpty()
    storedSubTransactionQueries.getUnprocessedExcept(emptyList()).executeAsList().shouldBeEmpty()
    replacedTransactionQueries.getAll().executeAsList().shouldBeEmpty()
    replacedSubTransactionQueries.getAll().executeAsList().shouldBeEmpty()
  }
}
