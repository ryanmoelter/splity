package co.moelten.splity

import co.moelten.splity.database.plus
import co.moelten.splity.injection.createFakeSplityComponent
import co.moelten.splity.test.Setup
import co.moelten.splity.test.addTransactions
import co.moelten.splity.test.toApiTransaction
import com.ryanmoelter.ynab.database.Database
import com.youneedabudget.client.models.TransactionDetail
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class ActionApplierTest : FunSpec({
  val serverDatabase = FakeYnabServerDatabase()
  val component = createFakeSplityComponent(serverDatabase)
  val localDatabase = component.database
  val actionApplier = component.actionApplier

  val setUpServerDatabase: Setup<FakeYnabServerDatabase> = { setUp -> serverDatabase.also(setUp) }

  val setUpLocalDatabase: Setup<Database> = { setUp -> localDatabase.also(setUp) }

  test("create") {
    setUpServerDatabase { }

    actionApplier.applyActions(
      TransactionAction.CreateComplement(
        manuallyAddedTransaction,
        toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
      )
    )

    val transactionList =
      serverDatabase.accountToTransactionsMap.getValue(TO_ACCOUNT_ID)

    transactionList shouldContainSingleComplementOf manuallyAddedTransaction

    assertSoftly(transactionList.first()) {
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

  context("with non-split transfer") {
    setUpLocalDatabase {
      addTransactions(transactionTransferNonSplitSource)
    }

    test("create from transfer") {
      actionApplier.applyActions(
        TransactionAction.CreateComplement(
          fromTransaction = transactionAddedFromTransfer,
          toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
        )
      )

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
        deleted.shouldBeFalse()
        accountId shouldBe TO_ACCOUNT_ID.plainUuid
      }
    }

    test("create from transfer without duplicating network calls") {
      actionApplier.applyActions(
        TransactionAction.CreateComplement(
          transactionAddedFromTransfer,
          toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
        )
      )

      val transactionList =
        serverDatabase.accountToTransactionsMap.getValue(TO_ACCOUNT_ID)
      transactionList shouldContainSingleComplementOf transactionAddedFromTransfer

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
  }

  test("create from split transfer") {
    setUpLocalDatabase {
      addTransactions(transactionTransferSplitSource)
    }
    actionApplier.applyActions(
      TransactionAction.CreateComplement(
        transactionAddedFromTransfer,
        toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
      )
    )

    val transactionList = serverDatabase.accountToTransactionsMap.getValue(TO_ACCOUNT_ID)

    transactionList shouldContainSingleComplementOf transactionAddedFromTransfer

    assertSoftly(transactionList.first()) {
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

  test("create from split transfer: recurring transaction") {
    val transactionAddedFromTransferWithLongId = transactionAddedFromTransfer.copy(
      id = transactionAddedFromTransfer.id + "_st_1_2020-06-20"
    )
    setUpLocalDatabase {
      addTransactions(
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
    }

    actionApplier.applyActions(
      TransactionAction.CreateComplement(
        transactionAddedFromTransferWithLongId,
        toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
      )
    )

    val transactionList = serverDatabase.accountToTransactionsMap.getValue(TO_ACCOUNT_ID)

    transactionList shouldContainSingleComplementOf transactionAddedFromTransfer

    assertSoftly(transactionList.first()) {
      amount shouldBe -transactionAddedFromTransferWithLongId.amount
      importId shouldBe "splity:${-transactionAddedFromTransferWithLongId.amount}:${transactionAddedFromTransferWithLongId.date}:1"
      date shouldBe transactionAddedFromTransferWithLongId.date
      payeeName shouldBe transactionTransferSplitSource.payeeName
      memo shouldBe transactionTransferSplitSource.memo + " • Out of $30.00, you paid 33.3%"
      cleared shouldBe TransactionDetail.ClearedEnum.CLEARED
      approved.shouldBeFalse()
      accountId shouldBe TO_ACCOUNT_ID.plainUuid
    }
  }

  test("update approved") {
    setUpServerDatabase {
      setUpBudgetsAndAccounts(fromBudget to listOf(FROM_ACCOUNT))
      addTransactionsForAccount(
        FROM_ACCOUNT_ID,
        listOf(manuallyAddedTransaction.toApiTransaction())
      )
    }

    actionApplier.applyActions(
      TransactionAction.UpdateComplement(
        fromTransaction = manuallyAddedTransactionComplement,
        complement = manuallyAddedTransaction,
        updateFields = setOf(UpdateField.CLEAR),
      )
    )

    serverDatabase.getTransactionById(manuallyAddedTransaction.id) shouldBe
      manuallyAddedTransaction.copy(cleared = TransactionDetail.ClearedEnum.CLEARED)
        .toApiTransaction()
  }
})
