package co.moelten.splity

import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.SaveTransaction
import com.youneedabudget.client.models.SaveTransactionsResponse
import com.youneedabudget.client.models.SaveTransactionsResponseData
import com.youneedabudget.client.models.SaveTransactionsWrapper
import com.youneedabudget.client.models.TransactionsResponse
import com.youneedabudget.client.models.TransactionsResponseData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

internal class SplityTest {

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
  fun updateTransaction_approved() {
    val manuallyAddedTransactionComplementApproved = manuallyAddedTransactionComplement.copy(approved = true)

    val actions = createActionsFromOneAccount(
      fromTransactions = listOf(manuallyAddedTransactionComplementApproved),
      toTransactions = listOf(manuallyAddedTransaction)
    )

    expectThat(actions) {
      hasSize(1)
      contains(TransactionAction.Update(manuallyAddedTransactionComplementApproved, manuallyAddedTransaction, listOf(UpdateField.CLEARED)))
    }
  }

  @Test
  fun applyActions_create() {
    val ynab = mockk<YnabClient>()
    coEvery { ynab.transactions.createTransaction(any(), any()) } returns
      SaveTransactionsResponse(SaveTransactionsResponseData(emptyList(), 1L))
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

    val saveTransactionSlot = slot<SaveTransactionsWrapper>()
    coVerify { ynab.transactions.createTransaction(TO_BUDGET_ID.toString(), capture(saveTransactionSlot)) }
    expectThat(saveTransactionSlot.captured.transaction).isEqualTo(SaveTransaction(
      accountId = TO_ACCOUNT_ID,
      date = manuallyAddedTransaction.date,
      amount = -manuallyAddedTransaction.amount,
      payeeId = null,
      payeeName = manuallyAddedTransaction.payeeName,
      categoryId = null,
      memo = manuallyAddedTransaction.memo,
      cleared = SaveTransaction.ClearedEnum.CLEARED,
      approved = false,
      flagColor = null,
      importId = manuallyAddedTransaction.id,
      subtransactions = null
    ))
  }

  @Test
  fun applyActions_create_fromTransfer() {
    val ynab = mockk<YnabClient>()
    coEvery { ynab.transactions.createTransaction(any(), any()) } returns
      SaveTransactionsResponse(SaveTransactionsResponseData(emptyList(), 1L))
    coEvery { ynab.transactions.getTransactionsByAccount(any(), any(), any(), any(), any()) } returns
      TransactionsResponse(TransactionsResponseData(
        listOf(transactionTransferNonSplitSource),
        1L
      ))
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

    val saveTransactionSlot = slot<SaveTransactionsWrapper>()
    coVerify { ynab.transactions.createTransaction(TO_BUDGET_ID.toString(), capture(saveTransactionSlot)) }
    expectThat(saveTransactionSlot.captured.transaction).isEqualTo(SaveTransaction(
      accountId = TO_ACCOUNT_ID,
      date = transactionAddedFromTransfer.date,
      amount = -transactionAddedFromTransfer.amount,
      payeeId = null,
      payeeName = "Chicken Butt",
      categoryId = null,
      memo = transactionTransferNonSplitSource.memo,
      cleared = SaveTransaction.ClearedEnum.CLEARED,
      approved = false,
      flagColor = null,
      importId = transactionAddedFromTransfer.id,
      subtransactions = null
    ))
  }

  @Test
  fun applyActions_create_fromTransfer_withoutDuplicatingNetworkCalls() {
    val ynab = mockk<YnabClient>()
    coEvery { ynab.transactions.createTransaction(any(), any()) } returns
      SaveTransactionsResponse(SaveTransactionsResponseData(emptyList(), 1L))
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

    val saveTransactionSlot = slot<SaveTransactionsWrapper>()
    coVerify { ynab.transactions.createTransaction(TO_BUDGET_ID.toString(), capture(saveTransactionSlot)) }
    expectThat(saveTransactionSlot.captured.transaction).isEqualTo(SaveTransaction(
      accountId = TO_ACCOUNT_ID,
      date = transactionAddedFromTransfer.date,
      amount = -transactionAddedFromTransfer.amount,
      payeeId = null,
      payeeName = "Chicken Butt",
      categoryId = null,
      memo = transactionTransferNonSplitSource.memo,
      cleared = SaveTransaction.ClearedEnum.CLEARED,
      approved = false,
      flagColor = null,
      importId = transactionAddedFromTransfer.id,
      subtransactions = null
    ))
  }

  @Test
  fun applyActions_create_fromSplitTransfer() {
    val ynab = mockk<YnabClient>()
    coEvery { ynab.transactions.createTransaction(any(), any()) } returns
      SaveTransactionsResponse(SaveTransactionsResponseData(emptyList(), 1L))
    coEvery { ynab.transactions.getTransactionsByAccount(any(), any(), any(), any(), any()) } returns
      TransactionsResponse(TransactionsResponseData(
        listOf(transactionTransferSplitSource),
        1L
      ))
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

    val saveTransactionSlot = slot<SaveTransactionsWrapper>()
    coVerify { ynab.transactions.createTransaction(TO_BUDGET_ID.toString(), capture(saveTransactionSlot)) }
    expectThat(saveTransactionSlot.captured.transaction).isEqualTo(SaveTransaction(
      accountId = TO_ACCOUNT_ID,
      date = transactionAddedFromTransfer.date,
      amount = -transactionAddedFromTransfer.amount,
      payeeId = null,
      payeeName = transactionTransferSplitSource.payeeName,
      categoryId = null,
      memo = transactionTransferSplitSource.memo,
      cleared = SaveTransaction.ClearedEnum.CLEARED,
      approved = false,
      flagColor = null,
      importId = transactionAddedFromTransfer.id,
      subtransactions = null
    ))
  }
}
