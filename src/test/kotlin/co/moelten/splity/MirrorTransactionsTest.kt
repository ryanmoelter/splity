package co.moelten.splity

import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.SaveTransaction
import com.youneedabudget.client.models.SaveTransactionWrapper
import com.youneedabudget.client.models.TransactionDetail
import com.youneedabudget.client.models.TransactionResponse
import com.youneedabudget.client.models.TransactionResponseData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse

internal class MirrorTransactionsTest {

  lateinit var ynab: FakeYnabClient

  private fun setUpDatabase(setUp: FakeDatabase.() -> Unit) {
    ynab = FakeYnabClient(FakeDatabase(setUp = setUp))
  }

  @Test
  fun addTransaction_add() {
    val actions = createActionsFromOneAccount(
      fromTransactions = listOf(manuallyAddedTransaction),
      toTransactions = listOf()
    )

    expectThat(actions) {
      hasSize(1)
      contains(TransactionAction.Create(manuallyAddedTransaction))
    }
  }

  @Test
  fun addTransaction_ignore_alreadyAdded() {
    val actions = createActionsFromOneAccount(
      fromTransactions = listOf(manuallyAddedTransaction),
      toTransactions = listOf(manuallyAddedTransactionComplement)
    )

    expectThat(actions).isEmpty()
  }

  @Test
  fun addTransaction_ignore_complement() {
    val actions = createActionsFromOneAccount(
      fromTransactions = listOf(manuallyAddedTransactionComplement),
      toTransactions = listOf(manuallyAddedTransaction)
    )

    expectThat(actions) {
      isEmpty()
    }
  }

  // TODO: fun addTransaction_ignore_matched()

  @Test
  @Disabled
  fun updateTransaction_approved() {
    val manuallyAddedTransactionComplementApproved = manuallyAddedTransactionComplement.copy(approved = true)

    val actions = createActionsFromOneAccount(
      fromTransactions = listOf(manuallyAddedTransactionComplementApproved),
      toTransactions = listOf(manuallyAddedTransaction)
    )

    expectThat(actions) {
      hasSize(1)
      contains(TransactionAction.Update(manuallyAddedTransactionComplementApproved, manuallyAddedTransaction, setOf(UpdateField.CLEAR)))
    }
  }

  @Test
  fun applyActions_create() {
    setUpDatabase { }

    runBlocking {
      applyActions(
        ynab,
        listOf(
          CompleteTransactionAction(
            transactionAction = TransactionAction.Create(manuallyAddedTransaction),
            fromAccountAndBudget = AccountAndBudget(FROM_ACCOUNT_ID, FROM_BUDGET_ID),
            toAccountAndBudget = AccountAndBudget(TO_ACCOUNT_ID, TO_BUDGET_ID)
          )
        )
      )
    }

    val transactionList = ynab.fakeDatabase.accountToTransactionsMap.getValue(TO_ACCOUNT_ID)
    expect {
      that(transactionList).hasSize(1)
      that(transactionList[0].amount).isEqualTo(-manuallyAddedTransaction.amount)
      that(transactionList[0].importId).isEqualTo(manuallyAddedTransaction.id)
      that(transactionList[0].date).isEqualTo(manuallyAddedTransaction.date)
      that(transactionList[0].payeeName).isEqualTo(manuallyAddedTransaction.payeeName)
      that(transactionList[0].memo).isEqualTo(manuallyAddedTransaction.memo)
      that(transactionList[0].cleared).isEqualTo(TransactionDetail.ClearedEnum.CLEARED)
      that(transactionList[0].approved).isFalse()
      that(transactionList[0].accountId).isEqualTo(TO_ACCOUNT_ID)
    }
  }

