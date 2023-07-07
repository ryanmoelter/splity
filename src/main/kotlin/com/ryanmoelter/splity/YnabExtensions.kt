package com.ryanmoelter.splity

import com.ryanmoelter.splity.database.AccountId
import com.ryanmoelter.splity.database.BudgetId
import com.ryanmoelter.splity.database.PayeeId
import com.ryanmoelter.splity.models.PublicTransactionDetail
import com.ynab.client.models.SaveTransaction
import com.ynab.client.models.TransactionDetail
import kotlin.math.absoluteValue

data class AccountAndBudget(val accountId: AccountId, val accountPayeeId: PayeeId, val budgetId: BudgetId)
data class TransactionDescription(val payeeName: String?, val memo: String, val totalAmount: Long)

val PublicTransactionDetail.transactionDescription
  get() = TransactionDescription(
    payeeName = payeeName,
    memo = memo.orEmpty(),
    totalAmount = amount
  )

/** Convert a YNAB amount (int representing value * 1000) into a $X.XX string, avoiding precision loss */
fun Long.toMoneyString() = "\$${"%,d".format(this / 1000)}.${"%02d".format((this / 10) % 100)}"

/** Calculate this as a percentage of [total], represented as a double from 0-100 */
fun Long.absolutePercentageOf(total: Long) = ((this * 100).toDouble() / total).absoluteValue

/** Convert a percentage (double representing value in percent, i.e. 0-100) into a percentage string */
fun Double.toPercentageString() = "%.1f".format(this) + "%"

fun TransactionDetail.ClearedEnum.toSaveTransactionClearedEnum() = when (this) {
  TransactionDetail.ClearedEnum.CLEARED -> SaveTransaction.ClearedEnum.CLEARED
  TransactionDetail.ClearedEnum.UNCLEARED -> SaveTransaction.ClearedEnum.UNCLEARED
  TransactionDetail.ClearedEnum.RECONCILED -> SaveTransaction.ClearedEnum.RECONCILED
}

fun TransactionDetail.FlagColorEnum.toSaveTransactionFlagColorEnum() = when (this) {
  TransactionDetail.FlagColorEnum.RED -> SaveTransaction.FlagColorEnum.RED
  TransactionDetail.FlagColorEnum.ORANGE -> SaveTransaction.FlagColorEnum.ORANGE
  TransactionDetail.FlagColorEnum.YELLOW -> SaveTransaction.FlagColorEnum.YELLOW
  TransactionDetail.FlagColorEnum.GREEN -> SaveTransaction.FlagColorEnum.GREEN
  TransactionDetail.FlagColorEnum.BLUE -> SaveTransaction.FlagColorEnum.BLUE
  TransactionDetail.FlagColorEnum.PURPLE -> SaveTransaction.FlagColorEnum.PURPLE
  TransactionDetail.FlagColorEnum.NONE -> null  // TODO: Create a SaveTransaction.Flag.NONE?
}
