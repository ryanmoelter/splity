package co.moelten.splity

import co.moelten.splity.database.AccountId
import co.moelten.splity.database.plus
import co.moelten.splity.injection.createFakeSplityComponent
import co.moelten.splity.models.PublicTransactionDetail
import co.moelten.splity.test.Setup
import co.moelten.splity.test.addTransactions
import co.moelten.splity.test.toApiTransaction
import com.ryanmoelter.ynab.SyncData
import com.ryanmoelter.ynab.database.Database
import com.youneedabudget.client.models.TransactionDetail
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot

internal class TransactionMirrorerTest : FunSpec({
  val serverDatabase = FakeYnabServerDatabase()
  val component = createFakeSplityComponent(serverDatabase)
  val localDatabase = component.database
  val transactionMirrorer = component.transactionMirrorer

  val setUpLocalDatabase: Setup<Database> = { setUp -> localDatabase.also(setUp) }

  val setUpServerDatabase: Setup<FakeYnabServerDatabase> = { setUp -> serverDatabase.also(setUp) }

  setUpServerDatabase {
    setUpBudgetsAndAccounts(
      fromBudget to listOf(FROM_ACCOUNT, FROM_TRANSFER_SOURCE_ACCOUNT),
      toBudget to listOf(TO_ACCOUNT)
    )
  }

  context("on first run (unfilled SyncData + no local database)") {
    context("with no new transactions") {
      test("mirrorTransactions does nothing") {
        transactionMirrorer.mirrorTransactions()

        serverDatabase.shouldHaveNoTransactions()
      }
    }

    context("with a manually added transaction") {
      setUpServerDatabase {
        addTransactionsForAccount(
          FROM_ACCOUNT_ID,
          listOf(manuallyAddedTransaction.toApiTransaction())
        )
      }

      mirrorTransactionMirrorsTransaction(
        transactionToMirror = manuallyAddedTransaction,
        transactionMirrorer = transactionMirrorer,
        serverDatabase = serverDatabase,
        toAccountId = TO_ACCOUNT_ID
      ) {
        amount shouldBe -manuallyAddedTransaction.amount
        importId shouldBe "splity:-350000:2020-02-06:1"
        date shouldBe manuallyAddedTransaction.date
        payeeName shouldBe manuallyAddedTransaction.payeeName
        memo shouldBe manuallyAddedTransaction.memo + " • Out of $350.00, you paid 100.0%"
        cleared shouldBe TransactionDetail.ClearedEnum.CLEARED
        approved.shouldBeFalse()
        deleted.shouldBeFalse()
        accountId shouldBe TO_ACCOUNT_ID.plainUuid
      }
    }

    context("with a non-split transfer") {
      setUpServerDatabase {
        addTransactionsForAccount(
          FROM_ACCOUNT_ID,
          listOf(transactionAddedFromTransfer.toApiTransaction())
        )
        addTransactionsForAccount(
          FROM_TRANSFER_SOURCE_ACCOUNT_ID,
          listOf(transactionTransferNonSplitSource.toApiTransaction())
        )
      }

      mirrorTransactionMirrorsTransaction(
        transactionToMirror = transactionAddedFromTransfer,
        transactionMirrorer = transactionMirrorer,
        serverDatabase = serverDatabase,
        toAccountId = TO_ACCOUNT_ID
      ) {
        amount shouldBe -transactionAddedFromTransfer.amount
        importId shouldBe "splity:10000:2020-02-07:1"
        date shouldBe transactionAddedFromTransfer.date
        payeeName shouldBe "Chicken Butt"
        memo shouldBe transactionTransferNonSplitSource.memo + " • Out of $10.00, you paid 100.0%"
        cleared shouldBe TransactionDetail.ClearedEnum.CLEARED
        approved.shouldBeFalse()
        deleted.shouldBeFalse()
        accountId shouldBe TO_ACCOUNT_ID.plainUuid
      }
    }

    context("with a split transfer") {
      setUpServerDatabase {
        addTransactionsForAccount(
          FROM_ACCOUNT_ID,
          listOf(transactionAddedFromTransfer.toApiTransaction())
        )
        addTransactionsForAccount(
          FROM_TRANSFER_SOURCE_ACCOUNT_ID,
          listOf(transactionTransferSplitSource.toApiTransaction())
        )
      }

      mirrorTransactionMirrorsTransaction(
        transactionToMirror = transactionAddedFromTransfer,
        transactionMirrorer = transactionMirrorer,
        serverDatabase = serverDatabase,
        toAccountId = TO_ACCOUNT_ID
      ) {
        amount shouldBe -transactionAddedFromTransfer.amount
        importId shouldBe "splity:${-transactionAddedFromTransfer.amount}:${transactionAddedFromTransfer.date}:1"
        date shouldBe transactionAddedFromTransfer.date
        payeeName shouldBe transactionTransferSplitSource.payeeName
        memo shouldBe transactionTransferSplitSource.memo + " • Out of $30.00, you paid 33.3%"
        cleared shouldBe TransactionDetail.ClearedEnum.CLEARED
        approved.shouldBeFalse()
        deleted.shouldBeFalse()
        accountId shouldBe TO_ACCOUNT_ID.plainUuid
      }
    }

    context("with a recurring split transaction") {
      val transactionAddedFromTransferWithLongId = transactionAddedFromTransfer.copy(
        id = transactionAddedFromTransfer.id + "_st_1_2020-06-20"
      )

      setUpServerDatabase {
        addTransactionsForAccount(
          FROM_ACCOUNT_ID,
          listOf(transactionAddedFromTransferWithLongId.toApiTransaction())
        )
        addTransactionsForAccount(
          FROM_TRANSFER_SOURCE_ACCOUNT_ID,
          listOf(
            transactionTransferSplitSource.copy(
              subTransactions = listOf(
                subTransactionNonTransferSplitSource,
                subTransactionTransferSplitSource.copy(
                  transferTransactionId = subTransactionTransferSplitSource
                    .transferTransactionId!! + "_st_1_2020-06-20"
                )
              )
            ).toApiTransaction()
          )
        )
      }

      mirrorTransactionMirrorsTransaction(
        transactionToMirror = transactionAddedFromTransferWithLongId,
        transactionMirrorer = transactionMirrorer,
        serverDatabase = serverDatabase,
        toAccountId = TO_ACCOUNT_ID
      ) {
        amount shouldBe -transactionAddedFromTransferWithLongId.amount
        importId shouldBe "splity:${-transactionAddedFromTransferWithLongId.amount}:${transactionAddedFromTransferWithLongId.date}:1"
        date shouldBe transactionAddedFromTransferWithLongId.date
        payeeName shouldBe transactionTransferSplitSource.payeeName
        memo shouldBe transactionTransferSplitSource.memo + " • Out of $30.00, you paid 33.3%"
        cleared shouldBe TransactionDetail.ClearedEnum.CLEARED
        approved.shouldBeFalse()
        deleted.shouldBeFalse()
        accountId shouldBe TO_ACCOUNT_ID.plainUuid
      }
    }

    context("ignored transactions") {
      context("with an unapproved transaction") {
        val unapprovedTransaction = manuallyAddedTransaction.copy(approved = false)
        setUpServerDatabase {
          addTransactionsForAccount(
            FROM_ACCOUNT_ID,
            listOf(unapprovedTransaction.toApiTransaction())
          )
        }

        mirrorTransactionsIgnoresTransaction(
          unapprovedTransaction,
          transactionMirrorer,
          serverDatabase
        )
      }

      context("with an already-added transaction") {
        setUpServerDatabase {
          addTransactionsForAccount(
            FROM_ACCOUNT_ID,
            listOf(manuallyAddedTransaction.toApiTransaction())
          )
          addTransactionsForAccount(
            TO_ACCOUNT_ID,
            listOf(manuallyAddedTransactionComplement.toApiTransaction())
          )
        }

        test("mirrorTransactions doesn't duplicate the transaction") {
          transactionMirrorer.mirrorTransactions()

          serverDatabase.accountToTransactionsMap
            .getValue(TO_ACCOUNT_ID) shouldContainSingleComplementOf manuallyAddedTransaction
          serverDatabase.accountToTransactionsMap.getValue(FROM_ACCOUNT_ID) shouldContainExactly
            listOf(manuallyAddedTransaction.toApiTransaction())
        }
      }
    }
  }

  context("on later run (with filled SyncData + filled local database)") {
    setUpLocalDatabase {
      syncDataQueries.insert(
        SyncData(
          10,
          FROM_BUDGET_ID,
          FROM_ACCOUNT_ID,
          10,
          TO_BUDGET_ID,
          TO_ACCOUNT_ID,
          shouldMatchTransactions = false
        )
      )
      addTransactions()
    }
  }
})