  @Test
  fun applyActions_create_fromTransfer() {
    setUpDatabase {
      accountToTransactionsMap = mapOf(
        FROM_TRANSFER_SOURCE_ACCOUNT_ID to listOf(transactionTransferNonSplitSource)
      )
    }

    runBlocking {
      applyActions(
        ynab,
        listOf(
          CompleteTransactionAction(
            transactionAction = TransactionAction.Create(transactionAddedFromTransfer),
            fromAccountAndBudget = AccountAndBudget(FROM_ACCOUNT_ID, FROM_BUDGET_ID),
            toAccountAndBudget = AccountAndBudget(TO_ACCOUNT_ID, TO_BUDGET_ID)
          )
        )
      )
    }

    val transactionList = ynab.fakeDatabase.accountToTransactionsMap.getValue(TO_ACCOUNT_ID)
    expect {
      that(transactionList).hasSize(1)
      that(transactionList[0].amount).isEqualTo(-transactionAddedFromTransfer.amount)
      that(transactionList[0].importId).isEqualTo(transactionAddedFromTransfer.id)
      that(transactionList[0].date).isEqualTo(transactionAddedFromTransfer.date)
      that(transactionList[0].payeeName).isEqualTo("Chicken Butt")
      that(transactionList[0].memo).isEqualTo(transactionTransferNonSplitSource.memo)
      that(transactionList[0].cleared).isEqualTo(TransactionDetail.ClearedEnum.CLEARED)
      that(transactionList[0].approved).isFalse()
      that(transactionList[0].accountId).isEqualTo(TO_ACCOUNT_ID)
    }
  }

  @Test
  fun applyActions_create_fromTransfer_withoutDuplicatingNetworkCalls() {
    setUpDatabase { }
    runBlocking {
      applyActions(
        ynab,
        listOf(
          CompleteTransactionAction(
            transactionAction = TransactionAction.Create(transactionAddedFromTransfer),
            fromAccountAndBudget = AccountAndBudget(FROM_ACCOUNT_ID, FROM_BUDGET_ID),
            toAccountAndBudget = AccountAndBudget(TO_ACCOUNT_ID, TO_BUDGET_ID)
          )
        ),
        mutableMapOf(
          FROM_TRANSFER_SOURCE_ACCOUNT_ID to listOf(transactionTransferNonSplitSource)
        )
      )
    }

    val transactionList = ynab.fakeDatabase.accountToTransactionsMap.getValue(TO_ACCOUNT_ID)
    expect {
      that(transactionList).hasSize(1)
      that(transactionList[0].amount).isEqualTo(-transactionAddedFromTransfer.amount)
      that(transactionList[0].importId).isEqualTo(transactionAddedFromTransfer.id)
      that(transactionList[0].date).isEqualTo(transactionAddedFromTransfer.date)
      that(transactionList[0].payeeName).isEqualTo("Chicken Butt")
      that(transactionList[0].memo).isEqualTo(transactionTransferNonSplitSource.memo)
      that(transactionList[0].cleared).isEqualTo(TransactionDetail.ClearedEnum.CLEARED)
      that(transactionList[0].approved).isFalse()
      that(transactionList[0].accountId).isEqualTo(TO_ACCOUNT_ID)
    }
  }

  @Test
  fun applyActions_create_fromSplitTransfer() {
    setUpDatabase {
      accountToTransactionsMap = mapOf(
        FROM_TRANSFER_SOURCE_ACCOUNT_ID to listOf(transactionTransferSplitSource)
      )
    }
    runBlocking {
      applyActions(
        ynab,
        listOf(
          CompleteTransactionAction(
            transactionAction = TransactionAction.Create(transactionAddedFromTransfer),
            fromAccountAndBudget = AccountAndBudget(FROM_ACCOUNT_ID, FROM_BUDGET_ID),
            toAccountAndBudget = AccountAndBudget(TO_ACCOUNT_ID, TO_BUDGET_ID)
          )
        )
      )
    }

    val transactionList = ynab.fakeDatabase.accountToTransactionsMap.getValue(TO_ACCOUNT_ID)
    expect {
      that(transactionList).hasSize(1)
      that(transactionList[0].amount).isEqualTo(-transactionAddedFromTransfer.amount)
      that(transactionList[0].importId).isEqualTo(transactionAddedFromTransfer.id)
      that(transactionList[0].date).isEqualTo(transactionAddedFromTransfer.date)
      that(transactionList[0].payeeName).isEqualTo(transactionTransferSplitSource.payeeName)
      that(transactionList[0].memo).isEqualTo(transactionTransferSplitSource.memo)
      that(transactionList[0].cleared).isEqualTo(TransactionDetail.ClearedEnum.CLEARED)
      that(transactionList[0].approved).isFalse()
      that(transactionList[0].accountId).isEqualTo(TO_ACCOUNT_ID)
    }
  }

