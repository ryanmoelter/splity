package co.moelten.splity

import co.moelten.splity.database.ProcessedState
import co.moelten.splity.database.ProcessedState.CREATED
import co.moelten.splity.database.ProcessedState.UP_TO_DATE
import co.moelten.splity.database.toCategoryId
import co.moelten.splity.database.toPayeeId
import co.moelten.splity.database.toSubTransactionId
import co.moelten.splity.database.toTransactionId
import co.moelten.splity.models.PublicSubTransaction
import co.moelten.splity.models.PublicTransactionDetail
import co.moelten.splity.test.toPublicTransactionDetail
import com.youneedabudget.client.models.TransactionDetail
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import java.util.UUID

val TRANSACTION_ADDED_FROM_TRANSFER_ID = "transactionAddedFromTransferLength36".toTransactionId()
const val NO_SERVER_KNOWLEDGE = 0
const val FIRST_SERVER_KNOWLEDGE = 10
const val SECOND_SERVER_KNOWLEDGE = 20

val manuallyAddedTransaction = PublicTransactionDetail(
  id = "manuallyAddedTransaction".toTransactionId(),
  date = LocalDate.of(2020, Month.FEBRUARY, 6),
  amount = 350000,
  cleared = TransactionDetail.ClearedEnum.UNCLEARED,
  approved = true,
  accountId = FROM_ACCOUNT_ID,
  accountName = FROM_ACCOUNT_NAME,
  memo = "Manually added transaction",
  flagColor = null,
  payeeId = UUID.randomUUID().toPayeeId(),
  payeeName = "Target",
  categoryId = UUID.randomUUID().toCategoryId(),
  categoryName = "Household goods",
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = null,
  subTransactions = emptyList(),
  budgetId = FROM_BUDGET_ID,
  processedState = CREATED,
)

val manuallyAddedTransactionComplement = PublicTransactionDetail(
  id = "manuallyAddedTransactionComplement".toTransactionId(),
  date = LocalDate.of(2020, Month.FEBRUARY, 6),
  amount = -350000,
  cleared = TransactionDetail.ClearedEnum.CLEARED,
  approved = false,
  accountId = TO_ACCOUNT_ID,
  accountName = TO_ACCOUNT_NAME,
  memo = "Manually added transaction",
  flagColor = null,
  payeeId = UUID.randomUUID().toPayeeId(),
  payeeName = "Target",
  categoryId = UUID.randomUUID().toCategoryId(),
  categoryName = "Household goods",
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = "manuallyAddedTransaction",
  subTransactions = emptyList(),
  budgetId = TO_BUDGET_ID,
  processedState = UP_TO_DATE
)

val transactionAddedFromTransfer = PublicTransactionDetail(
  id = TRANSACTION_ADDED_FROM_TRANSFER_ID,
  date = LocalDate.of(2020, Month.FEBRUARY, 7),
  amount = -10000,
  cleared = TransactionDetail.ClearedEnum.UNCLEARED,
  approved = true,
  accountId = FROM_ACCOUNT_ID,
  accountName = FROM_ACCOUNT_NAME,
  memo = "Transaction added by split",
  flagColor = null,
  payeeId = UUID.randomUUID().toPayeeId(),
  payeeName = "Transfer : $FROM_TRANSFER_SOURCE_ACCOUNT_NAME",
  categoryId = null,
  categoryName = null,
  transferAccountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = null,
  subTransactions = emptyList(),
  budgetId = FROM_BUDGET_ID,
  processedState = CREATED
)

fun subTransactionNonTransferSplitSource(
  processedState: ProcessedState = CREATED
) = PublicSubTransaction(
  id = "splitNonTransferSubTransaction".toSubTransactionId(),
  transactionId = "transactionTransferSplitSource".toTransactionId(),
  amount = -20000,
  memo = "I'm not the split you're looking for",
  payeeId = UUID.randomUUID().toPayeeId(),
  payeeName = null,
  categoryId = UUID.randomUUID().toCategoryId(),
  categoryName = "Household Goods",
  transferAccountId = null,
  transferTransactionId = null,
  accountId = FROM_ACCOUNT_ID,
  budgetId = FROM_BUDGET_ID,
  processedState = processedState
)

