package com.ryanmoelter.splity

import com.ryanmoelter.splity.database.toBudgetId
import io.kotest.core.spec.style.AnnotationSpec
import kotlinx.coroutines.runBlocking
import strikt.api.expect
import strikt.assertions.isEqualTo

class EnsureZeroBalanceOnCreditCardTest : AnnotationSpec() {
  private lateinit var ynab: FakeYnabClient

  private fun setUpDatabase(setUp: FakeYnabServerDatabase.() -> Unit) {
    ynab = FakeYnabClient(FakeYnabServerDatabase(setUp = setUp))
  }

  @Test
  fun noAction_zero() {
    setUpDatabase {
      budgets = listOf(fromBudget)
      budgetToAccountsMap =
        mapOf(
          fromBudget.id.toBudgetId() to
            listOf(
              firstCreditCardAccountSapphire,
              firstCreditCardAccountFreedom,
            ),
        )
      budgetToCategoryGroupsMap =
        mapOf(fromBudget.id.toBudgetId() to listOf(firstCreditCardCategoryGroup))

      setBalanceForAccount(FIRST_CREDIT_CARD_ACCOUNT_SAPPHIRE_ID, balanceAmount = 0)
      setBalanceForAccount(FIRST_CREDIT_CARD_ACCOUNT_FREEDOM_ID, balanceAmount = 0)
      setBudgetedAmountForCategory(
        FIRST_CREDIT_CARD_CATEGORY_SAPPHIRE_ID,
        balanceAmount = 0,
        budgetedAmount = 0,
      )
      setBudgetedAmountForCategory(
        FIRST_CREDIT_CARD_CATEGORY_FREEDOM_ID,
        balanceAmount = 0,
        budgetedAmount = 0,
      )
    }

    runBlocking {
      ensureZeroBalanceOnCreditCardsForOneAccount(
        ynab,
        firstAccountConfig,
        ynab.budgets.getBudgets(true).data,
      )
    }

    expect {
      that(
        ynab.fakeYnabServerDatabase.getBudgetedAmountForCategory(
          FIRST_CREDIT_CARD_CATEGORY_SAPPHIRE_ID,
        ),
      ).isEqualTo(0)
      that(
        ynab.fakeYnabServerDatabase.getBudgetedAmountForCategory(
          FIRST_CREDIT_CARD_CATEGORY_FREEDOM_ID,
        ),
      ).isEqualTo(0)
    }
  }

  @Test
  fun noAction_nonZero() {
    setUpDatabase {
      budgets = listOf(fromBudget)
      budgetToAccountsMap =
        mapOf(
          fromBudget.id.toBudgetId() to
            listOf(
              firstCreditCardAccountSapphire,
              firstCreditCardAccountFreedom,
            ),
        )
      budgetToCategoryGroupsMap =
        mapOf(fromBudget.id.toBudgetId() to listOf(firstCreditCardCategoryGroup))

      setBalanceForAccount(FIRST_CREDIT_CARD_ACCOUNT_SAPPHIRE_ID, balanceAmount = -50)
      setBalanceForAccount(FIRST_CREDIT_CARD_ACCOUNT_FREEDOM_ID, balanceAmount = -100)
      setBudgetedAmountForCategory(
        FIRST_CREDIT_CARD_CATEGORY_SAPPHIRE_ID,
        balanceAmount = 50,
        budgetedAmount = 25,
      )
      setBudgetedAmountForCategory(
        FIRST_CREDIT_CARD_CATEGORY_FREEDOM_ID,
        balanceAmount = 100,
        budgetedAmount = 100,
      )
    }

    runBlocking {
      ensureZeroBalanceOnCreditCardsForOneAccount(
        ynab,
        firstAccountConfig,
        ynab.budgets.getBudgets(true).data,
      )
    }

    expect {
      that(
        ynab.fakeYnabServerDatabase.getBudgetedAmountForCategory(
          FIRST_CREDIT_CARD_CATEGORY_SAPPHIRE_ID,
        ),
      ).isEqualTo(25)
      that(
        ynab.fakeYnabServerDatabase.getBudgetedAmountForCategory(
          FIRST_CREDIT_CARD_CATEGORY_FREEDOM_ID,
        ),
      ).isEqualTo(100)
    }
  }

  @Test
  fun fixMismatchedBalances() {
    setUpDatabase {
      budgets = listOf(fromBudget)
      budgetToAccountsMap =
        mapOf(
          fromBudget.id.toBudgetId() to
            listOf(
              firstCreditCardAccountSapphire,
              firstCreditCardAccountFreedom,
            ),
        )
      budgetToCategoryGroupsMap =
        mapOf(fromBudget.id.toBudgetId() to listOf(firstCreditCardCategoryGroup))

      setBalanceForAccount(
        FIRST_CREDIT_CARD_ACCOUNT_SAPPHIRE_ID,
        balanceAmount = -100,
      )
      setBalanceForAccount(FIRST_CREDIT_CARD_ACCOUNT_FREEDOM_ID, balanceAmount = -25)
      setBudgetedAmountForCategory(
        FIRST_CREDIT_CARD_CATEGORY_SAPPHIRE_ID,
        balanceAmount = 50,
        budgetedAmount = 30,
      )
      setBudgetedAmountForCategory(
        FIRST_CREDIT_CARD_CATEGORY_FREEDOM_ID,
        balanceAmount = 25,
        budgetedAmount = 100,
      )
    }

    runBlocking {
      ensureZeroBalanceOnCreditCardsForOneAccount(
        ynab,
        firstAccountConfig,
        ynab.budgets.getBudgets(true).data,
      )
    }

    expect {
      that(
        ynab.fakeYnabServerDatabase.getBudgetedAmountForCategory(
          FIRST_CREDIT_CARD_CATEGORY_SAPPHIRE_ID,
        ),
      ).isEqualTo(80)
      that(
        ynab.fakeYnabServerDatabase.getBudgetedAmountForCategory(
          FIRST_CREDIT_CARD_CATEGORY_FREEDOM_ID,
        ),
      ).isEqualTo(100)
    }
  }
}
