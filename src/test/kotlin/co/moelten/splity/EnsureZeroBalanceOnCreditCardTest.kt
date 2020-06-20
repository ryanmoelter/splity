package co.moelten.splity

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isEqualTo

internal class EnsureZeroBalanceOnCreditCardTest {

  lateinit var ynab: FakeYnabClient

  private fun setUpDatabase(setUp: FakeDatabase.() -> Unit) {
    ynab = FakeYnabClient(FakeDatabase(setUp = setUp))
  }

  @Test
  internal fun noAction() {
    setUpDatabase {
      budgets = listOf(firstBudget)
      budgetToAccountsMap =
        mapOf(firstBudget.id to listOf(firstCreditCardAccountSapphire, firstCreditCardAccountFreedom))
      budgetToCategoryGroupsMap = mapOf(firstBudget.id to listOf(firstCreditCardCategoryGroup))

      setBudgetedAmountForCategory(FIRST_CREDIT_CARD_CATEGORY_SAPPHIRE_ID, balanceAmount = 0, budgetedAmount = 0)
      setBudgetedAmountForCategory(FIRST_CREDIT_CARD_CATEGORY_FREEDOM_ID, balanceAmount = 0, budgetedAmount = 0)
      setBalanceForAccount(FIRST_CREDIT_CARD_ACCOUNT_SAPPHIRE_ID, balanceAmount = 0)
      setBalanceForAccount(FIRST_CREDIT_CARD_ACCOUNT_FREEDOM_ID, balanceAmount = 0)
    }

    runBlocking {
      ensureZeroBalanceOnCreditCardsForOneAccount(ynab, firstAccountConfig, ynab.budgets.getBudgets(true).data)
    }

    expect {
      that(ynab.fakeDatabase.getBalanceForAccount(FIRST_CREDIT_CARD_ACCOUNT_SAPPHIRE_ID)).isEqualTo(0)
      that(ynab.fakeDatabase.getBalanceForAccount(FIRST_CREDIT_CARD_ACCOUNT_FREEDOM_ID)).isEqualTo(0)
      that(ynab.fakeDatabase.getBudgetedAmountForCategory(FIRST_CREDIT_CARD_CATEGORY_SAPPHIRE_ID)).isEqualTo(0)
      that(ynab.fakeDatabase.getBudgetedAmountForCategory(FIRST_CREDIT_CARD_CATEGORY_FREEDOM_ID)).isEqualTo(0)
      that(ynab.fakeDatabase.getBalanceForCategory(FIRST_CREDIT_CARD_CATEGORY_SAPPHIRE_ID)).isEqualTo(0)
      that(ynab.fakeDatabase.getBalanceForCategory(FIRST_CREDIT_CARD_CATEGORY_FREEDOM_ID)).isEqualTo(0)
    }
  }
}
