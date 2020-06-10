package co.moelten.splity

data class Config(
  val ynabToken: String,
  val firstAccount: AccountConfig,
  val secondAccount: AccountConfig
)

data class AccountConfig(
  val budgetName: String,
  val accountName: String
)
