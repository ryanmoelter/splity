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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import java.util.UUID.randomUUID

internal class MirrorTransactionsTest {

  lateinit var ynab: FakeYnabClient

  @BeforeEach
  internal fun setUp() {
    setUpDatabase { }
  }

  private fun setUpDatabase(setUp: FakeDatabase.() -> Unit) {
    ynab = FakeYnabClient(FakeDatabase(setUp = setUp))
  }

  @Test
  fun addTransaction_add() {
    val actions = runBlocking {
      createActionsForBothAccounts(
        firstTransactions = listOf(manuallyAddedTransaction),
        secondTransactions = listOf(),
        firstAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
        secondAccountAndBudget = AccountAndBudget(TO_ACCOUNT_ID, TO_BUDGET_ID)
      )
    }

    expectThat(actions) {
      hasSize(1)
      contains(CompleteTransactionAction(
        TransactionAction.Create(manuallyAddedTransaction),
        FROM_ACCOUNT_AND_BUDGET,
        TO_ACCOUNT_AND_BUDGET
      ))
    }
  }

  @Test
  fun addTransaction_ignore_alreadyAdded() {
    val actions = runBlocking {
      createActionsForBothAccounts(
        firstTransactions = listOf(manuallyAddedTransaction),
        secondTransactions = listOf(manuallyAddedTransactionComplement),
        firstAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
        secondAccountAndBudget = AccountAndBudget(TO_ACCOUNT_ID, TO_BUDGET_ID)
      )
    }

    expectThat(actions).isEmpty()
  }

  @Test
  fun addTransaction_ignore_complement() {
    val actions = runBlocking {
      createActionsForBothAccounts(
        firstTransactions = listOf(manuallyAddedTransactionComplement),
        secondTransactions = listOf(manuallyAddedTransaction),
        firstAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
        secondAccountAndBudget = AccountAndBudget(TO_ACCOUNT_ID, TO_BUDGET_ID)
      )
    }

    expectThat(actions) {
      isEmpty()
    }
  }

  @Test
  fun addTransaction_ignore_complement_recurringSplit() {
    val transactionAddedFromTransferWithLongId = transactionAddedFromTransfer.copy(
      id = transactionAddedFromTransfer.id + "_st_1_2020-06-20"
    )
    val transactionAddedFromTransferWithLongIdComplement = transactionAddedFromTransferWithLongId.copy(
      id = randomUUID().toString(),
      amount = -transactionAddedFromTransferWithLongId.amount,
      importId = subtransactionTransferSplitSource.id,
      cleared = TransactionDetail.ClearedEnum.CLEARED,
      approved = false
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
    val actions = runBlocking {
      createActionsForBothAccounts(
        firstTransactions = listOf(transactionAddedFromTransferWithLongId),
        secondTransactions = listOf(transactionAddedFromTransferWithLongIdComplement),
        firstAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
        secondAccountAndBudget = AccountAndBudget(TO_ACCOUNT_ID, TO_BUDGET_ID)
      )
    }

    expectThat(actions) {
      isEmpty()
    }
  }

  @Test
  fun addTransaction_ignore_alreadyAdded_noImportId() {
    val manuallyAddedTransactionComplementWithoutImportId = manuallyAddedTransactionComplement.copy(
      importId = null,
      memo = "I'm a different memo"
    )
    val actions = runBlocking {
      createActionsForBothAccounts(
        firstTransactions = listOf(manuallyAddedTransaction),
        secondTransactions = listOf(manuallyAddedTransactionComplementWithoutImportId),
        firstAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
        secondAccountAndBudget = TO_ACCOUNT_AND_BUDGET
      )
    }

    expectThat(actions).isEmpty()
  }

  @Test
  @Disabled
  fun updateTransaction_approved() {
    val manuallyAddedTransactionComplementApproved = manuallyAddedTransactionComplement.copy(approved = true)

    val actions = runBlocking {
      createActionsForBothAccounts(
        firstTransactions = listOf(manuallyAddedTransactionComplementApproved),
        secondTransactions = listOf(manuallyAddedTransaction),
        firstAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
        secondAccountAndBudget = TO_ACCOUNT_AND_BUDGET
      )
    }

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
            fromAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
            toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
          )
        ),
        otherAccountTransactionsCache = OtherAccountTransactionsCache(ynab)
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
            fromAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
            toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
          )
        ),
        otherAccountTransactionsCache = OtherAccountTransactionsCache(ynab)
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
    setUpDatabase {
      accountToTransactionsMap = mapOf(FROM_TRANSFER_SOURCE_ACCOUNT_ID to listOf(transactionTransferNonSplitSource))
    }
    runBlocking {
      applyActions(
        ynab,
        listOf(
          CompleteTransactionAction(
            transactionAction = TransactionAction.Create(transactionAddedFromTransfer),
            fromAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
            toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
          )
        ),
        otherAccountTransactionsCache = OtherAccountTransactionsCache(ynab)
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
            fromAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
            toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
          )
        ),
        otherAccountTransactionsCache = OtherAccountTransactionsCache(ynab)
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
            fromAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
            toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
          )
        ),
        otherAccountTransactionsCache = OtherAccountTransactionsCache(ynab)
      )
    }

    val transactionList = ynab.fakeDatabase.accountToTransactionsMap.getValue(TO_ACCOUNT_ID)
    expect {
      that(transactionList).hasSize(1)
      that(transactionList[0].amount).isEqualTo(-transactionAddedFromTransferWithLongId.amount)
      that(transactionList[0].importId).isEqualTo(subtransactionTransferSplitSource.id)
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
            fromAccountAndBudget = FROM_ACCOUNT_AND_BUDGET,
            toAccountAndBudget = TO_ACCOUNT_AND_BUDGET
          )
        ),
        otherAccountTransactionsCache = OtherAccountTransactionsCache(ynab)
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
