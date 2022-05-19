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
import com.youneedabudget.client.models.TransactionDetail
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import java.util.UUID

val TRANSACTION_ADDED_FROM_TRANSFER_ID = "transactionAddedFromTransferLength36".toTransactionId()
const val NO_SERVER_KNOWLEDGE = 0L
const val FIRST_SYNC_SERVER_KNOWLEDGE = 10L
const val SECOND_SYNC_SERVER_KNOWLEDGE = 20L

fun manuallyAddedTransaction(processedState: ProcessedState = CREATED) = PublicTransactionDetail(
  id = "manuallyAddedTransaction".toTransactionId(),
  date = LocalDate.of(2020, Month.FEBRUARY, 6),
  amount = 350000,
  cleared = TransactionDetail.ClearedEnum.UNCLEARED,
  approved = true,
  accountId = FROM_ACCOUNT_ID,
  accountName = FROM_ACCOUNT_NAME,
  memo = "Manually added transaction",
  flagColor = null,
  payeeId = "0ec9431c-29cd-45cc-982b-5755e148b4ee".toPayeeId(),
  payeeName = "Target",
  categoryId = "12ecf80a-a918-45c1-9c08-e4217da4bebc".toCategoryId(),
  categoryName = "Household goods",
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = null,
  subTransactions = emptyList(),
  budgetId = FROM_BUDGET_ID,
  processedState = processedState,
)

fun manuallyAddedTransactionComplement(
  processedState: ProcessedState = CREATED
) = PublicTransactionDetail(
  id = "manuallyAddedTransactionComplement".toTransactionId(),
  date = LocalDate.of(2020, Month.FEBRUARY, 6),
  amount = -350000,
  cleared = TransactionDetail.ClearedEnum.CLEARED,
  approved = false,
  accountId = TO_ACCOUNT_ID,
  accountName = TO_ACCOUNT_NAME,
  memo = "Manually added transaction",
  flagColor = null,
  payeeId = "8b539d7c-54c4-46a7-a855-0f359bd768b3".toPayeeId(),
  payeeName = "Target",
  categoryId = "bf85d191-a91d-4bf6-b9b5-ab8b702eb67b".toCategoryId(),
  categoryName = "Household goods",
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = "manuallyAddedTransaction",
  subTransactions = emptyList(),
  budgetId = TO_BUDGET_ID,
  processedState = processedState
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
  payeeId = "a2de57ec-be16-4e49-9556-349721381d06".toPayeeId(),
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
  payeeId = "0ff89c33-a650-4769-91c9-b24090487b31".toPayeeId(),
  payeeName = null,
  categoryId = "a1bdc1de-a4bf-4f05-89e4-b178131060f8".toCategoryId(),
  categoryName = "Household Goods",
  transferAccountId = null,
  transferTransactionId = null,
  accountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID,
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
  payeeId = "09c22438-de08-43b2-a9cc-08c7fc6f6e10".toPayeeId(),
  payeeName = "Transfer : $FROM_ACCOUNT_NAME",
  categoryId = null,
  categoryName = null,
  transferAccountId = FROM_ACCOUNT_ID,
  transferTransactionId = TRANSACTION_ADDED_FROM_TRANSFER_ID,
  accountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID,
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
  payeeId = "7b3df9bf-0cb8-4317-8527-9d73fa968e5b".toPayeeId(),
  payeeName = "Hello Alfred",
  categoryId = "249a671c-5c7b-4abf-b355-8b4a72012091".toCategoryId(),
  categoryName = "Split SubCategory",
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = "5cbadc12-9b06-4c4a-8c7c-d739842059e4".toTransactionId(),
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
  payeeId = "9f7f45ae-d75d-4a72-a312-9a6afec5e3c0".toPayeeId(),
  payeeName = "Transfer : $FROM_ACCOUNT_NAME",
  categoryId = null,
  categoryName = null,
  transferAccountId = FROM_ACCOUNT_ID,
  transferTransactionId = TRANSACTION_ADDED_FROM_TRANSFER_ID,
  matchedTransactionId = "89987609-d7ca-4698-a4f2-a830b80873ec".toTransactionId(),
  importId = null,
  budgetId = FROM_BUDGET_ID,
  subTransactions = emptyList(),
  processedState = processedState,
)

val EXISTING_MIRRORED_TRANSACTION_ID = "existingMirroredTransaction".toTransactionId()
val EXISTING_MIRRORED_TRANSACTION_SOURCE_PARENT_ID = "existingMirroredTransactionSource".toTransactionId()
val EXISTING_MIRRORED_TRANSACTION_SOURCE_SPLIT_TRANSFER_ID =
  "existingMirroredTransactionSourceSplitTransfer".toSubTransactionId()

fun existingMirroredTransaction(processedState: ProcessedState = UP_TO_DATE) = PublicTransactionDetail(
  id = EXISTING_MIRRORED_TRANSACTION_ID,
  date = LocalDate.of(2020, Month.FEBRUARY, 7),
  amount = 25_000,
  cleared = TransactionDetail.ClearedEnum.CLEARED,
  approved = true,
  accountId = FROM_ACCOUNT_ID,
  accountName = FROM_ACCOUNT_NAME,
  memo = "Existing mirrored transaction",
  flagColor = null,
  payeeId = "7e599e78-94e7-4d63-a195-b0dd47ad1060".toPayeeId(),
  payeeName = "Original Pattern Brewery",
  categoryId = "45762a3f-b105-4441-a1ed-32b4897412d3".toCategoryId(),
  categoryName = "Outings",
  transferAccountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID,
  transferTransactionId = EXISTING_MIRRORED_TRANSACTION_SOURCE_SPLIT_TRANSFER_ID.string.toTransactionId(),
  matchedTransactionId = null,
  importId = null,
  subTransactions = emptyList(),
  budgetId = FROM_BUDGET_ID,
  processedState = processedState,
)