fun subTransactionTransferSplitSource(
  processedState: ProcessedState = CREATED
) = PublicSubTransaction(
  id = "splitTransferSubTransaction".toSubTransactionId(),
  transactionId = "transactionTransferSplitSource".toTransactionId(),
  amount = -10000,
  memo = "I'm the split you're looking for",
  payeeId = UUID.randomUUID().toPayeeId(),
  payeeName = "Transfer : $FROM_ACCOUNT_NAME",
  categoryId = null,
  categoryName = null,
  transferAccountId = FROM_ACCOUNT_ID,
  transferTransactionId = TRANSACTION_ADDED_FROM_TRANSFER_ID,
  accountId = FROM_ACCOUNT_ID,
  budgetId = FROM_BUDGET_ID,
  processedState = processedState
)

fun transactionTransferSplitSource(
  processedState: ProcessedState = CREATED
) = PublicTransactionDetail(
  id = "transactionTransferSplitSource".toTransactionId(),
  date = LocalDate.of(2020, Month.FEBRUARY, 7),
  amount = -30000,
  cleared = TransactionDetail.ClearedEnum.CLEARED,
  approved = true,
  accountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID,
  accountName = FROM_TRANSFER_SOURCE_ACCOUNT_NAME,
  memo = "Split transaction source",
  flagColor = null,
  payeeId = UUID.randomUUID().toPayeeId(),
  payeeName = "Hello Alfred",
  categoryId = UUID.randomUUID().toCategoryId(),
  categoryName = "Split SubCategory",
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = UUID.randomUUID().toTransactionId(),
  importId = null,
  subTransactions = listOf(
    subTransactionNonTransferSplitSource(processedState),
    subTransactionTransferSplitSource(processedState)
  ),
  budgetId = FROM_BUDGET_ID,
  processedState = processedState,
)

fun transactionTransferNonSplitSource(
  processedState: ProcessedState = CREATED
) = PublicTransactionDetail(
  id = "transactionTransferNonSplitSource".toTransactionId(),
  date = LocalDate.of(2020, Month.FEBRUARY, 7),
  amount = -10000,
  cleared = TransactionDetail.ClearedEnum.CLEARED,
  approved = true,
  accountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID,
  accountName = FROM_TRANSFER_SOURCE_ACCOUNT_NAME,
  memo = "Transfer transaction source",
  flagColor = null,
  payeeId = UUID.randomUUID().toPayeeId(),
  payeeName = "Transfer : $FROM_ACCOUNT_NAME",
  categoryId = null,
  categoryName = null,
  transferAccountId = FROM_ACCOUNT_ID,
  transferTransactionId = TRANSACTION_ADDED_FROM_TRANSFER_ID,
  matchedTransactionId = UUID.randomUUID().toTransactionId(),
  importId = null,
  budgetId = FROM_BUDGET_ID,
  subTransactions = emptyList(),
  processedState = processedState,
)

// -- Normal, non-split transactions ---------------------------------------------------------------
// These should be ignored by splity in most cases

val unremarkableTransactionInTransferSource = TransactionDetail(
  id = "unremarkableTransactionInTransferSource",
  date = LocalDate.of(2020, Month.FEBRUARY, 7),
  amount = -10100,
  cleared = TransactionDetail.ClearedEnum.CLEARED,
  approved = true,
  accountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID.plainUuid,
  deleted = false,
  accountName = FROM_TRANSFER_SOURCE_ACCOUNT_NAME,
  memo = "Unremarkable transaction",
  flagColor = null,
  payeeId = UUID.randomUUID(),
  payeeName = "Trader Joe's",
  categoryId = UUID.randomUUID(),
  categoryName = "Household Stuff",
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = null,
  subtransactions = emptyList()
)

fun publicUnremarkableTransactionInTransferSource(processedState: ProcessedState) =
  unremarkableTransactionInTransferSource.toPublicTransactionDetail(
    processedState,
    FROM_BUDGET_ID
  )
