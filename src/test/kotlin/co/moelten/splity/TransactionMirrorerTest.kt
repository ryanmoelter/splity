package co.moelten.splity

import co.moelten.splity.database.ProcessedState.CREATED
import co.moelten.splity.database.ProcessedState.DELETED
import co.moelten.splity.database.ProcessedState.UPDATED
import co.moelten.splity.database.ProcessedState.UP_TO_DATE
import co.moelten.splity.database.plus
import co.moelten.splity.database.replaceOnly
import co.moelten.splity.database.toPublicTransactionDetail
import co.moelten.splity.database.toPublicTransactionDetailList
import co.moelten.splity.injection.createFakeSplityComponent
import co.moelten.splity.models.PublicTransactionDetail
import co.moelten.splity.test.Setup
import co.moelten.splity.test.addTransactions
import co.moelten.splity.test.getAllReplacedTransactions
import co.moelten.splity.test.getAllTransactions
import co.moelten.splity.test.isComplementOf
import co.moelten.splity.test.shouldContainSingleComplementOf
import co.moelten.splity.test.shouldHaveAllTransactionsProcessed
import co.moelten.splity.test.shouldHaveAllTransactionsProcessedExcept
import co.moelten.splity.test.shouldHaveNoReplacedTransactions
import co.moelten.splity.test.shouldMatchServer
import co.moelten.splity.test.shouldNotContainComplementOf
import co.moelten.splity.test.syncServerKnowledge
import co.moelten.splity.test.toApiTransaction
import com.ryanmoelter.ynab.SyncData
import com.ryanmoelter.ynab.database.Database
import com.youneedabudget.client.models.TransactionDetail
import com.youneedabudget.client.models.TransactionDetail.ClearedEnum.CLEARED
import com.youneedabudget.client.models.TransactionDetail.ClearedEnum.UNCLEARED
import com.youneedabudget.client.models.TransactionDetail.FlagColorEnum.BLUE
import com.youneedabudget.client.models.TransactionDetail.FlagColorEnum.PURPLE
import com.youneedabudget.client.models.TransactionDetail.FlagColorEnum.RED
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.core.test.AssertionMode
import io.kotest.inspectors.shouldForAny
import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.threeten.bp.LocalDate

// This file is a bit long, cmd+shift+- (Collapse all) is your friend

