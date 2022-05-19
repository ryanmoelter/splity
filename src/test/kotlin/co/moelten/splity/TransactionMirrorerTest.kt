package co.moelten.splity

import co.moelten.splity.database.AccountId
import co.moelten.splity.database.ProcessedState.CREATED
import co.moelten.splity.database.ProcessedState.UPDATED
import co.moelten.splity.database.ProcessedState.UP_TO_DATE
import co.moelten.splity.database.plus
import co.moelten.splity.database.replaceOnly
import co.moelten.splity.injection.createFakeSplityComponent
import co.moelten.splity.models.PublicTransactionDetail
import co.moelten.splity.test.Setup
import co.moelten.splity.test.addTransactions
import co.moelten.splity.test.getAllReplacedTransactions
import co.moelten.splity.test.getAllTransactions
import co.moelten.splity.test.isComplementOf
import co.moelten.splity.test.shouldContainSingleComplementOf
import co.moelten.splity.test.shouldHaveAllTransactionsProcessed
import co.moelten.splity.test.shouldHaveNoReplacedTransactions
import co.moelten.splity.test.shouldNotContainComplementOf
import co.moelten.splity.test.syncServerKnowledge
import co.moelten.splity.test.toApiTransaction
import co.moelten.splity.test.toPublicTransactionDetail
import co.moelten.splity.test.toPublicTransactionDetailList
import com.ryanmoelter.ynab.SyncData
import com.ryanmoelter.ynab.database.Database
import com.youneedabudget.client.models.TransactionDetail
import com.youneedabudget.client.models.TransactionDetail.FlagColorEnum.RED
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

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
    simpleCreatedTransactionsShouldMirrorProperly(
      transactionMirrorer,
      serverDatabase,
      localDatabase,
      setUpServerDatabase,
      setUpLocalDatabase
    )
  }

  context("on later run (with filled SyncData + filled local database)") {
    setUpLocalDatabase {
      syncDataQueries.replaceOnly(
        SyncData(
          NO_SERVER_KNOWLEDGE,
          FROM_BUDGET_ID,
          FROM_ACCOUNT_ID,
          NO_SERVER_KNOWLEDGE,
          TO_BUDGET_ID,
          TO_ACCOUNT_ID,
          shouldMatchTransactions = false
        )
      )
      addTransactions(
        existingMirroredTransaction(UP_TO_DATE),
        existingMirroredTransactionComplement(UP_TO_DATE),
        existingMirroredTransactionSourceParent(UP_TO_DATE),
        unremarkableTransactionInTransferSource(UP_TO_DATE)
      )
    }
    setUpServerDatabase {
      addTransactions(
        existingMirroredTransaction(UP_TO_DATE),
        existingMirroredTransactionComplement(UP_TO_DATE),
        existingMirroredTransactionSourceParent(UP_TO_DATE),
        unremarkableTransactionInTransferSource(UP_TO_DATE)
      )
      syncServerKnowledge(localDatabase)
    }

    simpleCreatedTransactionsShouldMirrorProperly(
      transactionMirrorer,
      serverDatabase,
      localDatabase,
      setUpServerDatabase,
      setUpLocalDatabase
    )

    context("with updated transactions in other accounts") {
      setUpServerDatabase {

      }
    }

    context("with an UPDATED transaction") {
      context("with no complement") {
        test("this transaction is flagged as an error") {
          TODO()
        }
      }

      context("with an UP_TO_DATE complement") {
        test("complement is updated") {
          TODO()
        }
      }

      context("with a CREATED complement") {
        test("complement is updated") {
          TODO()
        }
      }

      context("with an UPDATED complement") {
        test("both are flagged as errors") {
          TODO()
        }
      }

      context("with a DELETED complement") {
        test("this transaction is marked as deleted") {
          TODO()
        }
      }
    }

    context("with a CREATED transaction") {
      context("with no complement") {
        test("complement is created") {
          TODO()
        }
      }

      context("with an UP_TO_DATE complement") {
        test("nothing happens") {
          TODO()
        }
      }

      context("with a CREATED complement") {
        test("nothing happens") {
          TODO()
        }
      }

      context("with an UPDATED complement") {
        test("this transaction is updated") {
          TODO()
        }
      }

      context("with a DELETED complement") {
        test("this transaction is marked as deleted") {
          TODO()
        }
      }
    }

    context("with a DELETED transaction") {
      context("with no complement") {
        test("this transaction is flagged as an error") {
          TODO()
        }
      }

      context("with an UP_TO_DATE complement") {
        test("complement is marked as deleted") {
          TODO()
        }
      }

      context("with a CREATED complement") {
        test("complement is marked as deleted") {
          TODO()
        }
      }

      context("with an UPDATED complement") {
        test("complement is marked as deleted") {
          TODO()
        }
      }

      context("with a DELETED complement") {
        test("nothing happens") {
          TODO()
        }
      }
    }
  }
})

