package com.ryanmoelter.splity

import com.ryanmoelter.splity.database.AccountId
import com.ryanmoelter.splity.database.BudgetId
import com.ryanmoelter.splity.database.CategoryId
import com.ryanmoelter.splity.database.TransactionId
import com.ryanmoelter.splity.database.toAccountId
import com.ryanmoelter.splity.database.toBudgetId
import com.ryanmoelter.splity.models.PublicTransactionDetail
import com.ryanmoelter.splity.test.toApiTransaction
import com.ynab.client.models.Account
import com.ynab.client.models.BudgetSummary
import com.ynab.client.models.Category
import com.ynab.client.models.CategoryGroupWithCategories
import com.ynab.client.models.SaveTransaction
import com.ynab.client.models.TransactionDetail

data class FakeYnabServerDatabase(
  var budgetToAccountsMap: Map<BudgetId, List<Account>> = mapOf(),
  var budgetToCategoryGroupsMap: Map<BudgetId, List<CategoryGroupWithCategories>> = mapOf(),
  var budgets: List<BudgetSummary> = emptyList(),
  private var accountToTransactionsMap: Map<AccountId, List<TransactionDetailWithServerKnowledge>> = mapOf(),
  var currentServerKnowledge: Long = 0,
  private val setUp: FakeYnabServerDatabase.() -> Unit = { }
) {
  init {
    this.setUp()
  }

  fun getTransactionById(id: TransactionId) = accountToTransactionsMap.values.flatten()
    .map { it.transactionDetail }
    .find { it.id == id.string }

  fun getAllTransactionsByAccount() = accountToTransactionsMap
    .mapValues { (_, transactionList) ->
      transactionList
        .filterBefore(NO_SERVER_KNOWLEDGE)
        .filter { !it.deleted }
        .map { transaction ->
          transaction.copy(subtransactions = transaction.subtransactions.filter { !it.deleted })
        }
    }
    .filterValues { it.isNotEmpty() }

  fun getTransactionsForAccount(
    accountId: AccountId,
    lastSyncedAt: Long = NO_SERVER_KNOWLEDGE
  ) = accountToTransactionsMap.getValue(accountId)
    .filterBefore(lastSyncedAt)

  fun setUpBudgetsAndAccounts(vararg budgetsToAccounts: Pair<BudgetSummary, List<Account>>) {
    budgets = budgetsToAccounts.map { it.first }
    budgetToAccountsMap = budgetsToAccounts.associate { it.first.id.toBudgetId() to it.second }
    accountToTransactionsMap = budgetsToAccounts
      .flatMap { pair -> pair.second }
      .map { account -> account.id.toAccountId() }
      .associateWith { emptyList() }
  }

  fun addTransactions(vararg transactions: PublicTransactionDetail) {
    val groupedTransactions = transactions.groupBy { it.accountId }
      .mapValues { (_, transactions) ->
        transactions.map { it.toApiTransaction() }
      }
    groupedTransactions.forEach(::addOrUpdateTransactionsForAccount)
  }

  fun addOrUpdateTransactionsForAccount(
    accountId: AccountId,
    transactions: List<TransactionDetail>
  ) {
    val updatedTransactionIds = transactions.map { it.id }.toHashSet()

    accountToTransactionsMap = accountToTransactionsMap
      .mutateOrCreateValue(accountId) { list ->
        list.filter { transaction ->
          !updatedTransactionIds.contains(transaction.transactionDetail.id)
        } + transactions.map { it.withServerKnowledge(currentServerKnowledge) }
      }
    currentServerKnowledge++
  }

  fun updateTransaction(transactionId: String, transaction: SaveTransaction): TransactionDetail {
    var result: TransactionDetail? = null
    accountToTransactionsMap = accountToTransactionsMap
      .mutateOrCreateValue(transaction.accountId.toAccountId()) { list ->
        val originalTransaction = list.find { it.transactionDetail.id == transactionId }!!
        val newTransaction =
          transaction.toNewTransactionDetail(transactionId, originalTransaction.transactionDetail)
        result = newTransaction

        list.filter { it.transactionDetail.id != transactionId } +
          newTransaction.withServerKnowledge(currentServerKnowledge)
      }

    currentServerKnowledge++
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
    currentServerKnowledge++
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

    currentServerKnowledge++
  }

  private fun List<TransactionDetailWithServerKnowledge>.filterBefore(lastSyncedAt: Long) =
    filter { it.updatedAt isAfter lastSyncedAt }
      .map { it.transactionDetail }

  private infix fun Long.isAfter(lastSyncedAt: Long) =
    this >= lastSyncedAt
}

data class TransactionDetailWithServerKnowledge(
  val transactionDetail: TransactionDetail,
  val updatedAt: Long
)

private fun TransactionDetail.withServerKnowledge(updatedAt: Long) =
  TransactionDetailWithServerKnowledge(
    transactionDetail = this,
    updatedAt = updatedAt
  )

private fun <Key, Value> Map<Key, Value>.mutate(
  action: MutableMap<Key, Value>.() -> Unit
): Map<Key, Value> = this.toMutableMap().apply(action).toMap()

private fun <Key, Value> Map<Key, List<Value>>.mutateOrCreateValue(
  key: Key,
  action: (List<Value>) -> List<Value>
): Map<Key, List<Value>> = mutate { put(key, action(get(key) ?: emptyList())) }
