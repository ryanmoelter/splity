package com.ryanmoelter.splity

import com.ryanmoelter.splity.database.ProcessedState.UP_TO_DATE
import com.ryanmoelter.splity.database.plus
import com.ryanmoelter.splity.database.toPublicTransactionDetail
import com.ryanmoelter.splity.database.toPublicTransactionDetailList
import com.ryanmoelter.splity.injection.createFakeSplityComponent
import com.ryanmoelter.splity.test.Setup
import com.ryanmoelter.splity.test.addTransactions
import com.ryanmoelter.splity.test.isComplementOf
import com.ryanmoelter.splity.test.shouldContainSingleComplementOf
import com.ryanmoelter.splity.test.shouldHaveAllTransactionsProcessed
import com.ryanmoelter.ynab.database.Database
import com.ynab.client.models.TransactionDetail
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
        manuallyAddedTransaction(),
        toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
      )
    )

    val transactionList =
      serverDatabase.getTransactionsForAccount(TO_ACCOUNT_ID)

    transactionList.toPublicTransactionDetailList(TO_BUDGET_ID, UP_TO_DATE)
      .shouldContainSingleComplementOf(manuallyAddedTransaction())

    assertSoftly(transactionList.first()) {
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

    localDatabase.shouldHaveAllTransactionsProcessed()
  }

  context("with non-split transfer") {
    setUpLocalDatabase {
      addTransactions(transactionTransferNonSplitSource(UP_TO_DATE))
    }

    test("create from transfer") {
      actionApplier.applyActions(
        TransactionAction.CreateComplement(
          fromTransaction = transactionAddedFromTransfer(isFromSplitSource = false),
          toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
        )
      )

      val transactionList =
        serverDatabase.getTransactionsForAccount(TO_ACCOUNT_ID)
      transactionList shouldHaveSize 1
      assertSoftly(transactionList.first()) {
        amount shouldBe -transactionAddedFromTransfer(isFromSplitSource = false).amount
        importId shouldBe "splity:${
          -transactionAddedFromTransfer(isFromSplitSource = false).amount
        }:${
          transactionAddedFromTransfer(isFromSplitSource = false).date
        }:1"
        date shouldBe transactionAddedFromTransfer(isFromSplitSource = false).date
        payeeName shouldBe "Chicken Butt"
        memo shouldBe transactionTransferNonSplitSource().memo + " • Out of $10.00, you paid 100.0%"
        cleared shouldBe TransactionDetail.ClearedEnum.CLEARED
        deleted.shouldBeFalse()
        accountId shouldBe TO_ACCOUNT_ID.plainUuid
      }

      localDatabase.shouldHaveAllTransactionsProcessed()
    }

    test("create from transfer without duplicating network calls") {
      actionApplier.applyActions(
        TransactionAction.CreateComplement(
          transactionAddedFromTransfer(isFromSplitSource = false),
          toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
        )
      )

      val transactionList =
        serverDatabase.getTransactionsForAccount(TO_ACCOUNT_ID)
      transactionList.toPublicTransactionDetailList(TO_BUDGET_ID, UP_TO_DATE)
        .shouldContainSingleComplementOf(transactionAddedFromTransfer(isFromSplitSource = false))

      assertSoftly(transactionList.first()) {
        amount shouldBe -transactionAddedFromTransfer(isFromSplitSource = false).amount
        importId shouldBe "splity:${
          -transactionAddedFromTransfer(isFromSplitSource = false).amount
        }:${
          transactionAddedFromTransfer(isFromSplitSource = false).date
        }:1"
        date shouldBe transactionAddedFromTransfer(isFromSplitSource = false).date
        payeeName shouldBe "Chicken Butt"
        memo shouldBe transactionTransferNonSplitSource().memo + " • Out of $10.00, you paid 100.0%"
        cleared shouldBe TransactionDetail.ClearedEnum.CLEARED
        approved.shouldBeFalse()
        accountId shouldBe TO_ACCOUNT_ID.plainUuid
      }

      localDatabase.shouldHaveAllTransactionsProcessed()
    }
  }

  test("create from split transfer") {
    setUpLocalDatabase {
      addTransactions(transactionTransferSplitSource(UP_TO_DATE))
    }
    actionApplier.applyActions(
      TransactionAction.CreateComplement(
        transactionAddedFromTransfer(isFromSplitSource = true),
        toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
      )
    )

    val transactionList = serverDatabase.getTransactionsForAccount(TO_ACCOUNT_ID)

    transactionList.toPublicTransactionDetailList(TO_BUDGET_ID, UP_TO_DATE)
      .shouldContainSingleComplementOf(transactionAddedFromTransfer(isFromSplitSource = true))

    val complement = transactionList.find { transactionDetail ->
      transactionDetail.toPublicTransactionDetail(TO_BUDGET_ID, UP_TO_DATE)
        .isComplementOf(transactionAddedFromTransfer(isFromSplitSource = true))
    }!!

    assertSoftly(complement) {
      amount shouldBe -transactionAddedFromTransfer(isFromSplitSource = true).amount
      importId shouldBe "splity:${
        -transactionAddedFromTransfer(isFromSplitSource = true).amount
      }:${
        transactionAddedFromTransfer(isFromSplitSource = true).date
      }:1"
      date shouldBe transactionAddedFromTransfer(isFromSplitSource = true).date
      payeeName shouldBe transactionTransferSplitSource().payeeName
      memo shouldBe subTransactionTransferSplitSource().memo + " • Out of $30.00, you paid 33.3%"
      cleared shouldBe TransactionDetail.ClearedEnum.CLEARED
      approved.shouldBeFalse()
      deleted.shouldBeFalse()
      accountId shouldBe TO_ACCOUNT_ID.plainUuid
    }

    localDatabase.shouldHaveAllTransactionsProcessed()
  }

  test("create from split transfer: recurring transaction") {
    val transactionAddedFromTransferWithLongId =
      transactionAddedFromTransfer(isFromSplitSource = true).copy(
        id = transactionAddedFromTransfer(isFromSplitSource = true).id + "_st_1_2020-06-20"
      )
    setUpLocalDatabase {
      addTransactions(
        transactionTransferSplitSource(UP_TO_DATE).copy(
          subTransactions = listOf(
            subTransactionNonTransferSplitSource(UP_TO_DATE),
            subTransactionTransferSplitSource(UP_TO_DATE).copy(
              transferTransactionId = subTransactionTransferSplitSource(UP_TO_DATE)
                .transferTransactionId!! + "_st_1_2020-06-20"
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

    val transactionList = serverDatabase.getTransactionsForAccount(TO_ACCOUNT_ID)

    transactionList.toPublicTransactionDetailList(TO_BUDGET_ID, UP_TO_DATE)
      .shouldContainSingleComplementOf(transactionAddedFromTransfer(isFromSplitSource = true))

    assertSoftly(transactionList.first()) {
      amount shouldBe -transactionAddedFromTransferWithLongId.amount
      importId shouldBe "splity:${-transactionAddedFromTransferWithLongId.amount}:${transactionAddedFromTransferWithLongId.date}:1"
      date shouldBe transactionAddedFromTransferWithLongId.date
      payeeName shouldBe transactionTransferSplitSource().payeeName
      memo shouldBe subTransactionTransferSplitSource().memo + " • Out of $30.00, you paid 33.3%"
      cleared shouldBe TransactionDetail.ClearedEnum.CLEARED
      approved.shouldBeFalse()
      accountId shouldBe TO_ACCOUNT_ID.plainUuid
    }
  }
})