private suspend fun FunSpecContainerScope.mirrorTransactionsIgnoresTransaction(
  transactionToIgnore: PublicTransactionDetail,
  transactionMirrorer: TransactionMirrorer,
  serverDatabase: FakeYnabServerDatabase
) {
  test("mirrorTransactions ignores the transaction") {
    transactionMirrorer.mirrorTransactions()

    serverDatabase.accountToTransactionsMap.getValue(TO_ACCOUNT_ID)
      .shouldNotContainComplementOf(transactionToIgnore)
    serverDatabase.accountToTransactionsMap
      .getValue(FROM_ACCOUNT_ID) shouldContainExactly listOf(transactionToIgnore.toApiTransaction())
  }
}

private suspend fun FunSpecContainerScope.mirrorTransactionMirrorsTransaction(
  transactionToMirror: PublicTransactionDetail,
  transactionMirrorer: TransactionMirrorer,
  serverDatabase: FakeYnabServerDatabase,
  toAccountId: AccountId,
  assertSoftly: TransactionDetail.(TransactionDetail) -> Unit
) {
  test("mirrorTransactions mirrors the transaction") {
    transactionMirrorer.mirrorTransactions()

    val transactionsInToAccount =
      serverDatabase.accountToTransactionsMap.getValue(toAccountId)

    transactionsInToAccount shouldContainSingleComplementOf transactionToMirror

    val complement = transactionsInToAccount.find { transaction ->
      transaction isComplementOf transactionToMirror
    }!!

    assertSoftly(complement, assertSoftly)
  }
}

infix fun List<TransactionDetail>.shouldContainSingleComplementOf(
  transactionToMirror: PublicTransactionDetail
) {
  this should containComplementOf(transactionToMirror)
  this should containNoDuplicateComplementsOf(transactionToMirror)
}

infix fun List<TransactionDetail>.shouldNotContainComplementOf(
  transactionToMirror: PublicTransactionDetail
) = this shouldNot containComplementOf(transactionToMirror)

fun containNoDuplicateComplementsOf(
  transactionToMirror: PublicTransactionDetail
) = Matcher<List<TransactionDetail>> { transactionList ->
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
) = Matcher<List<TransactionDetail>> { transactionList ->
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

infix fun TransactionDetail.isComplementOf(
  transactionToMirror: PublicTransactionDetail
) = date == transactionToMirror.date &&
  amount == -transactionToMirror.amount
