package co.moelten.splity

import co.moelten.splity.database.AccountId
import co.moelten.splity.database.BudgetId
import co.moelten.splity.database.CategoryId
import co.moelten.splity.database.TransactionId
import co.moelten.splity.database.toAccountId
import co.moelten.splity.database.toBudgetId
import com.youneedabudget.client.models.Account
import com.youneedabudget.client.models.BudgetSummary
import com.youneedabudget.client.models.Category
import com.youneedabudget.client.models.CategoryGroupWithCategories
import com.youneedabudget.client.models.SaveTransaction
import com.youneedabudget.client.models.TransactionDetail

data class FakeYnabServerDatabase(
  var budgetToAccountsMap: Map<BudgetId, List<Account>> = mapOf(),
  var budgetToCategoryGroupsMap: Map<BudgetId, List<CategoryGroupWithCategories>> = mapOf(),
  var budgets: List<BudgetSummary> = emptyList(),
  var accountToTransactionsMap: Map<AccountId, List<TransactionDetail>> = mapOf(),
  val setUp: FakeYnabServerDatabase.() -> Unit = { }
) {
  init {
    this.setUp()
  }

  fun getTransactionById(id: TransactionId) =
    accountToTransactionsMap.values.flatten().find { it.id == id.string }

  fun setUpBudgetsAndAccounts(vararg budgetsToAccounts: Pair<BudgetSummary, List<Account>>) {
    budgets = budgetsToAccounts.map { it.first }
    budgetToAccountsMap = budgetsToAccounts.associate { it.first.id.toBudgetId() to it.second }
    accountToTransactionsMap = budgetsToAccounts
      .flatMap { pair -> pair.second }
      .map { account -> account.id.toAccountId() }
      .associateWith { emptyList() }
  }

  fun addTransactionsForAccount(accountId: AccountId, transactions: List<TransactionDetail>) {
    accountToTransactionsMap = accountToTransactionsMap
      .mutateValue(accountId) { list -> list + transactions }
  }

  fun updateTransaction(id: String, transaction: SaveTransaction): TransactionDetail {
    var result: TransactionDetail? = null
    accountToTransactionsMap = accountToTransactionsMap
      .mutateValue(transaction.accountId.toAccountId()) { list ->
        val originalTransaction = list.find { it.id == id }
        val newTransaction = transaction.toNewTransactionDetail(id, originalTransaction)
        result = newTransaction
        list.filter { it.id != id } + newTransaction
      }

    return result!!
  }

  fun getBudgetedAmountForCategory(categoryId: CategoryId) = findCategory(categoryId).budgeted

  fun setBudgetedAmountForCategory(
    categoryId: CategoryId,
    balanceAmount: Long,
    budgetedAmount: Long
  ) {
    findCategory(categoryId)
      .apply {
        balance = balanceAmount
        budgeted = budgetedAmount
      }
  }

  private fun findCategory(categoryId: CategoryId): Category {
    return budgetToCategoryGroupsMap
      .flatMap { (_, categoryGroupList) -> categoryGroupList }
      .flatMap { it.categories }
      .find { it.id == categoryId.plainUuid }!!
  }

  fun setBalanceForAccount(
    accountId: AccountId,
    balanceAmount: Long
  ) {
    budgetToAccountsMap
      .flatMap { (_, accounts) -> accounts }
      .find { it.id == accountId.plainUuid }!!
      .balance = balanceAmount
  }
}

private fun <Key, Value> Map<Key, Value>.mutate(
  action: MutableMap<Key, Value>.() -> Unit
): Map<Key, Value> = this.toMutableMap().apply(action).toMap()

private fun <Key, Value> Map<Key, Value>.mutateValue(
  key: Key,
  action: (Value) -> Value
): Map<Key, Value> = mutate { put(key, action(getValue(key))) }
