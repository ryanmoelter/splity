package co.moelten.splity.test

import co.moelten.splity.FakeYnabServerDatabase
import co.moelten.splity.models.PublicTransactionDetail
import com.ryanmoelter.ynab.database.Database
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot

fun Database.shouldHaveAllTransactionsProcessed() {
  withClue("Database should have transactions processed") {
    storedTransactionQueries.getUnprocessedExcept(emptyList()).executeAsList().shouldBeEmpty()
    storedSubTransactionQueries.getUnprocessedExcept(emptyList()).executeAsList().shouldBeEmpty()
    shouldHaveNoReplacedTransactions()
  }
}

fun Database.shouldHaveNoReplacedTransactions() {
  withClue("Database should have no replaced transactions left") {
    replacedTransactionQueries.getAll().executeAsList().shouldBeEmpty()
    replacedSubTransactionQueries.getAll().executeAsList().shouldBeEmpty()
  }
}

fun Database.shouldMatchServer(serverDatabase: FakeYnabServerDatabase) {
  withClue("Local database should match server") {
    val serverTransactionsByAccount = serverDatabase.getAllTransactionsByAccount()
    val localTransactionsByAccount = getAllTransactions()
      .groupBy { transaction -> transaction.accountId }
      .mapValues { (_, list) -> list.map { it.toApiTransaction() } }
    assertSoftly {
      localTransactionsByAccount.keys shouldContainExactly serverTransactionsByAccount.keys
      localTransactionsByAccount.forEach { (accountId, transactionList) ->
        withClue("For account $accountId") {
          transactionList.shouldContainExactlyInAnyOrder(
            serverTransactionsByAccount.getValue(accountId)
          )
        }
      }
    }
  }
}

infix fun List<PublicTransactionDetail>.shouldContainSingleComplementOf(
  transactionToMirror: PublicTransactionDetail
) {
  this should containComplementOf(transactionToMirror)
  this should containNoDuplicateComplementsOf(transactionToMirror)
}

infix fun List<PublicTransactionDetail>.shouldNotContainComplementOf(
  transactionToMirror: PublicTransactionDetail
) = this shouldNot containComplementOf(transactionToMirror)

fun containNoDuplicateComplementsOf(
  transactionToMirror: PublicTransactionDetail
) = Matcher<List<PublicTransactionDetail>> { transactionList ->
  MatcherResult(
    passed = transactionList
      .filter { transaction -> transaction isComplementOf transactionToMirror }
      .size <= 1,
    failureMessageFn = {
      "expected to find one or no transactions with date ${transactionToMirror.date} and amount " +
        "${-transactionToMirror.amount} but found 2+ in $transactionList"
    },
    negatedFailureMessageFn = {
      "list should contain 2+ complements with date ${transactionToMirror.date} and amount " +
        "${-transactionToMirror.amount} but it did not: $transactionList"
    }
  )
}

fun containComplementOf(
  transactionToMirror: PublicTransactionDetail
) = Matcher<List<PublicTransactionDetail>> { transactionList ->
  MatcherResult(
    passed = transactionList.find { transaction ->
      transaction isComplementOf transactionToMirror
    } != null,
    failureMessageFn = {
      "expected to find a transaction with date ${transactionToMirror.date} and amount " +
        "${-transactionToMirror.amount} but found nothing in $transactionList"
    },
    negatedFailureMessageFn = {
      "list should not contain a complement with date ${transactionToMirror.date} and amount " +
        "${-transactionToMirror.amount} but it did: $transactionList"
    }
  )
}

infix fun PublicTransactionDetail.isComplementOf(
  transactionToMirror: PublicTransactionDetail
) = date == transactionToMirror.date &&
  amount == -transactionToMirror.amount
