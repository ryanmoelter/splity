package co.moelten.splity

import com.youneedabudget.client.models.SubTransaction
import com.youneedabudget.client.models.TransactionDetail
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import java.util.UUID.randomUUID

val FROM_BUDGET_ID = randomUUID()
val FROM_ACCOUNT_ID = randomUUID()
const val FROM_ACCOUNT_NAME = "Split - From"

val TO_BUDGET_ID = randomUUID()
val TO_ACCOUNT_ID = randomUUID()
const val TO_ACCOUNT_NAME = "Split - To"

val FROM_TRANSFER_SOURCE_ACCOUNT_ID = randomUUID()
const val FROM_TRANSFER_SOURCE_ACCOUNT_NAME = "Checking"

val manuallyAddedTransaction = TransactionDetail(
  id = "manuallyAddedTransaction",
  date = LocalDate.of(2020, Month.FEBRUARY, 6),
  amount = 350000,
  cleared = TransactionDetail.ClearedEnum.UNCLEARED,
  approved = true,
  accountId = FROM_ACCOUNT_ID,
  deleted = false,
  accountName = FROM_ACCOUNT_NAME,
  memo = "Manually added transaction",
  flagColor = null,
  payeeId = randomUUID(),
  payeeName = "Target",
  categoryId = randomUUID(),
  categoryName = "Household goods",
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = null,
  subtransactions = emptyList()
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
  memo = "Manually added transaction",
  flagColor = null,
  payeeId = randomUUID(),
  payeeName = "Target",
  categoryId = randomUUID(),
  categoryName = "Household goods",
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = "manuallyAddedTransaction",
  subtransactions = emptyList()
)

val transactionAddedFromTransfer = TransactionDetail(
  id = "transactionAddedFromTransfer",
  date = LocalDate.of(2020, Month.FEBRUARY, 7),
  amount = -10000,
  cleared = TransactionDetail.ClearedEnum.UNCLEARED,
  approved = true,
  accountId = FROM_ACCOUNT_ID,
  deleted = false,
  accountName = FROM_ACCOUNT_NAME,
  memo = "Transaction added by split",
  flagColor = null,
  payeeId = randomUUID(),
  payeeName = "Transfer : $FROM_TRANSFER_SOURCE_ACCOUNT_NAME",
  categoryId = null,
  categoryName = null,
  transferAccountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = null,
  subtransactions = emptyList()
)

val transactionTransferSplitSource = TransactionDetail(
  id = "transactionTransferSplitSource",
  date = LocalDate.of(2020, Month.FEBRUARY, 7),
  amount = -30000,
  cleared = TransactionDetail.ClearedEnum.CLEARED,
  approved = true,
  accountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID,
  deleted = false,
  accountName = FROM_TRANSFER_SOURCE_ACCOUNT_NAME,
  memo = "Split transaction source",
  flagColor = null,
  payeeId = randomUUID(),
  payeeName = "Hello Alfred",
  categoryId = randomUUID(),
  categoryName = "Split SubCategory",
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = randomUUID().toString(),
  importId = null,
  subtransactions = listOf(
    SubTransaction(
      id = "splitNonTransferSubtransaction",
      transactionId = "transactionTransferSplitSource",
      amount = -20000,
      deleted = false,
      memo = "I'm not the split you're looking for",
      payeeId = randomUUID(),
      payeeName = null,
      categoryId = randomUUID(),
      categoryName = "Household Goods",
      transferAccountId = null,
      transferTransactionId = null
    ),
    SubTransaction(
      id = "splitTransferSubtransaction",
      transactionId = "transactionTransferSplitSource",
      amount = -10000,
      deleted = false,
      memo = "I'm the split you're looking for",
      payeeId = randomUUID(),
      payeeName = "Transfer : $FROM_ACCOUNT_NAME",
      categoryId = null,
      categoryName = null,
      transferAccountId = FROM_ACCOUNT_ID,
      transferTransactionId = "transactionAddedFromTransfer"
    )
  )
)

val transactionTransferNonSplitSource = TransactionDetail(
  id = "transactionTransferNonSplitSource",
  date = LocalDate.of(2020, Month.FEBRUARY, 7),
  amount = -10000,
  cleared = TransactionDetail.ClearedEnum.CLEARED,
  approved = true,
  accountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID,
  deleted = false,
  accountName = FROM_TRANSFER_SOURCE_ACCOUNT_NAME,
  memo = "Transfer transaction source",
  flagColor = null,
  payeeId = randomUUID(),
  payeeName = "Transfer : $FROM_ACCOUNT_NAME",
  categoryId = null,
  categoryName = null,
  transferAccountId = FROM_ACCOUNT_ID,
  transferTransactionId = "transactionAddedFromTransfer",
  matchedTransactionId = randomUUID().toString(),
  importId = null,
  subtransactions = emptyList()
)