private suspend fun FunSpecContainerScope.simpleCreatedTransactionsShouldMirrorProperly(
  transactionMirrorer: TransactionMirrorer,
  serverDatabase: FakeYnabServerDatabase,
  localDatabase: Database,
  setUpServerDatabase: (FakeYnabServerDatabase.() -> Unit) -> Unit,
  setUpLocalDatabase: (Database.() -> Unit) -> Unit
) {
  context("with no new transactions") {
    test("mirrorTransactions does nothing") {
      transactionMirrorer.mirrorTransactions()

      localDatabase.shouldHaveAllTransactionsProcessed()
    }
  }

  context("created transactions with no complement") {
    context("with a manually added transaction") {
      setUpServerDatabase {
        addTransactionsForAccount(
          FROM_ACCOUNT_ID,
          listOf(manuallyAddedTransaction().toApiTransaction())
        )
      }

      mirrorTransactionMirrorsTransaction(
        transactionToMirror = manuallyAddedTransaction(),
        transactionMirrorer = transactionMirrorer,
        serverDatabase = serverDatabase,
        toAccountId = TO_ACCOUNT_ID
      ) {
        amount shouldBe -manuallyAddedTransaction().amount
        importId shouldBe "splity:-350000:2020-02-06:1"
        date shouldBe manuallyAddedTransaction().date
        payeeName shouldBe manuallyAddedTransaction().payeeName
        memo shouldBe manuallyAddedTransaction().memo + " • Out of $350.00, you paid 100.0%"
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
          listOf(transactionTransferNonSplitSource().toApiTransaction())
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
        memo shouldBe transactionTransferNonSplitSource().memo + " • Out of $10.00, you paid 100.0%"
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
          listOf(transactionTransferSplitSource().toApiTransaction())
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
        payeeName shouldBe transactionTransferSplitSource().payeeName
        memo shouldBe transactionTransferSplitSource().memo + " • Out of $30.00, you paid 33.3%"
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
            transactionTransferSplitSource().copy(
              subTransactions = listOf(
                subTransactionNonTransferSplitSource(),
                subTransactionTransferSplitSource().copy(
                  transferTransactionId = subTransactionTransferSplitSource()
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
        payeeName shouldBe transactionTransferSplitSource().payeeName
        memo shouldBe transactionTransferSplitSource().memo + " • Out of $30.00, you paid 33.3%"
        cleared shouldBe TransactionDetail.ClearedEnum.CLEARED
        approved.shouldBeFalse()
        deleted.shouldBeFalse()
        accountId shouldBe TO_ACCOUNT_ID.plainUuid
      }
    }
  }

  context("created transactions to ignore") {
    context("with an unapproved transaction") {
      val unapprovedTransaction = manuallyAddedTransaction().copy(approved = false)
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
          listOf(manuallyAddedTransaction().toApiTransaction())
        )
        addTransactionsForAccount(
          TO_ACCOUNT_ID,
          listOf(manuallyAddedTransactionComplement().toApiTransaction())
        )
      }

      test("mirrorTransactions doesn't duplicate the transaction") {
        transactionMirrorer.mirrorTransactions()

        serverDatabase.getTransactionsForAccount(TO_ACCOUNT_ID)
          .toPublicTransactionDetailList(TO_BUDGET_ID, UP_TO_DATE)
          .shouldContainSingleComplementOf(manuallyAddedTransaction())
        serverDatabase.getTransactionsForAccount(FROM_ACCOUNT_ID)
          .toPublicTransactionDetailList(FROM_BUDGET_ID, UP_TO_DATE)
          .shouldContainSingleComplementOf(manuallyAddedTransactionComplement())
      }
    }

    context("with a red-flagged transaction") {
      val redFlaggedTransaction = manuallyAddedTransaction().copy(flagColor = RED)
      setUpServerDatabase {
        addTransactionsForAccount(
          FROM_ACCOUNT_ID,
          listOf(redFlaggedTransaction.toApiTransaction())
        )
      }

      test("mirrorTransactions doesn't duplicate the transaction") {
        transactionMirrorer.mirrorTransactions()

        serverDatabase.getTransactionsForAccount(FROM_ACCOUNT_ID)
          .shouldContain(redFlaggedTransaction.toApiTransaction())
        serverDatabase.getTransactionsForAccount(TO_ACCOUNT_ID)
          .toPublicTransactionDetailList(TO_BUDGET_ID, UP_TO_DATE)
          .shouldNotContainComplementOf(redFlaggedTransaction)
        localDatabase.shouldHaveNoReplacedTransactions()
        localDatabase.getAllTransactions()
          .shouldContain(redFlaggedTransaction.copy(processedState = CREATED))
        localDatabase.getAllTransactions()
          .shouldNotContainComplementOf(redFlaggedTransaction)
      }

      context("with a red-flagged update") {
        val oldManualTransaction = manuallyAddedTransaction(UP_TO_DATE)
          .copy(approved = false)

        setUpLocalDatabase {
          addTransactions(oldManualTransaction)
        }

        test("mirrorTransactions doesn't duplicate the transaction") {
          transactionMirrorer.mirrorTransactions()

          serverDatabase.getTransactionsForAccount(FROM_ACCOUNT_ID)
            .shouldContain(redFlaggedTransaction.toApiTransaction())
          serverDatabase.getTransactionsForAccount(TO_ACCOUNT_ID)
            .toPublicTransactionDetailList(TO_BUDGET_ID, UP_TO_DATE)
            .shouldNotContainComplementOf(redFlaggedTransaction)
          localDatabase.getAllReplacedTransactions().shouldContainExactly(oldManualTransaction)
          localDatabase.getAllTransactions()
            .shouldContain(redFlaggedTransaction.copy(processedState = UPDATED))
          localDatabase.getAllTransactions()
            .shouldNotContainComplementOf(redFlaggedTransaction)
        }
      }
    }
  }
}

private suspend fun FunSpecContainerScope.mirrorTransactionsIgnoresTransaction(
  transactionToIgnore: PublicTransactionDetail,
  transactionMirrorer: TransactionMirrorer,
  serverDatabase: FakeYnabServerDatabase
) {
  test("mirrorTransactions ignores the transaction") {
    transactionMirrorer.mirrorTransactions()

    serverDatabase.getTransactionsForAccount(TO_ACCOUNT_ID)
      .toPublicTransactionDetailList(TO_BUDGET_ID, UP_TO_DATE)
      .shouldNotContainComplementOf(transactionToIgnore)
    serverDatabase.getTransactionsForAccount(FROM_ACCOUNT_ID)
      .shouldContain(transactionToIgnore.toApiTransaction())
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
      serverDatabase.getTransactionsForAccount(toAccountId)

    transactionsInToAccount.toPublicTransactionDetailList(TO_BUDGET_ID, UP_TO_DATE)
      .shouldContainSingleComplementOf(transactionToMirror)

    val complement = transactionsInToAccount.find { transaction ->
      transaction.toPublicTransactionDetail(TO_BUDGET_ID) isComplementOf transactionToMirror
    }!!

    assertSoftly(complement, assertSoftly)
  }
}
