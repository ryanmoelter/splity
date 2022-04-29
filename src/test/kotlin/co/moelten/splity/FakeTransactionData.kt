package co.moelten.splity

import co.moelten.splity.database.AccountId
import co.moelten.splity.database.BudgetId
import co.moelten.splity.database.ProcessedState
import co.moelten.splity.database.toAccountId
import co.moelten.splity.database.toCategoryId
import co.moelten.splity.database.toPayeeId
import co.moelten.splity.database.toSubTransactionId
import co.moelten.splity.database.toTransactionId
import co.moelten.splity.models.PublicSubTransaction
import co.moelten.splity.models.PublicTransactionDetail
import com.youneedabudget.client.models.SubTransaction
import com.youneedabudget.client.models.TransactionDetail
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import java.util.UUID

const val TRANSACTION_ADDED_FROM_TRANSFER_ID = "transactionAddedFromTransferLength36"

val manuallyAddedTransaction = TransactionDetail(
  id = "manuallyAddedTransaction",
  date = LocalDate.of(2020, Month.FEBRUARY, 6),
  amount = 350000,
  cleared = TransactionDetail.ClearedEnum.UNCLEARED,
  approved = true,
  accountId = FROM_ACCOUNT_ID.plainUuid,
  deleted = false,
  accountName = FROM_ACCOUNT_NAME,
  memo = "Manually added transaction",
  flagColor = null,
  payeeId = UUID.randomUUID(),
  payeeName = "Target",
  categoryId = UUID.randomUUID(),
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
  accountId = TO_ACCOUNT_ID.plainUuid,
  deleted = false,
  accountName = TO_ACCOUNT_NAME,
  memo = "Manually added transaction",
  flagColor = null,
  payeeId = UUID.randomUUID(),
  payeeName = "Target",
  categoryId = UUID.randomUUID(),
  categoryName = "Household goods",
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = "manuallyAddedTransaction",
  subtransactions = emptyList()
)

val transactionAddedFromTransfer = TransactionDetail(
  id = TRANSACTION_ADDED_FROM_TRANSFER_ID,
  date = LocalDate.of(2020, Month.FEBRUARY, 7),
  amount = -10000,
  cleared = TransactionDetail.ClearedEnum.UNCLEARED,
  approved = true,
  accountId = FROM_ACCOUNT_ID.plainUuid,
  deleted = false,
  accountName = FROM_ACCOUNT_NAME,
  memo = "Transaction added by split",
  flagColor = null,
  payeeId = UUID.randomUUID(),
  payeeName = "Transfer : $FROM_TRANSFER_SOURCE_ACCOUNT_NAME",
  categoryId = null,
  categoryName = null,
  transferAccountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID.plainUuid,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = null,
  subtransactions = emptyList()
)

val subTransactionNonTransferSplitSource = SubTransaction(
  id = "splitNonTransferSubTransaction",
  transactionId = "transactionTransferSplitSource",
  amount = -20000,
  deleted = false,
  memo = "I'm not the split you're looking for",
  payeeId = UUID.randomUUID(),
  payeeName = null,
  categoryId = UUID.randomUUID(),
  categoryName = "Household Goods",
  transferAccountId = null,
  transferTransactionId = null
)

val subTransactionTransferSplitSource = SubTransaction(
  id = "splitTransferSubTransaction",
  transactionId = "transactionTransferSplitSource",
  amount = -10000,
  deleted = false,
  memo = "I'm the split you're looking for",
  payeeId = UUID.randomUUID(),
  payeeName = "Transfer : $FROM_ACCOUNT_NAME",
  categoryId = null,
  categoryName = null,
  transferAccountId = FROM_ACCOUNT_ID.plainUuid,
  transferTransactionId = TRANSACTION_ADDED_FROM_TRANSFER_ID
)

val transactionTransferSplitSource = TransactionDetail(
  id = "transactionTransferSplitSource",
  date = LocalDate.of(2020, Month.FEBRUARY, 7),
  amount = -30000,
  cleared = TransactionDetail.ClearedEnum.CLEARED,
  approved = true,
  accountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID.plainUuid,
  deleted = false,
  accountName = FROM_TRANSFER_SOURCE_ACCOUNT_NAME,
  memo = "Split transaction source",
  flagColor = null,
  payeeId = UUID.randomUUID(),
  payeeName = "Hello Alfred",
  categoryId = UUID.randomUUID(),
  categoryName = "Split SubCategory",
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = UUID.randomUUID().toString(),
  importId = null,
  subtransactions = listOf(
    subTransactionNonTransferSplitSource,
    subTransactionTransferSplitSource
  )
)

val transactionTransferNonSplitSource = TransactionDetail(
  id = "transactionTransferNonSplitSource",
  date = LocalDate.of(2020, Month.FEBRUARY, 7),
  amount = -10000,
  cleared = TransactionDetail.ClearedEnum.CLEARED,
  approved = true,
  accountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID.plainUuid,
  deleted = false,
  accountName = FROM_TRANSFER_SOURCE_ACCOUNT_NAME,
  memo = "Transfer transaction source",
  flagColor = null,
  payeeId = UUID.randomUUID(),
  payeeName = "Transfer : $FROM_ACCOUNT_NAME",
  categoryId = null,
  categoryName = null,
  transferAccountId = FROM_ACCOUNT_ID.plainUuid,
  transferTransactionId = TRANSACTION_ADDED_FROM_TRANSFER_ID,
  matchedTransactionId = UUID.randomUUID().toString(),
  importId = null,
  subtransactions = emptyList()
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

// -- Conversions ----------------------------------------------------------------------------------

fun TransactionDetail.toPublicTransactionDetail(
  processedState: ProcessedState,
  budgetId: BudgetId
) =
  PublicTransactionDetail(
    id = id.toTransactionId(),
    date = date,
    amount = amount,
    cleared = cleared,
    approved = approved,
    accountId = accountId.toAccountId(),
    accountName = accountName,
    memo = memo,
    flagColor = flagColor,
    payeeId = payeeId?.toPayeeId(),
    categoryId = categoryId?.toCategoryId(),
    transferAccountId = transferAccountId?.toAccountId(),
    transferTransactionId = transferTransactionId?.toTransactionId(),
    matchedTransactionId = matchedTransactionId?.toTransactionId(),
    importId = importId,
    payeeName = payeeName,
    categoryName = categoryName,
    subTransactions = subtransactions.map {
      it.toPublicSubTransaction(
        processedState,
        budgetId,
        accountId.toAccountId()
      )
    },
    processedState = processedState,
    budgetId = budgetId,
  )

fun SubTransaction.toPublicSubTransaction(
  processedState: ProcessedState,
  budgetId: BudgetId,
  accountId: AccountId
) = PublicSubTransaction(
  id = id.toSubTransactionId(),
  transactionId = transactionId.toTransactionId(),
  amount = amount,
  memo = memo,
  payeeId = payeeId?.toPayeeId(),
  payeeName = payeeName,
  categoryId = categoryId?.toCategoryId(),
  categoryName = categoryName,
  transferAccountId = transferAccountId?.toAccountId(),
  transferTransactionId = transferTransactionId?.toTransactionId(),
  processedState = processedState,
  accountId = accountId,
  budgetId = budgetId,
)