fun existingMirroredTransactionComplement(processedState: ProcessedState = UP_TO_DATE) = PublicTransactionDetail(
  id = "existingMirroredTransactionComplement".toTransactionId(),
  date = LocalDate.of(2020, Month.FEBRUARY, 7),
  amount = -25_000,
  cleared = TransactionDetail.ClearedEnum.CLEARED,
  approved = true,
  accountId = TO_ACCOUNT_ID,
  accountName = TO_ACCOUNT_NAME,
  memo = "Existing mirrored transaction • Out of $55.00, you paid $25.00 (something%)",
  flagColor = null,
  payeeId = "781c0469-3f01-4192-a82a-cc3bb1a52240".toPayeeId(),
  payeeName = "Original Pattern Brewery",
  categoryId = "809741f3-b85d-46ca-8abf-5fc613fb4c53".toCategoryId(),
  categoryName = "Dining out",
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = null,
  subTransactions = emptyList(),
  budgetId = TO_BUDGET_ID,
  processedState = processedState,
)

fun existingMirroredTransactionSourceParent(
  processedState: ProcessedState = UP_TO_DATE
) = PublicTransactionDetail(
  id = EXISTING_MIRRORED_TRANSACTION_SOURCE_PARENT_ID,
  date = LocalDate.of(2020, Month.FEBRUARY, 7),
  amount = -55_000,
  cleared = TransactionDetail.ClearedEnum.CLEARED,
  approved = true,
  accountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID,
  accountName = FROM_TRANSFER_SOURCE_ACCOUNT_NAME,
  memo = "Existing mirrored transaction source",
  flagColor = null,
  payeeId = "7e599e78-94e7-4d63-a195-b0dd47ad1060".toPayeeId(),
  payeeName = "Original Pattern Brewery",
  categoryId = "45762a3f-b105-4441-a1ed-32b4897412d3".toCategoryId(),
  categoryName = "Outings",
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = null,
  subTransactions = listOf(
    existingMirroredTransactionSourceSplitTransfer(processedState),
    existingMirroredTransactionSourceSplitCategory(processedState)
  ),
  budgetId = FROM_BUDGET_ID,
  processedState = processedState,
)

fun existingMirroredTransactionSourceSplitTransfer(
  processedState: ProcessedState = UP_TO_DATE
) = PublicSubTransaction(
  id = EXISTING_MIRRORED_TRANSACTION_SOURCE_SPLIT_TRANSFER_ID,
  transactionId = EXISTING_MIRRORED_TRANSACTION_SOURCE_PARENT_ID,
  amount = -25_000,
  memo = "I'm the existing split you're looking for",
  payeeId = "eb226794-2bb1-409f-9fd6-a08bfcd5a164".toPayeeId(),
  payeeName = "Transfer : $FROM_ACCOUNT_NAME",
  categoryId = null,
  categoryName = null,
  transferAccountId = FROM_ACCOUNT_ID,
  transferTransactionId = EXISTING_MIRRORED_TRANSACTION_ID,
  accountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID,
  budgetId = FROM_BUDGET_ID,
  processedState = processedState
)

fun existingMirroredTransactionSourceSplitCategory(
  processedState: ProcessedState = UP_TO_DATE
) = PublicSubTransaction(
  id = "existingMirroredTransactionSourceSplitCategory".toSubTransactionId(),
  transactionId = EXISTING_MIRRORED_TRANSACTION_SOURCE_PARENT_ID,
  amount = -30_000,
  memo = "I'm NOT the existing split you're looking for",
  payeeId = null,
  payeeName = null,
  categoryId = "c956fdc6-0760-4714-b708-896424cd8216".toCategoryId(),
  categoryName = "Outings",
  transferAccountId = null,
  transferTransactionId = null,
  accountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID,
  budgetId = FROM_BUDGET_ID,
  processedState = processedState
)

// -- Normal, non-split transactions ---------------------------------------------------------------
// These should be ignored by splity in most cases

fun unremarkableTransactionInTransferSource(processedState: ProcessedState = CREATED) =
  PublicTransactionDetail(
    id = "unremarkableTransactionInTransferSource".toTransactionId(),
    date = LocalDate.of(2020, Month.FEBRUARY, 7),
    amount = -10100,
    cleared = TransactionDetail.ClearedEnum.CLEARED,
    approved = true,
    accountId = FROM_TRANSFER_SOURCE_ACCOUNT_ID,
    accountName = FROM_TRANSFER_SOURCE_ACCOUNT_NAME,
    memo = "Unremarkable transaction",
    flagColor = null,
    payeeId = "249a671c-5c7b-4abf-b355-8b4a72012091".toPayeeId(),
    payeeName = "Trader Joe's",
    categoryId = "1ab5b286-72e6-422b-8b8b-06d51a5bfdc6".toCategoryId(),
    categoryName = "Household Stuff",
    transferAccountId = null,
    transferTransactionId = null,
    matchedTransactionId = null,
    importId = null,
    subTransactions = emptyList(),
    budgetId = FROM_BUDGET_ID,
    processedState = processedState,
  )

fun String.toUUID() = UUID.fromString(this)