internal class TransactionMirrorerTest : FunSpec({
  assertions = AssertionMode.Error

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
          FROM_ACCOUNT_PAYEE_ID,
          NO_SERVER_KNOWLEDGE,
          TO_BUDGET_ID,
          TO_ACCOUNT_ID,
          TO_ACCOUNT_PAYEE_ID
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
        existingMirroredTransaction(),
        existingMirroredTransactionComplement(),
        existingMirroredTransactionSourceParent(),
        unremarkableTransactionInTransferSource()
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
      val updatedUnremarkableTransaction = unremarkableTransactionInTransferSource(UPDATED)
        .copy(amount = -150_000)
      setUpServerDatabase {
        addTransactions(anotherUnremarkableTransactionInTransferSource())
        addTransactions(updatedUnremarkableTransaction)
      }
      val beginningServerKnowledge = serverDatabase.currentServerKnowledge

      test("nothing happens") {
        transactionMirrorer.mirrorTransactions()

        withClue("Server knowledge should not change") {
          serverDatabase.currentServerKnowledge shouldBe beginningServerKnowledge
        }
        localDatabase.shouldHaveAllTransactionsProcessed()
        localDatabase.shouldMatchServer(serverDatabase)
      }
    }

    context("with a CREATED transaction") {
      setUpServerDatabase {
        addTransactions(manuallyAddedTransaction())
      }

      context("with no complement") {
        test("complement is created") {
          transactionMirrorer.mirrorTransactions()

          manuallyAddedTransaction().complementOnServerShould(
            serverDatabase,
            TO_ACCOUNT_AND_BUDGET
          ) {
            amount shouldBe -manuallyAddedTransaction().amount
            importId shouldBe "splity:-350000:2020-02-06:1"
            date shouldBe manuallyAddedTransaction().date
            payeeName shouldBe manuallyAddedTransaction().payeeName
            memo shouldBe manuallyAddedTransaction().memo + " • Out of $350.00, you paid 100.0%"
            cleared shouldBe CLEARED
            approved.shouldBeFalse()
            deleted.shouldBeFalse()
            accountId shouldBe TO_ACCOUNT_ID.plainUuid
          }
          localDatabase.shouldMatchServer(serverDatabase)
          localDatabase.shouldHaveAllTransactionsProcessed()
        }
      }

      context("with an UP_TO_DATE complement") {
        setUpServerDatabase {
          addTransactions(manuallyAddedTransactionComplement())
        }
        setUpLocalDatabase {
          addTransactions(manuallyAddedTransactionComplement(UP_TO_DATE))
        }
        val beginningServerKnowledge = serverDatabase.currentServerKnowledge

        test("nothing happens") {
          transactionMirrorer.mirrorTransactions()

          withClue("Server knowledge should not change") {
            serverDatabase.currentServerKnowledge shouldBe beginningServerKnowledge
          }
          localDatabase.shouldMatchServer(serverDatabase)
          localDatabase.shouldHaveAllTransactionsProcessed()
        }
      }

      context("with a CREATED complement") {
        setUpServerDatabase {
          addTransactions(manuallyAddedTransactionComplement())
        }
        val beginningServerKnowledge = serverDatabase.currentServerKnowledge

        test("nothing happens") {
          transactionMirrorer.mirrorTransactions()

          withClue("Server knowledge should not change") {
            serverDatabase.currentServerKnowledge shouldBe beginningServerKnowledge
          }
          localDatabase.shouldMatchServer(serverDatabase)
        }
      }

      context("with an UPDATED complement") {
        val updatedComplement = manuallyAddedTransactionComplement(UP_TO_DATE).copy(
          amount = -300_000,
          date = manuallyAddedTransactionComplement().date.plusDays(1)
        )
        setUpServerDatabase {
          addTransactions(updatedComplement)
        }
        setUpLocalDatabase {
          addTransactions(manuallyAddedTransactionComplement(UP_TO_DATE))
        }

        test("this transaction is updated") {
          transactionMirrorer.mirrorTransactions()

          updatedComplement.complementOnServerShould(serverDatabase, FROM_ACCOUNT_AND_BUDGET) {
            amount shouldBe -updatedComplement.amount
            date shouldBe updatedComplement.date
            flagColor shouldBe BLUE
            approved.shouldBeFalse()

            payeeName shouldBe manuallyAddedTransaction().payeeName
            memo shouldBe manuallyAddedTransaction().memo
            deleted.shouldBeFalse()
            accountId shouldBe FROM_ACCOUNT_ID.plainUuid
          }
          localDatabase.shouldMatchServer(serverDatabase)
          localDatabase.shouldHaveAllTransactionsProcessed()
        }
      }

      context("with a DELETED complement") {
        val deletedComplement = manuallyAddedTransactionComplement(UP_TO_DATE).copy(
          processedState = DELETED
        )
        setUpServerDatabase {
          addTransactions(deletedComplement)
        }
        setUpLocalDatabase {
          addTransactions(manuallyAddedTransactionComplement(UP_TO_DATE))
        }

        test("this transaction is marked as deleted") {
          transactionMirrorer.mirrorTransactions()

          deletedComplement.complementOnServerShould(serverDatabase, FROM_ACCOUNT_AND_BUDGET) {
            flagColor shouldBe RED
            approved.shouldBeFalse()
            deleted.shouldBeFalse()

            amount shouldBe -deletedComplement.amount
            date shouldBe deletedComplement.date
            payeeName shouldBe manuallyAddedTransaction().payeeName
            memo shouldBe manuallyAddedTransaction().memo
            accountId shouldBe FROM_ACCOUNT_ID.plainUuid
          }
          localDatabase.shouldMatchServer(serverDatabase)
          localDatabase.shouldHaveAllTransactionsProcessed()
        }
      }
    }

    context("with an UPDATED transaction") {
      val updatedTransaction = manuallyAddedTransaction(UP_TO_DATE).copy(
        amount = -300_000,
        date = manuallyAddedTransaction().date.plusDays(1)
      )
      setUpServerDatabase {
        addTransactions(updatedTransaction)
      }
      setUpLocalDatabase {
        addTransactions(manuallyAddedTransaction(UP_TO_DATE))
      }

      context("with no complement") {
        test("this transaction is flagged as an error") {
          transactionMirrorer.mirrorTransactions()

          updatedTransaction.thisOnServerShould(serverDatabase) {
            flagColor shouldBe RED
            approved.shouldBeFalse()
            memo shouldBe "ERROR: This updated transaction has no complement; this shouldn't " +
              "be possible, so delete this transaction and re-create it to get things " +
              "back in sync. • " + updatedTransaction.memo

            amount shouldBe updatedTransaction.amount
            date shouldBe updatedTransaction.date
            payeeName shouldBe updatedTransaction.payeeName
            accountId shouldBe FROM_ACCOUNT_ID.plainUuid
            deleted.shouldBeFalse()
          }
          localDatabase.shouldMatchServer(serverDatabase)
          localDatabase.shouldHaveAllTransactionsProcessedExcept(
            transactions = setOf(manuallyAddedTransaction().id)
          )
        }

        // This error shouldn't be possible, so we're going to ignore it for now
        xcontext("once a complement is created") {
          transactionMirrorer.mirrorTransactions()
          val updatedComplement = manuallyAddedTransactionComplement().copy(
            amount = 300_000,
            date = manuallyAddedTransaction().date.plusDays(1)
          )
          setUpServerDatabase {
            addTransactions(updatedComplement)
          }

          test("nothing happens") {
            val beginningServerKnowledge = serverDatabase.currentServerKnowledge
            transactionMirrorer.mirrorTransactions()

            assertSoftly {
              withClue("Server knowledge should not change") {
                serverDatabase.currentServerKnowledge shouldBe beginningServerKnowledge
              }

              localDatabase.shouldMatchServer(serverDatabase)
              localDatabase.shouldHaveAllTransactionsProcessedExcept(
                setOf(
                  updatedTransaction.id,
                  updatedComplement.id
                ),
              )
            }
          }

          context("once the transaction is unflagged") {
            transactionMirrorer.mirrorTransactions()
            setUpServerDatabase {
              addTransactions(updatedTransaction)
            }

            test("the conflict is resolved") {
              val beginningServerKnowledge = serverDatabase.currentServerKnowledge
              transactionMirrorer.mirrorTransactions()

              assertSoftly {
                withClue("Server knowledge should not change") {
                  serverDatabase.currentServerKnowledge shouldBe beginningServerKnowledge
                }

                updatedComplement.thisOnServerShould(serverDatabase) {
                  amount shouldBe updatedComplement.amount
                  date shouldBe updatedComplement.date
                  payeeName shouldBe updatedComplement.payeeName
                  memo shouldBe updatedComplement.memo
                  cleared shouldBe CLEARED
                  approved.shouldBeTrue()
                  deleted.shouldBeFalse()
                  accountId shouldBe TO_ACCOUNT_ID.plainUuid
                }

                updatedTransaction.thisOnServerShould(serverDatabase) {
                  amount shouldBe updatedTransaction.amount
                  date shouldBe updatedTransaction.date
                  payeeName shouldBe updatedTransaction.payeeName
                  memo shouldBe updatedTransaction.memo
                  cleared shouldBe CLEARED
                  flagColor shouldBe null
                  approved.shouldBeTrue()
                  deleted.shouldBeFalse()
                  accountId shouldBe updatedTransaction.accountId.plainUuid
                }

                localDatabase.shouldMatchServer(serverDatabase)
                localDatabase.shouldHaveAllTransactionsProcessed()
              }
            }
          }
        }
      }

      context("with an UP_TO_DATE complement") {
        setUpServerDatabase {
          addTransactions(manuallyAddedTransactionComplement())
        }
        setUpLocalDatabase {
          addTransactions(manuallyAddedTransactionComplement(UP_TO_DATE))
        }

        test("complement is updated") {
          transactionMirrorer.mirrorTransactions()

          updatedTransaction.complementOnServerShould(serverDatabase, TO_ACCOUNT_AND_BUDGET) {
            amount shouldBe -updatedTransaction.amount
            date shouldBe updatedTransaction.date
            flagColor shouldBe BLUE
            approved.shouldBeFalse()

            payeeName shouldBe manuallyAddedTransactionComplement().payeeName
            memo shouldBe manuallyAddedTransactionComplement().memo
            deleted.shouldBeFalse()
            accountId shouldBe TO_ACCOUNT_ID.plainUuid
          }
          localDatabase.shouldMatchServer(serverDatabase)
          localDatabase.shouldHaveAllTransactionsProcessed()
        }
      }

      context("with a CREATED complement") {
        setUpServerDatabase {
          addTransactions(manuallyAddedTransactionComplement())
        }

        test("complement is updated") {
          transactionMirrorer.mirrorTransactions()

          updatedTransaction.complementOnServerShould(serverDatabase, TO_ACCOUNT_AND_BUDGET) {
            amount shouldBe -updatedTransaction.amount
            date shouldBe updatedTransaction.date
            flagColor shouldBe BLUE
            approved.shouldBeFalse()

            payeeName shouldBe manuallyAddedTransactionComplement().payeeName
            memo shouldBe manuallyAddedTransactionComplement().memo
            deleted.shouldBeFalse()
            accountId shouldBe TO_ACCOUNT_ID.plainUuid
          }
          localDatabase.shouldMatchServer(serverDatabase)
          localDatabase.shouldHaveAllTransactionsProcessed()
        }
      }

      context("with an UPDATED complement") {
        val updatedComplement = manuallyAddedTransactionComplement(UPDATED).copy(amount = -400_000)

        setUpServerDatabase {
          addTransactions(updatedComplement)
        }
        setUpLocalDatabase {
          addTransactions(manuallyAddedTransactionComplement(UP_TO_DATE))
        }

        test("both are flagged as errors") {
          transactionMirrorer.mirrorTransactions()

          updatedTransaction.thisOnServerShould(serverDatabase) {
            flagColor shouldBe RED
            approved.shouldBeFalse()
            memo shouldBe "ERROR: Both this and its complement have been updated; update both to " +
              "the same amount and date to bring them back in sync. • " + updatedTransaction.memo

            amount shouldBe updatedTransaction.amount
            date shouldBe updatedTransaction.date
            payeeName shouldBe updatedTransaction.payeeName
            accountId shouldBe updatedTransaction.accountId.plainUuid
            deleted.shouldBeFalse()
          }

          updatedComplement.thisOnServerShould(serverDatabase) {
            flagColor shouldBe RED
            approved.shouldBeFalse()
            memo shouldBe "ERROR: Both this and its complement have been updated; update both to " +
              "the same amount and date to bring them back in sync. • " + updatedComplement.memo

            amount shouldBe updatedComplement.amount
            date shouldBe updatedComplement.date
            payeeName shouldBe updatedComplement.payeeName
            accountId shouldBe updatedComplement.accountId.plainUuid
            deleted.shouldBeFalse()
          }
          localDatabase.shouldMatchServer(serverDatabase)
          localDatabase.shouldHaveAllTransactionsProcessedExcept(
            transactions = setOf(
              manuallyAddedTransaction().id,
              manuallyAddedTransactionComplement().id
            )
          )
        }

        context("once one side updates") {
          transactionMirrorer.mirrorTransactions()
          setUpServerDatabase {
            addTransactions(manuallyAddedTransactionComplement().copy(approved = true))
          }

          test("nothing happens") {
            val beginningServerKnowledge = serverDatabase.currentServerKnowledge
            transactionMirrorer.mirrorTransactions()

            assertSoftly {
              withClue("Server knowledge should not change") {
                serverDatabase.currentServerKnowledge shouldBe beginningServerKnowledge
              }

              localDatabase.shouldMatchServer(serverDatabase)
              localDatabase.shouldHaveAllTransactionsProcessedExcept(
                setOf(
                  manuallyAddedTransactionComplement().id,
                  manuallyAddedTransaction().id
                ),
              )
            }
          }

          context("once the other side updates") {
            transactionMirrorer.mirrorTransactions()
            setUpServerDatabase {
              addTransactions(manuallyAddedTransaction().copy(cleared = CLEARED))
            }

            test("conflict is resolved") {
              val beginningServerKnowledge = serverDatabase.currentServerKnowledge
              transactionMirrorer.mirrorTransactions()

              withClue("Server knowledge should not change") {
                serverDatabase.currentServerKnowledge shouldBe beginningServerKnowledge
              }

              manuallyAddedTransactionComplement().thisOnServerShould(serverDatabase) {
                amount shouldBe manuallyAddedTransactionComplement().amount
                date shouldBe manuallyAddedTransactionComplement().date
                payeeName shouldBe manuallyAddedTransactionComplement().payeeName
                memo shouldBe manuallyAddedTransactionComplement().memo
                cleared shouldBe CLEARED
                approved.shouldBeTrue()
                deleted.shouldBeFalse()
                accountId shouldBe TO_ACCOUNT_ID.plainUuid
              }

              manuallyAddedTransaction().thisOnServerShould(serverDatabase) {
                amount shouldBe manuallyAddedTransaction().amount
                date shouldBe manuallyAddedTransaction().date
                payeeName shouldBe manuallyAddedTransaction().payeeName
                memo shouldBe manuallyAddedTransaction().memo
                cleared shouldBe CLEARED
                flagColor shouldBe null
                approved.shouldBeTrue()
                deleted.shouldBeFalse()
                accountId shouldBe manuallyAddedTransaction().accountId.plainUuid
              }

              localDatabase.shouldMatchServer(serverDatabase)
              localDatabase.shouldHaveAllTransactionsProcessed()
            }
          }
        }
      }

      context("with a DELETED complement") {
        val deletedComplement = manuallyAddedTransactionComplement(UP_TO_DATE).copy(
          processedState = DELETED
        )
        setUpServerDatabase {
          addTransactions(deletedComplement)
        }
        setUpLocalDatabase {
          addTransactions(manuallyAddedTransactionComplement(UP_TO_DATE))
        }

        test("this transaction is marked as deleted") {
          transactionMirrorer.mirrorTransactions()

          updatedTransaction.thisOnServerShould(serverDatabase) {
            flagColor shouldBe RED
            approved.shouldBeFalse()
            deleted.shouldBeFalse()

            amount shouldBe updatedTransaction.amount
            date shouldBe updatedTransaction.date
            payeeName shouldBe updatedTransaction.payeeName
            memo shouldBe updatedTransaction.memo
            accountId shouldBe FROM_ACCOUNT_ID.plainUuid
          }
          localDatabase.shouldMatchServer(serverDatabase)
          localDatabase.shouldHaveAllTransactionsProcessedExcept(setOf(updatedTransaction.id))
        }
      }
    }

    context("with a DELETED transaction") {
      val deletedTransaction = manuallyAddedTransaction(UP_TO_DATE).copy(processedState = DELETED)
      setUpServerDatabase {
        addTransactions(deletedTransaction)
      }
      setUpLocalDatabase {
        addTransactions(manuallyAddedTransaction(UP_TO_DATE))
      }

      context("with no complement") {
        val beginningServerKnowledge = serverDatabase.currentServerKnowledge

        test("nothing happens") {
          transactionMirrorer.mirrorTransactions()

          withClue("Server knowledge should not change") {
            serverDatabase.currentServerKnowledge shouldBe beginningServerKnowledge
          }
          localDatabase.shouldMatchServer(serverDatabase)
        }
      }

      context("with an UP_TO_DATE complement") {
        setUpServerDatabase {
          addTransactions(manuallyAddedTransactionComplement())
        }
        setUpLocalDatabase {
          addTransactions(manuallyAddedTransactionComplement(UP_TO_DATE))
        }

        test("complement is marked as deleted") {
          transactionMirrorer.mirrorTransactions()

          deletedTransaction.complementOnServerShould(serverDatabase, TO_ACCOUNT_AND_BUDGET) {
            flagColor shouldBe RED
            approved.shouldBeFalse()
            deleted.shouldBeFalse()

            amount shouldBe manuallyAddedTransactionComplement().amount
            date shouldBe manuallyAddedTransactionComplement().date
            payeeName shouldBe manuallyAddedTransactionComplement().payeeName
            memo shouldBe manuallyAddedTransactionComplement().memo
            accountId shouldBe manuallyAddedTransactionComplement().accountId.plainUuid
          }
          localDatabase.shouldMatchServer(serverDatabase)
          localDatabase.shouldHaveAllTransactionsProcessed()
        }

        context("once the complement is also deleted") {
          transactionMirrorer.mirrorTransactions()
          setUpServerDatabase {
            addOrUpdateTransactionsForAccount(
              TO_ACCOUNT_ID,
              listOf(
                serverDatabase.getTransactionsForAccount(TO_ACCOUNT_ID)
                  .find { it.id == manuallyAddedTransactionComplement().id.string }!!
                  .copy(deleted = true)
              )
            )
          }

          test("both transactions are fully deleted") {
            transactionMirrorer.mirrorTransactions()

            deletedTransaction.complementOnServerShould(serverDatabase, TO_ACCOUNT_AND_BUDGET) {
              deleted.shouldBeTrue()
            }
            localDatabase.shouldMatchServer(serverDatabase)
            localDatabase.shouldHaveAllTransactionsProcessed()
          }
        }

        xcontext("once the complement is un-flagged (un-deleted)") {
          /* TODO: This causes problems because you can't recreate the transaction with the same
           * importId. Let's just ignore this for now, and come back to it later.
           *
           * Some ideas for then:
           * - Store the DELETED transaction in the local database even after deletion, update it
           *   rather than creating a whole new transaction. Definitely the best option, but need
           *   to double-check that it works with the API.
           * - Right now, we ignore any changes that aren't amount, date, and approved. Need to add
           *   a "flag" UpdateField to cover un-deletion (and eventually flag-to-split).
           */
          transactionMirrorer.mirrorTransactions()
          val unflaggedTransaction = serverDatabase.getTransactionsForAccount(TO_ACCOUNT_ID)
            .find { it.id == manuallyAddedTransactionComplement().id.string }!!
            .copy(flagColor = null, approved = true)
            .toPublicTransactionDetail(TO_BUDGET_ID, UPDATED)
          setUpServerDatabase {
            addTransactions(unflaggedTransaction)
          }

          test("this transaction is recreated") {
            transactionMirrorer.mirrorTransactions()

            deletedTransaction.complementOnServerShould(serverDatabase, TO_ACCOUNT_AND_BUDGET) {
              flagColor shouldBe null
              approved.shouldBeTrue()
              deleted.shouldBeFalse()

              amount shouldBe manuallyAddedTransactionComplement().amount
              date shouldBe manuallyAddedTransactionComplement().date
              payeeName shouldBe manuallyAddedTransactionComplement().payeeName
              memo shouldBe manuallyAddedTransactionComplement().memo
              accountId shouldBe manuallyAddedTransactionComplement().accountId.plainUuid
            }

            unflaggedTransaction.complementOnServerShould(serverDatabase, TO_ACCOUNT_AND_BUDGET) {
              flagColor shouldBe null
              approved.shouldBeTrue()
              deleted.shouldBeFalse()

              amount shouldBe manuallyAddedTransactionComplement().amount
              date shouldBe manuallyAddedTransactionComplement().date
              payeeName shouldBe manuallyAddedTransactionComplement().payeeName
              memo shouldBe manuallyAddedTransactionComplement().memo
              accountId shouldBe manuallyAddedTransactionComplement().accountId.plainUuid
            }
            localDatabase.shouldMatchServer(serverDatabase)
            localDatabase.shouldHaveAllTransactionsProcessed()
          }
        }
      }

      context("with a CREATED complement") {
        setUpServerDatabase {
          addTransactions(manuallyAddedTransactionComplement())
        }

        test("complement is marked as deleted") {
          transactionMirrorer.mirrorTransactions()

          deletedTransaction.complementOnServerShould(serverDatabase, TO_ACCOUNT_AND_BUDGET) {
            flagColor shouldBe RED
            approved.shouldBeFalse()
            deleted.shouldBeFalse()

            amount shouldBe manuallyAddedTransactionComplement().amount
            date shouldBe manuallyAddedTransactionComplement().date
            payeeName shouldBe manuallyAddedTransactionComplement().payeeName
            memo shouldBe manuallyAddedTransactionComplement().memo
            accountId shouldBe manuallyAddedTransactionComplement().accountId.plainUuid
          }
          localDatabase.shouldMatchServer(serverDatabase)
          localDatabase.shouldHaveAllTransactionsProcessed()
        }
      }

      context("with an UPDATED complement") {
        val updatedComplement =
          manuallyAddedTransactionComplement(UP_TO_DATE).copy(amount = -300_000)
        setUpServerDatabase {
          addTransactions(updatedComplement)
        }
        setUpLocalDatabase {
          addTransactions(manuallyAddedTransactionComplement(UP_TO_DATE))
        }

        test("complement is marked as deleted") {
          transactionMirrorer.mirrorTransactions()

          updatedComplement.thisOnServerShould(serverDatabase) {
            flagColor shouldBe RED
            approved.shouldBeFalse()
            deleted.shouldBeFalse()

            amount shouldBe updatedComplement.amount
            date shouldBe updatedComplement.date
            payeeName shouldBe updatedComplement.payeeName
            memo shouldBe updatedComplement.memo
            accountId shouldBe updatedComplement.accountId.plainUuid
          }
          localDatabase.shouldMatchServer(serverDatabase)
          localDatabase.shouldHaveAllTransactionsProcessedExcept(
            setOf(updatedComplement.id)
          )
        }
      }

      context("with a DELETED complement") {
        val deletedComplement = manuallyAddedTransactionComplement(UP_TO_DATE).copy(
          processedState = DELETED
        )
        setUpServerDatabase {
          addTransactions(deletedComplement)
        }
        setUpLocalDatabase {
          addTransactions(manuallyAddedTransactionComplement(UP_TO_DATE))
        }
        val beginningServerKnowledge = serverDatabase.currentServerKnowledge

        test("nothing happens") {
          transactionMirrorer.mirrorTransactions()

          withClue("Server knowledge should not change") {
            serverDatabase.currentServerKnowledge shouldBe beginningServerKnowledge
          }
          localDatabase.shouldMatchServer(serverDatabase)
        }
      }
    }

    context("with a recurring (unapproved) split into an account") {
      setUpServerDatabase {
        addTransactions(
          transactionTransferSplitSource(),
          transactionAddedFromTransfer(isFromSplitSource = true).copy(approved = false)
        )
      }

      test("the unapproved transaction is ignored") {
        val beginningServerKnowledge = serverDatabase.currentServerKnowledge
        transactionMirrorer.mirrorTransactions()

        withClue("Server knowledge should not change") {
          serverDatabase.currentServerKnowledge shouldBe beginningServerKnowledge
        }
        localDatabase.shouldMatchServer(serverDatabase)
        localDatabase.shouldHaveAllTransactionsProcessedExcept(
          setOf(transactionAddedFromTransfer(isFromSplitSource = true).id)
        )
      }

      context("when approved") {
        transactionMirrorer.mirrorTransactions()
        setUpServerDatabase {
          addTransactions(transactionAddedFromTransfer(isFromSplitSource = true))
        }

        test("the approved transaction is mirrored") {
          transactionMirrorer.mirrorTransactions()

          transactionAddedFromTransfer(isFromSplitSource = true).complementOnServerShould(
            serverDatabase,
            TO_ACCOUNT_AND_BUDGET
          ) {
            approved.shouldBeFalse()

            amount shouldBe -transactionAddedFromTransfer(isFromSplitSource = true).amount
            importId shouldBe "splity:-10000:2020-02-07:1"
            date shouldBe transactionAddedFromTransfer(isFromSplitSource = true).date
            payeeName shouldBe transactionTransferSplitSource().payeeName
            memo shouldBe transactionTransferSplitSource().memo + " • Out of $30.00, you paid 33.3%"
            cleared shouldBe CLEARED
            deleted.shouldBeFalse()
            accountId shouldBe TO_ACCOUNT_ID.plainUuid
          }
          localDatabase.shouldMatchServer(serverDatabase)
          localDatabase.shouldHaveAllTransactionsProcessed()
        }

        context("when the complement is updated") {
          transactionMirrorer.mirrorTransactions()
          val existingComplement = serverDatabase.getTransactionsForAccount(TO_ACCOUNT_ID)
            .find { it isComplementOf transactionAddedFromTransfer(isFromSplitSource = true).toApiTransaction() }!!
            .toPublicTransactionDetail(TO_BUDGET_ID, UPDATED)
          val updatedComplement = existingComplement
            .copy(approved = true)
            .copy(amount = -15_000)
          setUpServerDatabase {
            addTransactions(updatedComplement)
          }

          test("the updated complement is marked as an error") {
            transactionMirrorer.mirrorTransactions()

            assertSoftly {
              updatedComplement.thisOnServerShould(serverDatabase) {
                flagColor shouldBe RED
                approved.shouldBeFalse()
                memo shouldBe "ERROR: Mirrors of transfers from split transactions can't be " +
                  "updated, so the change has been undone. Change the mirror of this transaction " +
                  "in the other budget instead. • " + existingComplement.memo

                amount shouldBe existingComplement.amount
                date shouldBe existingComplement.date
                payeeName shouldBe existingComplement.payeeName
                accountId shouldBe existingComplement.accountId.plainUuid
                deleted.shouldBeFalse()
              }
              localDatabase.shouldMatchServer(serverDatabase)
              localDatabase.shouldHaveAllTransactionsProcessedExcept(
                setOf(updatedComplement.id)
              )
            }
          }
        }

        context("when the complement is approved") {
          transactionMirrorer.mirrorTransactions()
          setUpServerDatabase {
            addOrUpdateTransactionsForAccount(
              TO_ACCOUNT_ID,
              listOf(
                serverDatabase.getTransactionsForAccount(TO_ACCOUNT_ID)
                  .find {
                    it.isComplementOf(
                      transactionAddedFromTransfer(isFromSplitSource = true).toApiTransaction()
                    )
                  }!!
                  .copy(approved = true)
              )
            )
          }

          test("nothing happens") {
            val beginningServerKnowledge = serverDatabase.currentServerKnowledge
            transactionMirrorer.mirrorTransactions()

            withClue("Server knowledge should not change") {
              serverDatabase.currentServerKnowledge shouldBe beginningServerKnowledge
            }
            localDatabase.shouldHaveAllTransactionsProcessed()
            localDatabase.shouldMatchServer(serverDatabase)
          }
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
  context("with simple CREATED transactions") {
    context("with no new transactions") {
      test("mirrorTransactions does nothing") {
        transactionMirrorer.mirrorTransactions()

        localDatabase.shouldHaveAllTransactionsProcessed()
      }
    }

    context("created transactions with no complement") {
      context("with a manually added transaction") {
        setUpServerDatabase {
          addTransactions(manuallyAddedTransaction())
        }

        mirrorTransactionMirrorsTransaction(
          transactionToMirror = manuallyAddedTransaction(),
          transactionMirrorer = transactionMirrorer,
          localDatabase = localDatabase,
          serverDatabase = serverDatabase,
          toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
        ) {
          amount shouldBe -manuallyAddedTransaction().amount
          importId shouldBe "splity:-350000:2020-02-06:1"
          date shouldBe manuallyAddedTransaction().date
          payeeName shouldBe manuallyAddedTransaction().payeeName
          memo shouldBe manuallyAddedTransaction().memo + " • Out of $350.00, you paid 100.0%"
          cleared shouldBe CLEARED
          approved.shouldBeFalse()
          deleted.shouldBeFalse()
          accountId shouldBe TO_ACCOUNT_ID.plainUuid
        }
      }

      context("with a non-split transfer") {
        setUpServerDatabase {
          addOrUpdateTransactionsForAccount(
            FROM_ACCOUNT_ID,
            listOf(transactionAddedFromTransfer(isFromSplitSource = false).toApiTransaction())
          )
          addOrUpdateTransactionsForAccount(
            FROM_TRANSFER_SOURCE_ACCOUNT_ID,
            listOf(transactionTransferNonSplitSource().toApiTransaction())
          )
        }

        mirrorTransactionMirrorsTransaction(
          transactionToMirror = transactionAddedFromTransfer(isFromSplitSource = false),
          transactionMirrorer = transactionMirrorer,
          localDatabase = localDatabase,
          serverDatabase = serverDatabase,
          toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
        ) {
          amount shouldBe -transactionAddedFromTransfer(isFromSplitSource = false).amount
          importId shouldBe "splity:-10000:2020-02-07:1"
          date shouldBe transactionAddedFromTransfer(isFromSplitSource = false).date
          payeeName shouldBe "Chicken Butt"
          memo shouldBe transactionTransferNonSplitSource().memo + " • Out of $10.00, you paid 100.0%"
          cleared shouldBe CLEARED
          approved.shouldBeFalse()
          deleted.shouldBeFalse()
          accountId shouldBe TO_ACCOUNT_ID.plainUuid
        }
      }

      context("with a split transfer") {
        setUpServerDatabase {
          addTransactions(
            transactionAddedFromTransfer(isFromSplitSource = true),
            transactionTransferSplitSource()
          )
        }

        mirrorTransactionMirrorsTransaction(
          transactionToMirror = transactionAddedFromTransfer(isFromSplitSource = true),
          transactionMirrorer = transactionMirrorer,
          localDatabase = localDatabase,
          serverDatabase = serverDatabase,
          toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
        ) {
          amount shouldBe -transactionAddedFromTransfer(isFromSplitSource = true).amount
          importId shouldBe "splity:${
            -transactionAddedFromTransfer(isFromSplitSource = true).amount
          }:${
            transactionAddedFromTransfer(isFromSplitSource = true).date
          }:1"
          date shouldBe transactionAddedFromTransfer(isFromSplitSource = true).date
          payeeName shouldBe transactionTransferSplitSource().payeeName
          memo shouldBe transactionTransferSplitSource().memo + " • Out of $30.00, you paid 33.3%"
          cleared shouldBe CLEARED
          approved.shouldBeFalse()
          deleted.shouldBeFalse()
          accountId shouldBe TO_ACCOUNT_ID.plainUuid
        }
      }

      context("with a recurring split transaction") {
        val transactionAddedFromTransferWithLongId =
          transactionAddedFromTransfer(isFromSplitSource = true).copy(
            id = transactionAddedFromTransfer(isFromSplitSource = true).id + "_st_1_2020-06-20"
          )

        setUpServerDatabase {
          addOrUpdateTransactionsForAccount(
            FROM_ACCOUNT_ID,
            listOf(transactionAddedFromTransferWithLongId.toApiTransaction())
          )
          addOrUpdateTransactionsForAccount(
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
          localDatabase = localDatabase,
          serverDatabase = serverDatabase,
          toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
        ) {
          amount shouldBe -transactionAddedFromTransferWithLongId.amount
          importId shouldBe "splity:${
            -transactionAddedFromTransferWithLongId.amount
          }:${
            transactionAddedFromTransferWithLongId.date
          }:1"
          date shouldBe transactionAddedFromTransferWithLongId.date
          payeeName shouldBe transactionTransferSplitSource().payeeName
          memo shouldBe transactionTransferSplitSource().memo + " • Out of $30.00, you paid 33.3%"
          cleared shouldBe CLEARED
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
          addOrUpdateTransactionsForAccount(
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
          addOrUpdateTransactionsForAccount(
            FROM_ACCOUNT_ID,
            listOf(manuallyAddedTransaction().toApiTransaction())
          )
          addOrUpdateTransactionsForAccount(
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
          addOrUpdateTransactionsForAccount(
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
            .copy(amount = 200_000)

          setUpLocalDatabase {
            addTransactions(oldManualTransaction)
          }

          test("mirrorTransactions doesn't duplicate the transaction") {
            transactionMirrorer.mirrorTransactions()

            withClue("Server transactions") {
              serverDatabase.getTransactionsForAccount(FROM_ACCOUNT_ID)
                .shouldContain(redFlaggedTransaction.toApiTransaction())
              serverDatabase.getTransactionsForAccount(TO_ACCOUNT_ID)
                .toPublicTransactionDetailList(TO_BUDGET_ID, UP_TO_DATE)
                .shouldNotContainComplementOf(redFlaggedTransaction)
            }
            withClue("Replaced transactions") {
              localDatabase.getAllReplacedTransactions().shouldContainExactly(oldManualTransaction)
            }
            withClue("All transactions transactions") {
              localDatabase.getAllTransactions()
                .shouldContain(redFlaggedTransaction.copy(processedState = UPDATED))
              localDatabase.getAllTransactions()
                .shouldNotContainComplementOf(redFlaggedTransaction)
            }
          }
        }
      }
    }
  }

  context("with a purple-flagged transaction in another account") {
    setUpServerDatabase {
      addTransactions(unremarkableTransactionInTransferSource().copy(flagColor = PURPLE))
    }

    test("should mirror transaction") {
      transactionMirrorer.mirrorTransactions()

      val halfAmount = unremarkableTransactionInTransferSource().amount / 2

      withClue("Newly-split transaction") {
        unremarkableTransactionInTransferSource().thisOnServerShould(serverDatabase) {
          date shouldBe unremarkableTransactionInTransferSource().date
          payeeId shouldBe unremarkableTransactionInTransferSource().payeeId?.plainUuid
          payeeName shouldBe unremarkableTransactionInTransferSource().payeeName
          memo shouldBe unremarkableTransactionInTransferSource().memo
          cleared shouldBe CLEARED
          approved.shouldBeTrue()
          deleted.shouldBeFalse()
          accountId shouldBe FROM_TRANSFER_SOURCE_ACCOUNT_ID.plainUuid
          // TODO: categoryId.shouldBeNull()
          subtransactions.shouldForAny { subTransaction ->
            with(subTransaction) {
              transactionId shouldBe unremarkableTransactionInTransferSource().id.string
              amount shouldBe halfAmount
              deleted.shouldBeFalse()
              memo.shouldBeNull()
              payeeId shouldBe FROM_ACCOUNT_PAYEE_ID.plainUuid
              payeeName.shouldBeNull()
              categoryId.shouldBeNull()
              // TODO: fix limitation of our fake backend
              // transferAccountId shouldBe FROM_ACCOUNT_ID
              // transferTransactionId.shouldNotBeNull()
            }
          }
          subtransactions.shouldForAny { subTransaction ->
            with(subTransaction) {
              transactionId shouldBe unremarkableTransactionInTransferSource().id.string
              amount shouldBe halfAmount
              deleted.shouldBeFalse()
              memo.shouldBeNull()
              payeeId.shouldBeNull()
              payeeName.shouldBeNull()
              categoryId shouldBe unremarkableTransactionInTransferSource().categoryId!!.plainUuid
              transferAccountId.shouldBeNull()
              transferTransactionId.shouldBeNull()
            }
          }
        }
      }

      withClue("Generated split transaction") {
        complementOnServerShould(
          -halfAmount,
          unremarkableTransactionInTransferSource().date,
          serverDatabase,
          FROM_ACCOUNT_AND_BUDGET
        ) {
          importId shouldBe null
          date shouldBe unremarkableTransactionInTransferSource().date
          payeeName shouldBe "Transfer : $FROM_TRANSFER_SOURCE_ACCOUNT_NAME"
          memo shouldBe null
          cleared shouldBe UNCLEARED
          approved.shouldBeTrue()
          deleted.shouldBeFalse()
          accountId shouldBe FROM_ACCOUNT_ID.plainUuid
        }
      }

      withClue("Mirrored complement transaction") {
        complementOnServerShould(
          halfAmount,
          unremarkableTransactionInTransferSource().date,
          serverDatabase,
          TO_ACCOUNT_AND_BUDGET
        ) {
          importId shouldBe "splity:${halfAmount}:${unremarkableTransactionInTransferSource().date}:1"
          date shouldBe unremarkableTransactionInTransferSource().date
          payeeName shouldBe unremarkableTransactionInTransferSource().payeeName
          memo shouldBe unremarkableTransactionInTransferSource().memo + " • Out of $101.00, you paid 50.0%"
          cleared shouldBe CLEARED
          approved.shouldBeFalse()
          deleted.shouldBeFalse()
          accountId shouldBe TO_ACCOUNT_ID.plainUuid
        }
      }

      localDatabase.shouldMatchServer(serverDatabase)
      localDatabase.shouldHaveAllTransactionsProcessed()
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
  localDatabase: Database,
  serverDatabase: FakeYnabServerDatabase,
  toAccountAndBudget: AccountAndBudget,
  assertSoftly: TransactionDetail.(TransactionDetail) -> Unit
) {
  test("mirrorTransactions mirrors the transaction") {
    transactionMirrorer.mirrorTransactions()

    transactionToMirror.complementOnServerShould(
      serverDatabase,
      toAccountAndBudget,
      assertSoftly
    )
    localDatabase.shouldMatchServer(serverDatabase)
    localDatabase.shouldHaveAllTransactionsProcessed()
  }
}

private fun PublicTransactionDetail.thisOnServerShould(
  serverDatabase: FakeYnabServerDatabase,
  assertSoftly: TransactionDetail.(TransactionDetail) -> Unit
) {
  val transactionsInToAccount =
    serverDatabase.getTransactionsForAccount(accountId)

  transactionsInToAccount.shouldForOne { it.id shouldBe this.id.string }

  val transaction = transactionsInToAccount
    .find { transaction -> transaction.id == this.id.string }!!

  withClue("For transaction $transaction") {
    assertSoftly(transaction, assertSoftly)
  }
}

private fun PublicTransactionDetail.complementOnServerShould(
  serverDatabase: FakeYnabServerDatabase,
  complementAccountAndBudget: AccountAndBudget,
  assertSoftly: TransactionDetail.(TransactionDetail) -> Unit
) {
  val transactionsInToAccount =
    serverDatabase.getTransactionsForAccount(complementAccountAndBudget.accountId)

  transactionsInToAccount
    .toPublicTransactionDetailList(complementAccountAndBudget.budgetId, UP_TO_DATE)
    .shouldContainSingleComplementOf(this)

  val complement = transactionsInToAccount.find { transaction ->
    transaction.toPublicTransactionDetail(complementAccountAndBudget.budgetId) isComplementOf this
  }!!

  withClue("For complement $complement") {
    assertSoftly(complement, assertSoftly)
  }
}

private fun complementOnServerShould(
  complementAmount: Long,
  complementDate: LocalDate,
  serverDatabase: FakeYnabServerDatabase,
  complementAccountAndBudget: AccountAndBudget,
  assertSoftly: TransactionDetail.(TransactionDetail) -> Unit
) {
  val transactionsInToAccount =
    serverDatabase.getTransactionsForAccount(complementAccountAndBudget.accountId)

  transactionsInToAccount.shouldForAny { transaction ->
    transaction.date == complementDate && transaction.amount == complementAmount
  }

  val complement = transactionsInToAccount.find { transaction ->
    transaction.date == complementDate && transaction.amount == complementAmount
  }!!

  withClue("For complement $complement") {
    assertSoftly(complement, assertSoftly)
  }
}