  @Test
  fun applyActions_create_fromSplitTransfer_recurringTransaction() {
    val transactionAddedFromTransferWithLongId = transactionAddedFromTransfer.copy(
      id = transactionAddedFromTransfer.id + "_st_1_2020-06-20"
    )
    setUpDatabase {
      accountToTransactionsMap = mapOf(
        FROM_TRANSFER_SOURCE_ACCOUNT_ID to listOf(transactionTransferSplitSource.copy(
          subtransactions = listOf(
            subtransactionNonTransferSplitSource,
            subtransactionTransferSplitSource.copy(
              transferTransactionId = subtransactionTransferSplitSource.transferTransactionId + "_st_1_2020-06-20"
            )
          )
        ))
      )
    }
    runBlocking {
      applyActions(
        ynab,
        listOf(
          CompleteTransactionAction(
            transactionAction = TransactionAction.Create(transactionAddedFromTransferWithLongId),
            fromAccountAndBudget = AccountAndBudget(FROM_ACCOUNT_ID, FROM_BUDGET_ID),
            toAccountAndBudget = AccountAndBudget(TO_ACCOUNT_ID, TO_BUDGET_ID)
          )
        )
      )
    }

    val transactionList = ynab.fakeDatabase.accountToTransactionsMap.getValue(TO_ACCOUNT_ID)
    expect {
      that(transactionList).hasSize(1)
      that(transactionList[0].amount).isEqualTo(-transactionAddedFromTransferWithLongId.amount)
      that(transactionList[0].importId).isEqualTo(transactionAddedFromTransferWithLongId.id)
      that(transactionList[0].date).isEqualTo(transactionAddedFromTransferWithLongId.date)
      that(transactionList[0].payeeName).isEqualTo(transactionTransferSplitSource.payeeName)
      that(transactionList[0].memo).isEqualTo(transactionTransferSplitSource.memo)
      that(transactionList[0].cleared).isEqualTo(TransactionDetail.ClearedEnum.CLEARED)
      that(transactionList[0].approved).isFalse()
      that(transactionList[0].accountId).isEqualTo(TO_ACCOUNT_ID)
    }
  }

  @Test
  fun applyActions_update_approved() {
    val ynab = mockk<YnabClient>()
    coEvery { ynab.transactions.updateTransaction(any(), any(), any()) } returns
      TransactionResponse(TransactionResponseData(mockk()))
    runBlocking {
      applyActions(
        ynab,
        listOf(
          CompleteTransactionAction(
            transactionAction = TransactionAction.Update(
              fromTransaction = manuallyAddedTransactionComplement.copy(approved = true),
              toTransaction = manuallyAddedTransaction,
              updateFields = setOf(UpdateField.CLEAR)
            ),
            fromAccountAndBudget = AccountAndBudget(FROM_ACCOUNT_ID, FROM_BUDGET_ID),
            toAccountAndBudget = AccountAndBudget(TO_ACCOUNT_ID, TO_BUDGET_ID)
          )
        )
      )
    }

    val saveTransactionSlot = slot<SaveTransactionWrapper>()
    coVerify { ynab.transactions.updateTransaction(TO_BUDGET_ID.toString(), manuallyAddedTransaction.id, capture(saveTransactionSlot)) }
    expectThat(saveTransactionSlot.captured.transaction).isEqualTo(SaveTransaction(
      accountId = manuallyAddedTransaction.accountId,
      date = manuallyAddedTransaction.date,
      amount = manuallyAddedTransaction.amount,
      payeeId = manuallyAddedTransaction.payeeId,
      payeeName = null,
      categoryId = manuallyAddedTransaction.categoryId,
      memo = manuallyAddedTransaction.memo,
      cleared = SaveTransaction.ClearedEnum.CLEARED,
      approved = manuallyAddedTransaction.approved,
      flagColor = manuallyAddedTransaction.flagColor?.toSaveTransactionFlagColorEnum(),
      importId = manuallyAddedTransaction.importId,
      subtransactions = null
    ))
  }
}
