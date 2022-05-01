package co.moelten.splity

import co.moelten.splity.database.ProcessedState
import co.moelten.splity.database.plus
import co.moelten.splity.database.toTransactionId
import co.moelten.splity.injection.createFakeSplityComponent
import co.moelten.splity.test.Setup
import co.moelten.splity.test.addTransactions
import com.ryanmoelter.ynab.database.Database
import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.SaveTransaction
import com.youneedabudget.client.models.SaveTransactionWrapper
import com.youneedabudget.client.models.TransactionDetail
import com.youneedabudget.client.models.TransactionResponse
import com.youneedabudget.client.models.TransactionResponseData
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import java.util.UUID.randomUUID

internal class MirrorTransactionsTest : FunSpec({
  val serverDatabase = FakeYnabServerDatabase()
  val component = createFakeSplityComponent(serverDatabase)
  val localDatabase = component.database
  val transactionMirrorer = component.transactionMirrorer

  val setUpServerDatabase: Setup<FakeYnabServerDatabase> = { setUp -> serverDatabase.also(setUp) }

  val setUpLocalDatabase: Setup<Database> = { setUp -> localDatabase.also(setUp) }

  context("add transaction") {

    test("add") {
      val actions = runBlocking {
        transactionMirrorer.createDifferentialActionsForBothAccounts(
          firstTransactions = listOf(manuallyAddedTransaction),
          secondTransactions = listOf(),
          firstAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
          secondAccountAndBudget = AccountAndBudget(TO_ACCOUNT_ID, TO_BUDGET_ID),
          startDate = LocalDate.of(1994, Month.FEBRUARY, 6)
        )
      }

      expectThat(actions) {
        hasSize(1)
        contains(
          CompleteTransactionAction(
            TransactionAction.Create(manuallyAddedTransaction),
            FROM_ACCOUNT_AND_BUDGET,
            TO_ACCOUNT_AND_BUDGET
          )
        )
      }
    }

    test("ignore unapproved") {
      val unapprovedTransaction = manuallyAddedTransaction.copy(approved = false)
      val actions = runBlocking {
        transactionMirrorer.createDifferentialActionsForBothAccounts(
          firstTransactions = listOf(unapprovedTransaction),
          secondTransactions = listOf(),
          firstAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
          secondAccountAndBudget = TO_ACCOUNT_AND_BUDGET,
          startDate = LocalDate.of(1994, Month.FEBRUARY, 6)
        )
      }

      expectThat(actions) {
        isEmpty()
      }
    }

    test("ignore alreadyAdded") {
      val actions = runBlocking {
        transactionMirrorer.createDifferentialActionsForBothAccounts(
          firstTransactions = listOf(manuallyAddedTransaction.copy(processedState = ProcessedState.UPDATED)),
          secondTransactions = listOf(manuallyAddedTransactionComplement),
          firstAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
          secondAccountAndBudget = TO_ACCOUNT_AND_BUDGET,
          startDate = LocalDate.of(1994, Month.FEBRUARY, 6)
        )
      }

      expectThat(actions).isEmpty()
    }

    test("ignore complement") {
      val actions = runBlocking {
        transactionMirrorer.createDifferentialActionsForBothAccounts(
          firstTransactions = listOf(manuallyAddedTransactionComplement),
          secondTransactions = listOf(manuallyAddedTransaction),
          firstAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
          secondAccountAndBudget = TO_ACCOUNT_AND_BUDGET,
          startDate = LocalDate.of(1994, Month.FEBRUARY, 6)
        )
      }

      expectThat(actions) {
        isEmpty()
      }
    }

    test("ignore complement with recurring split") {
      val transactionAddedFromTransferWithLongId = transactionAddedFromTransfer.copy(
        id = transactionAddedFromTransfer.id + "_st_1_2020-06-20"
      )
      val transactionAddedFromTransferWithLongIdComplement =
        transactionAddedFromTransferWithLongId.copy(
          id = randomUUID().toTransactionId(),
          amount = -transactionAddedFromTransferWithLongId.amount,
          importId = subTransactionTransferSplitSource.id.toString(),
          cleared = TransactionDetail.ClearedEnum.CLEARED,
          approved = false
        )
      setUpLocalDatabase {
        addTransactions(
          listOf(
            transactionTransferSplitSource.copy(
              subTransactions = listOf(
                subTransactionNonTransferSplitSource,
                subTransactionTransferSplitSource.copy(
                  transferTransactionId = subTransactionTransferSplitSource.transferTransactionId!! +
                    "_st_1_2020-06-20"
                )
              )
            )
          )
        )
      }
      val actions = runBlocking {
        transactionMirrorer.createDifferentialActionsForBothAccounts(
          firstTransactions = listOf(transactionAddedFromTransferWithLongId),
          secondTransactions = listOf(transactionAddedFromTransferWithLongIdComplement),
          firstAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
          secondAccountAndBudget = TO_ACCOUNT_AND_BUDGET,
          startDate = LocalDate.of(1994, Month.FEBRUARY, 6)
        )
      }

      expectThat(actions) {
        isEmpty()
      }
    }

    test("ignore already added with no importId") {
      val manuallyAddedTransactionComplementWithoutImportId =
        manuallyAddedTransactionComplement.copy(
          importId = null,
          memo = "I'm a different memo"
        )
      val actions = runBlocking {
        transactionMirrorer.createDifferentialActionsForBothAccounts(
          firstTransactions = listOf(manuallyAddedTransaction),
          secondTransactions = listOf(manuallyAddedTransactionComplementWithoutImportId),
          firstAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
          secondAccountAndBudget = TO_ACCOUNT_AND_BUDGET,
          startDate = LocalDate.of(1994, Month.FEBRUARY, 6)
        )
      }

      expectThat(actions).isEmpty()
    }

    test("ignore before start date") {
      val actions = runBlocking {
        transactionMirrorer.createDifferentialActionsForBothAccounts(
          firstTransactions = listOf(manuallyAddedTransaction),
          secondTransactions = emptyList(),
          firstAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
          secondAccountAndBudget = TO_ACCOUNT_AND_BUDGET,
          startDate = manuallyAddedTransaction.date.plusDays(1)
        )
      }

      expectThat(actions).isEmpty()
    }
  }

  xtest("updateTransaction_approved") {
    val manuallyAddedTransactionComplementApproved =
      manuallyAddedTransactionComplement.copy(approved = true)

    val actions = runBlocking {
      transactionMirrorer.createDifferentialActionsForBothAccounts(
        firstTransactions = listOf(manuallyAddedTransactionComplementApproved),
        secondTransactions = listOf(manuallyAddedTransaction),
        firstAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
        secondAccountAndBudget = TO_ACCOUNT_AND_BUDGET,
        startDate = LocalDate.of(1994, Month.FEBRUARY, 6)
      )
    }

    expectThat(actions) {
      hasSize(1)
      contains(
        TransactionAction.Update(
          manuallyAddedTransactionComplementApproved,
          manuallyAddedTransaction,
          setOf(UpdateField.CLEAR)
        )
      )
    }
  }

  context("apply actions") {

    test("create") {
      setUpServerDatabase { }

      runBlocking {
        transactionMirrorer.applyActions(
          listOf(
            CompleteTransactionAction(
              transactionAction = TransactionAction.Create(manuallyAddedTransaction),
              fromAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
              toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
            )
          )
        )
      }

      val transactionList =
        serverDatabase.accountToTransactionsMap.getValue(TO_ACCOUNT_ID)
      expect {
        that(transactionList).hasSize(1)
        that(transactionList[0].amount).isEqualTo(-manuallyAddedTransaction.amount)
        that(transactionList[0].importId).isEqualTo("splity:-350000:2020-02-06:1")
        that(transactionList[0].date).isEqualTo(manuallyAddedTransaction.date)
        that(transactionList[0].payeeName).isEqualTo(manuallyAddedTransaction.payeeName)
        that(transactionList[0].memo).isEqualTo(manuallyAddedTransaction.memo + " • Out of $350.00, you paid 100.0%")
        that(transactionList[0].cleared).isEqualTo(TransactionDetail.ClearedEnum.CLEARED)
        that(transactionList[0].approved).isFalse()
        that(transactionList[0].accountId).isEqualTo(TO_ACCOUNT_ID.plainUuid)
      }
    }

    test("create from transfer") {
      setUpLocalDatabase {
        addTransactions(listOf(transactionTransferNonSplitSource))
      }

      runBlocking {
        transactionMirrorer.applyActions(
          listOf(
            CompleteTransactionAction(
              transactionAction = TransactionAction.Create(transactionAddedFromTransfer),
              fromAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
              toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
            )
          )
        )
      }

      val transactionList =
        serverDatabase.accountToTransactionsMap.getValue(TO_ACCOUNT_ID)
      transactionList shouldHaveSize 1
      assertSoftly(transactionList.first()) {
        amount shouldBe -transactionAddedFromTransfer.amount
        importId shouldBe "splity:${-transactionAddedFromTransfer.amount}:${transactionAddedFromTransfer.date}:1"
        date shouldBe transactionAddedFromTransfer.date
        payeeName shouldBe "Chicken Butt"
        memo shouldBe transactionTransferNonSplitSource.memo + " • Out of $10.00, you paid 100.0%"
        cleared shouldBe TransactionDetail.ClearedEnum.CLEARED
        approved.shouldBeFalse()
        accountId shouldBe TO_ACCOUNT_ID.plainUuid
      }
    }

    test("create from transfer without duplicating network calls") {
      setUpLocalDatabase {
        addTransactions(listOf(transactionTransferNonSplitSource))
      }
      runBlocking {
        transactionMirrorer.applyActions(
          listOf(
            CompleteTransactionAction(
              transactionAction = TransactionAction.Create(transactionAddedFromTransfer),
              fromAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
              toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
            )
          )
        )
      }

      val transactionList =
        serverDatabase.accountToTransactionsMap.getValue(TO_ACCOUNT_ID)
      expect {
        that(transactionList).hasSize(1)
        that(transactionList[0].amount).isEqualTo(-transactionAddedFromTransfer.amount)
        that(transactionList[0].importId).isEqualTo("splity:${-transactionAddedFromTransfer.amount}:${transactionAddedFromTransfer.date}:1")
        that(transactionList[0].date).isEqualTo(transactionAddedFromTransfer.date)
        that(transactionList[0].payeeName).isEqualTo("Chicken Butt")
        that(transactionList[0].memo).isEqualTo(transactionTransferNonSplitSource.memo + " • Out of $10.00, you paid 100.0%")
        that(transactionList[0].cleared).isEqualTo(TransactionDetail.ClearedEnum.CLEARED)
        that(transactionList[0].approved).isFalse()
        that(transactionList[0].accountId).isEqualTo(TO_ACCOUNT_ID.plainUuid)
      }
    }

    test("create from split transfer") {
      setUpLocalDatabase {
        addTransactions(listOf(transactionTransferSplitSource))
      }
      runBlocking {
        transactionMirrorer.applyActions(
          listOf(
            CompleteTransactionAction(
              transactionAction = TransactionAction.Create(transactionAddedFromTransfer),
              fromAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
              toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
            )
          )
        )
      }

      val transactionList =
        serverDatabase.accountToTransactionsMap.getValue(TO_ACCOUNT_ID)
      expect {
        that(transactionList).hasSize(1)
        that(transactionList[0].amount).isEqualTo(-transactionAddedFromTransfer.amount)
        that(transactionList[0].importId).isEqualTo("splity:${-transactionAddedFromTransfer.amount}:${transactionAddedFromTransfer.date}:1")
        that(transactionList[0].date).isEqualTo(transactionAddedFromTransfer.date)
        that(transactionList[0].payeeName).isEqualTo(transactionTransferSplitSource.payeeName)
        that(transactionList[0].memo).isEqualTo(transactionTransferSplitSource.memo + " • Out of $30.00, you paid 33.3%")
        that(transactionList[0].cleared).isEqualTo(TransactionDetail.ClearedEnum.CLEARED)
        that(transactionList[0].approved).isFalse()
        that(transactionList[0].accountId).isEqualTo(TO_ACCOUNT_ID.plainUuid)
      }
    }

    test("create from split transfer: recurring transaction") {
      val transactionAddedFromTransferWithLongId = transactionAddedFromTransfer.copy(
        id = transactionAddedFromTransfer.id + "_st_1_2020-06-20"
      )
      setUpLocalDatabase {
        addTransactions(
          listOf(
            transactionTransferSplitSource.copy(
              subTransactions = listOf(
                subTransactionNonTransferSplitSource,
                subTransactionTransferSplitSource.copy(
                  transferTransactionId = subTransactionTransferSplitSource.transferTransactionId!! +
                    "_st_1_2020-06-20"
                )
              )
            )
          )
        )
      }
      runBlocking {
        transactionMirrorer.applyActions(
          listOf(
            CompleteTransactionAction(
              transactionAction = TransactionAction.Create(transactionAddedFromTransferWithLongId),
              fromAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
              toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
            )
          )
        )
      }

      val transactionList =
        serverDatabase.accountToTransactionsMap.getValue(TO_ACCOUNT_ID)
      expect {
        that(transactionList).hasSize(1)
        that(transactionList[0].amount).isEqualTo(-transactionAddedFromTransferWithLongId.amount)
        that(transactionList[0].importId).isEqualTo("splity:${-transactionAddedFromTransferWithLongId.amount}:${transactionAddedFromTransferWithLongId.date}:1")
        that(transactionList[0].date).isEqualTo(transactionAddedFromTransferWithLongId.date)
        that(transactionList[0].payeeName).isEqualTo(transactionTransferSplitSource.payeeName)
        that(transactionList[0].memo).isEqualTo(transactionTransferSplitSource.memo + " • Out of $30.00, you paid 33.3%")
        that(transactionList[0].cleared).isEqualTo(TransactionDetail.ClearedEnum.CLEARED)
        that(transactionList[0].approved).isFalse()
        that(transactionList[0].accountId).isEqualTo(TO_ACCOUNT_ID.plainUuid)
      }
    }

    test("update approved") {
      val ynab = mockk<YnabClient>()
      coEvery { ynab.transactions.updateTransaction(any(), any(), any()) } returns
        TransactionResponse(TransactionResponseData(mockk()))
      runBlocking {
        transactionMirrorer.applyActions(
          listOf(
            CompleteTransactionAction(
              transactionAction = TransactionAction.Update(
                fromTransaction = manuallyAddedTransactionComplement.copy(approved = true),
                toTransaction = manuallyAddedTransaction,
                updateFields = setOf(UpdateField.CLEAR)
              ),
              fromAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
              toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
            )
          )
        )
      }

      val saveTransactionSlot = slot<SaveTransactionWrapper>()
      coVerify {
        ynab.transactions.updateTransaction(
          TO_BUDGET_ID.toString(),
          manuallyAddedTransaction.id.string,
          capture(saveTransactionSlot)
        )
      }
      expectThat(saveTransactionSlot.captured.transaction).isEqualTo(
        SaveTransaction(
          accountId = manuallyAddedTransaction.accountId.plainUuid,
          date = manuallyAddedTransaction.date,
          amount = manuallyAddedTransaction.amount,
          payeeId = manuallyAddedTransaction.payeeId!!.plainUuid,
          payeeName = null,
          categoryId = manuallyAddedTransaction.categoryId!!.plainUuid,
          memo = manuallyAddedTransaction.memo,
          cleared = SaveTransaction.ClearedEnum.CLEARED,
          approved = manuallyAddedTransaction.approved,
          flagColor = manuallyAddedTransaction.flagColor?.toSaveTransactionFlagColorEnum(),
          importId = manuallyAddedTransaction.importId,
          subtransactions = null
        )
      )
    }
  }
})
