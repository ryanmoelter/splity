package co.moelten.splity

import com.youneedabudget.client.models.TransactionDetail
import org.junit.jupiter.api.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import java.util.UUID

val FROM_ACCOUNT_ID = UUID.randomUUID()
const val FROM_ACCOUNT_NAME = "Split - From"

val TO_ACCOUNT_ID = UUID.randomUUID()
const val TO_ACCOUNT_NAME = "Split - To"

val FROM_TRANSFER_ACCOUNT_ID = UUID.randomUUID()
const val FROM_TRANSFER_ACCOUNT_NAME = "Transfer : Checking"

val manuallyAddedTransaction = TransactionDetail(
  id = "manuallyAddedTransaction",
  date = LocalDate.of(2020, Month.FEBRUARY, 6),
  amount = 350000,
  cleared = TransactionDetail.ClearedEnum.UNCLEARED,
  approved = true,
  accountId = FROM_ACCOUNT_ID,
  deleted = false,
  accountName = FROM_ACCOUNT_NAME,
  subtransactions = emptyList(),
  memo = "Manually added transaction",
  flagColor = null,
  payeeId = UUID.randomUUID(),
  categoryId = UUID.randomUUID(),
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = null,
  payeeName = "Target",
  categoryName = "Household goods"
)

val manuallyAddedTransactionComplement = TransactionDetail(
  id = "manuallyAddedTransactionComplement",
  date = LocalDate.of(2020, Month.FEBRUARY, 6),
  amount = -350000,
  cleared = TransactionDetail.ClearedEnum.CLEARED,
  approved = false,
  accountId = TO_ACCOUNT_ID,
  deleted = false,
  accountName = TO_ACCOUNT_NAME,
  subtransactions = emptyList(),
  memo = "Manually added transaction",
  flagColor = null,
  payeeId = UUID.randomUUID(),
  categoryId = UUID.randomUUID(),
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = "manuallyAddedTransaction",
  payeeName = "Target",
  categoryName = "Household goods"
)

val transactionAddedFromSplit = TransactionDetail(
  id = "transactionAddedFromSplit",
  date = LocalDate.of(2020, Month.FEBRUARY, 7),
  amount = 10000,
  cleared = TransactionDetail.ClearedEnum.UNCLEARED,
  approved = true,
  accountId = FROM_ACCOUNT_ID,
  deleted = false,
  accountName = FROM_ACCOUNT_NAME,
  subtransactions = emptyList(),
  memo = "Transaction added by split",
  flagColor = null,
  payeeId = UUID.randomUUID(),
  categoryId = null,
  transferAccountId = FROM_TRANSFER_ACCOUNT_ID,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = null,
  payeeName = FROM_TRANSFER_ACCOUNT_NAME,
  categoryName = null
)

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
}
