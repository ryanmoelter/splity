package co.moelten.splity.database

import com.squareup.sqldelight.ColumnAdapter
import java.util.UUID

@JvmInline
value class BudgetId(val id: UUID) {
  override fun toString(): String = id.toString()
}

fun String.toBudgetId() = BudgetId(UUID.fromString(this))

val budgetIdAdapter = object : ColumnAdapter<BudgetId, String> {
  override fun decode(databaseValue: String) = databaseValue.toBudgetId()
  override fun encode(value: BudgetId) = value.toString()
}

@JvmInline
value class AccountId(val id: UUID) {
  override fun toString(): String = id.toString()
}

fun String.toAccountId() = AccountId(UUID.fromString(this))

val accountIdAdapter = object : ColumnAdapter<AccountId, String> {
  override fun decode(databaseValue: String) = databaseValue.toAccountId()
  override fun encode(value: AccountId) = value.toString()
}

@JvmInline
value class TransactionId(val id: UUID) {
  override fun toString(): String = id.toString()
}

fun String.toTransactionId() = TransactionId(UUID.fromString(this))

val transactionIdAdapter = object : ColumnAdapter<TransactionId, String> {
  override fun decode(databaseValue: String) = databaseValue.toTransactionId()
  override fun encode(value: TransactionId) = value.toString()
}

@JvmInline
value class SubTransactionId(val id: UUID) {
  override fun toString(): String = id.toString()
}

fun String.toSubTransactionId() = SubTransactionId(UUID.fromString(this))

val subTransactionIdAdapter = object : ColumnAdapter<SubTransactionId, String> {
  override fun decode(databaseValue: String) = databaseValue.toSubTransactionId()
  override fun encode(value: SubTransactionId) = value.toString()
}

@JvmInline
value class CategoryId(val id: UUID) {
  override fun toString(): String = id.toString()
}

fun String.toCategoryId() = CategoryId(UUID.fromString(this))

val categoryIdAdapter = object : ColumnAdapter<CategoryId, String> {
  override fun decode(databaseValue: String) = databaseValue.toCategoryId()
  override fun encode(value: CategoryId) = value.toString()
}

@JvmInline
value class PayeeId(val id: UUID) {
  override fun toString(): String = id.toString()
}

fun String.toPayeeId() = PayeeId(UUID.fromString(this))

val payeeIdAdapter = object : ColumnAdapter<PayeeId, String> {
  override fun decode(databaseValue: String) = databaseValue.toPayeeId()
  override fun encode(value: PayeeId) = value.toString()
}
