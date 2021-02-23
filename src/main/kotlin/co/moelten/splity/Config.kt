package co.moelten.splity

import org.threeten.bp.LocalDate

data class Config(
  val ynabToken: String,
  val firstAccount: AccountConfig,
  val secondAccount: AccountConfig,
  val startDate: LocalDate = LocalDate.of(1900, 1, 1)  // LocalDate.MIN causes exceptions
)

data class AccountConfig(
  val budgetName: String,
  val accountName: String
)
