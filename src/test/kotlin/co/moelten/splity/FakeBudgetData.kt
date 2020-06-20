package co.moelten.splity

import com.youneedabudget.client.models.BudgetSummary
import java.util.UUID.randomUUID

val FROM_BUDGET_ID = randomUUID()
const val FROM_BUDGET_NAME = "First budget"
val FROM_ACCOUNT_ID = randomUUID()
const val FROM_ACCOUNT_NAME = "Split - From"

val TO_BUDGET_ID = randomUUID()
const val TO_BUDGET_NAME = "Second budget"
val TO_ACCOUNT_ID = randomUUID()
const val TO_ACCOUNT_NAME = "Split - To"

val FROM_TRANSFER_SOURCE_ACCOUNT_ID = randomUUID()
const val FROM_TRANSFER_SOURCE_ACCOUNT_NAME = "Checking"

val firstAccountConfig = AccountConfig(FROM_BUDGET_NAME, FROM_ACCOUNT_NAME)
val secondAccountConfig = AccountConfig(TO_BUDGET_NAME, TO_ACCOUNT_NAME)
val fakeConfig = Config("fakeToken", firstAccountConfig, secondAccountConfig)

val firstBudget = BudgetSummary(
  FROM_BUDGET_ID,
  FROM_BUDGET_NAME,
  null,
  null,
  null,
  null,
  null,
  emptyList()
)
