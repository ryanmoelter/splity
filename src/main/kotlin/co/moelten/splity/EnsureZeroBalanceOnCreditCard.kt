package co.moelten.splity

import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.models.BudgetSummaryResponseData
import com.youneedabudget.client.models.PatchMonthCategoryWrapper
import com.youneedabudget.client.models.SaveMonthCategory
import org.threeten.bp.LocalDate

@Suppress("unused")
suspend fun ensureZeroBalanceOnCreditCards(
  ynab: YnabClient,
  config: Config,
  budgetResponse: BudgetSummaryResponseData
) {
  ensureZeroBalanceOnCreditCardsForOneAccount(ynab, config.firstAccount, budgetResponse)
  ensureZeroBalanceOnCreditCardsForOneAccount(ynab, config.secondAccount, budgetResponse)
}

suspend fun ensureZeroBalanceOnCreditCardsForOneAccount(
  ynab: YnabClient,
  accountConfig: AccountConfig,
  budgetResponse: BudgetSummaryResponseData
) {
  val budget = budgetResponse.budgets.find { it.name == accountConfig.budgetName }!!

  val categories = ynab.categories.getCategories(budget.id.toString(), 0).data

  val creditCardCategories = categories.categoryGroups
    .find { it.name == "Credit Card Payments" }!!
    .categories

  val catagoriesAndAccounts = creditCardCategories
    .map { category ->
      category to budget.accounts!!.find { account -> account.name == category.name }!!
    }

  catagoriesAndAccounts.forEach { (category, account) ->
    if (category.balance != -account.balance) {
      println("Action: Update ${category.name}'s balance from ${category.balance} to ${-account.balance} " +
        "(budgeted from ${category.budgeted} to ${category.budgeted - (category.balance + account.balance)})")
      ynab.categories.updateMonthCategory(
        budgetId = budget.id.toString(),
        month = LocalDate.now(),
        categoryId = category.id.toString(),
        data = PatchMonthCategoryWrapper(SaveMonthCategory(category.budgeted - (category.balance + account.balance)))
      )
    } else {
      println("No action: ${category.name}'s balance is ${category.balance}")
    }
  }
}
