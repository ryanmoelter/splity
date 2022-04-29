package co.moelten.splity.models

import co.moelten.splity.database.AccountId
import co.moelten.splity.database.BudgetId
import co.moelten.splity.database.CategoryId
import co.moelten.splity.database.PayeeId
import co.moelten.splity.database.ProcessedState
import co.moelten.splity.database.SubTransactionId
import co.moelten.splity.database.TransactionId
import com.ryanmoelter.ynab.StoredSubTransaction
import com.ryanmoelter.ynab.StoredTransaction
import com.youneedabudget.client.models.TransactionDetail
import org.threeten.bp.LocalDate

data class PublicTransactionDetail(
  val id: TransactionId,
  val date: LocalDate,
  val amount: Long,
  val cleared: TransactionDetail.ClearedEnum,
  val approved: Boolean,
  val accountId: AccountId,
  val accountName: String,
  val memo: String?,
  val flagColor: TransactionDetail.FlagColorEnum?,
  val payeeId: PayeeId?,
  val categoryId: CategoryId?,
  val transferAccountId: AccountId?,
  val transferTransactionId: TransactionId?,
  val matchedTransactionId: TransactionId?,
  val importId: String?,
  val payeeName: String?,
  val categoryName: String?,
  val subTransactions: List<PublicSubTransaction>,
  val processedState: ProcessedState,
  val budgetId: BudgetId,
)

data class PublicSubTransaction(
  val id: SubTransactionId,
  val transactionId: TransactionId,
  val amount: Long,
  val memo: String?,
  val payeeId: PayeeId?,
  val payeeName: String?,
  val categoryId: CategoryId?,
  val categoryName: String?,
  val transferAccountId: AccountId?,
  val transferTransactionId: TransactionId?,
  val processedState: ProcessedState,
  val accountId: AccountId,
  val budgetId: BudgetId,
)

fun StoredTransaction.toPublicTransactionDetail(subTransactions: List<StoredSubTransaction>) =
  PublicTransactionDetail(
    id = id,
    date = date,
    amount = amount,
    cleared = cleared,
    approved = approved,
    accountId = accountId,
    accountName = accountName,
    memo = memo,
    flagColor = flagColor,
    payeeId = payeeId,
    categoryId = categoryId,
    transferAccountId = transferAccountId,
    transferTransactionId = transferTransactionId,
    matchedTransactionId = matchedTransactionId,
    importId = importId,
    payeeName = payeeName,
    categoryName = categoryName,
    subTransactions = subTransactions.map { it.toPublicSubTransaction() },
    processedState = processedState,
    budgetId = budgetId,
  )

fun StoredSubTransaction.toPublicSubTransaction() = PublicSubTransaction(
  id = id,
  transactionId = transactionId,
  amount = amount,
  memo = memo,
  payeeId = payeeId,
  payeeName = payeeName,
  categoryId = categoryId,
  categoryName = categoryName,
  transferAccountId = transferAccountId,
  transferTransactionId = transferTransactionId,
  processedState = processedState,
  accountId = accountId,
  budgetId = budgetId,
)
