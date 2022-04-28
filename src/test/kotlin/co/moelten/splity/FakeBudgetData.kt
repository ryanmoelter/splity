package co.moelten.splity

import co.moelten.splity.database.toAccountId
import co.moelten.splity.database.toBudgetId
import com.youneedabudget.client.models.BudgetSummary
import java.util.UUID.randomUUID

val FROM_BUDGET_ID = randomUUID().toBudgetId()
const val FROM_BUDGET_NAME = "First budget"
val FROM_ACCOUNT_ID = randomUUID().toAccountId()
const val FROM_ACCOUNT_NAME = "Split - From"
val FROM_ACCOUNT_AND_BUDGET = AccountAndBudget(FROM_ACCOUNT_ID, FROM_BUDGET_ID)

val TO_BUDGET_ID = randomUUID().toBudgetId()
const val TO_BUDGET_NAME = "Second budget"
val TO_ACCOUNT_ID = randomUUID().toAccountId()
const val TO_ACCOUNT_NAME = "Split - To"
val TO_ACCOUNT_AND_BUDGET = AccountAndBudget(TO_ACCOUNT_ID, TO_BUDGET_ID)

val FROM_TRANSFER_SOURCE_ACCOUNT_ID = randomUUID()!!.toAccountId()
const val FROM_TRANSFER_SOURCE_ACCOUNT_NAME = "Checking"

val firstAccountConfig = AccountConfig(FROM_BUDGET_NAME, FROM_ACCOUNT_NAME)
val secondAccountConfig = AccountConfig(TO_BUDGET_NAME, TO_ACCOUNT_NAME)
val fakeConfig = Config(
  version = "0.0.0-TEST",
  ynabToken = "fakeToken",
  firstAccount = firstAccountConfig,
  secondAccount = secondAccountConfig
)

val firstBudget = BudgetSummary(
  FROM_BUDGET_ID.plainUuid,
  FROM_BUDGET_NAME,
  null,
  null,
  null,
  null,
  null,
  emptyList()
)
