package co.moelten.splity

import com.youneedabudget.client.YnabClient
import com.youneedabudget.client.apis.AccountsApi
import com.youneedabudget.client.apis.BudgetsApi
import com.youneedabudget.client.apis.CategoriesApi
import com.youneedabudget.client.apis.TransactionsApi
import com.youneedabudget.client.models.Account
import com.youneedabudget.client.models.AccountResponse
import com.youneedabudget.client.models.AccountsResponse
import com.youneedabudget.client.models.AccountsResponseData
import com.youneedabudget.client.models.BudgetDetailResponse
import com.youneedabudget.client.models.BudgetSettingsResponse
import com.youneedabudget.client.models.BudgetSummary
import com.youneedabudget.client.models.BudgetSummaryResponse
import com.youneedabudget.client.models.BudgetSummaryResponseData
import com.youneedabudget.client.models.CategoriesResponse
import com.youneedabudget.client.models.CategoriesResponseData
import com.youneedabudget.client.models.Category
import com.youneedabudget.client.models.CategoryGroupWithCategories
import com.youneedabudget.client.models.CategoryResponse
import com.youneedabudget.client.models.HybridTransactionsResponse
import com.youneedabudget.client.models.SaveCategoryResponse
import com.youneedabudget.client.models.SaveMonthCategoryWrapper
import com.youneedabudget.client.models.SaveTransaction
import com.youneedabudget.client.models.SaveTransactionWrapper
import com.youneedabudget.client.models.SaveTransactionsResponse
import com.youneedabudget.client.models.SaveTransactionsResponseData
import com.youneedabudget.client.models.SaveTransactionsWrapper
import com.youneedabudget.client.models.TransactionDetail
import com.youneedabudget.client.models.TransactionResponse
import com.youneedabudget.client.models.TransactionsImportResponse
import com.youneedabudget.client.models.TransactionsResponse
import com.youneedabudget.client.models.TransactionsResponseData
import com.youneedabudget.client.models.UpdateTransactionsWrapper
import org.threeten.bp.LocalDate
import java.util.UUID

data class FakeDatabase(
  var budgetToAccountsMap: Map<UUID, List<Account>> = mapOf(),
  var budgetToCategoryGroupsMap: Map<UUID, List<CategoryGroupWithCategories>> = mapOf(),
  var budgets: List<BudgetSummary> = emptyList(),
  var accountToTransactionsMap: Map<UUID, List<TransactionDetail>> = mapOf(),
  val setUp: FakeDatabase.() -> Unit = { }
) {
  init {
    this.setUp()
  }

  fun getBudgetedAmountForCategory(categoryId: UUID) = findCategory(categoryId).budgeted

  fun getBalanceForCategory(categoryId: UUID) = findCategory(categoryId).balance

  fun setBudgetedAmountForCategory(
    categoryId: UUID,
    balanceAmount: Long,
    budgetedAmount: Long
  ) {
    findCategory(categoryId)
      .apply {
        balance = balanceAmount
        budgeted = budgetedAmount
      }
  }

  private fun findCategory(categoryId: UUID): Category {
    return budgetToCategoryGroupsMap
      .flatMap { (_, categoryGroupList) -> categoryGroupList }
      .flatMap { it.categories }
      .find { it.id == categoryId }!!
  }

  fun getBalanceForAccount(accountId: UUID) = budgetToAccountsMap
    .flatMap { (_, accounts) -> accounts }
    .find { it.id == accountId }!!
    .balance

  fun setBalanceForAccount(
    accountId: UUID,
    balanceAmount: Long
  ) {
    budgetToAccountsMap
      .flatMap { (_, accounts) -> accounts }
      .find { it.id == accountId }!!
      .balance = balanceAmount
  }
}

class FakeYnabClient(val fakeDatabase: FakeDatabase) : YnabClient {
  override val budgets: BudgetsApi = FakeBudgets(fakeDatabase)
  override val accounts: AccountsApi = FakeAccounts(fakeDatabase)
  override val transactions: TransactionsApi = FakeTransactions(fakeDatabase)
  override val categories: CategoriesApi = FakeCategories(fakeDatabase)
}

class FakeBudgets(
  private val fakeDatabase: FakeDatabase
) : BudgetsApi {
  override suspend fun getBudgetById(budgetId: String, lastKnowledgeOfServer: Long?): BudgetDetailResponse {
    TODO("Not yet implemented")
  }

  override suspend fun getBudgetSettingsById(budgetId: String): BudgetSettingsResponse {
    TODO("Not yet implemented")
  }

  override suspend fun getBudgets(includeAccounts: Boolean?): BudgetSummaryResponse {
    val budgets = if (includeAccounts ?: false) {
      fakeDatabase.budgets.map { budgetSummary ->
        budgetSummary.copy(accounts = fakeDatabase.budgetToAccountsMap[budgetSummary.id])
      }
    } else {
      fakeDatabase.budgets
    }
    return BudgetSummaryResponse(BudgetSummaryResponseData(budgets = budgets))
  }
}

class FakeAccounts(
  private val fakeDatabase: FakeDatabase
) : AccountsApi {
  override suspend fun getAccountById(budgetId: String, accountId: UUID): AccountResponse {
    TODO("Not yet implemented")
  }

  override suspend fun getAccounts(budgetId: String, lastKnowledgeOfServer: Long?): AccountsResponse {
    return AccountsResponse(
      AccountsResponseData(
        accounts = fakeDatabase.budgetToAccountsMap.getValue(budgetId.toUUID()),
        serverKnowledge = 0
      )
    )
  }
}

class FakeTransactions(
  private val fakeDatabase: FakeDatabase
) : TransactionsApi {
  override suspend fun createTransaction(budgetId: String, data: SaveTransactionsWrapper): SaveTransactionsResponse {
    val newTransactionDetail = data.transaction!!.toNewTransactionDetail()
    fakeDatabase.accountToTransactionsMap = fakeDatabase.accountToTransactionsMap
      .mapValues { (accountId, transactionDetails) ->
        if (accountId == data.transaction!!.accountId) {
          transactionDetails + newTransactionDetail
        } else {
          transactionDetails
        }
      }
    return SaveTransactionsResponse(
      SaveTransactionsResponseData(
        listOf(newTransactionDetail.id),
        0,
        newTransactionDetail,
        null,
        null  // TODO: Fail on duplicate import ids
      )
    )
  }

  override suspend fun getTransactionById(budgetId: String, transactionId: String): TransactionResponse {
    TODO("Not yet implemented")
  }

  override suspend fun getTransactions(
    budgetId: String,
    sinceDate: LocalDate?,
    type: String?,
    lastKnowledgeOfServer: Long?
  ): TransactionsResponse {
    TODO("Not yet implemented")
  }

  override suspend fun getTransactionsByAccount(
    budgetId: String,
    accountId: String,
    sinceDate: LocalDate?,
    type: String?,
    lastKnowledgeOfServer: Long?
  ): TransactionsResponse {
    return TransactionsResponse(
      TransactionsResponseData(
        fakeDatabase.accountToTransactionsMap.getValue(accountId.toUUID()),
        0
      )
    )
  }

  override suspend fun getTransactionsByCategory(
    budgetId: String,
    categoryId: String,
    sinceDate: LocalDate?,
    type: String?,
    lastKnowledgeOfServer: Long?
  ): HybridTransactionsResponse {
    TODO("Not yet implemented")
  }

  override suspend fun getTransactionsByPayee(
    budgetId: String,
    payeeId: String,
    sinceDate: LocalDate?,
    type: String?,
    lastKnowledgeOfServer: Long?
  ): HybridTransactionsResponse {
    TODO("Not yet implemented")
  }

  override suspend fun importTransactions(budgetId: String): TransactionsImportResponse {
    TODO("Not yet implemented")
  }

  override suspend fun updateTransaction(
    budgetId: String,
    transactionId: String,
    data: SaveTransactionWrapper
  ): TransactionResponse {
    TODO("Not yet implemented")
  }

  override suspend fun updateTransactions(budgetId: String, data: UpdateTransactionsWrapper): SaveTransactionsResponse {
    TODO("Not yet implemented")
  }
}

class FakeCategories(val fakeDatabase: FakeDatabase) : CategoriesApi {
  override suspend fun getCategories(budgetId: String, lastKnowledgeOfServer: Long?): CategoriesResponse {
    return CategoriesResponse(CategoriesResponseData(fakeDatabase.budgetToCategoryGroupsMap.getValue(budgetId.toUUID()), 0))
  }

  override suspend fun getCategoryById(budgetId: String, categoryId: String): CategoryResponse {
    TODO("Not yet implemented")
  }

  override suspend fun getMonthCategoryById(budgetId: String, month: LocalDate, categoryId: String): CategoryResponse {
    TODO("Not yet implemented")
  }

  override suspend fun updateMonthCategory(
    budgetId: String,
    month: LocalDate,
    categoryId: String,
    data: SaveMonthCategoryWrapper
  ): SaveCategoryResponse {
    TODO("Not yet implemented")
  }

}

private fun String.toUUID() = UUID.fromString(this)
private fun SaveTransaction.toNewTransactionDetail() = TransactionDetail(
  UUID.randomUUID().toString(),
  date,
  amount,
  cleared?.toTransactionDetailClearedEnum() ?: TransactionDetail.ClearedEnum.UNCLEARED,
  approved ?: false,
  accountId,
  false,
  "",  // TODO: Add account name
  if (subtransactions.isNullOrEmpty()) emptyList() else TODO("Add subtransaction support"),
  memo,
  flagColor?.toRegularFlagColorEnum(),
  payeeId,
  categoryId,
  null,
  null,
  null,
  importId,
  payeeName,
  ""  // TODO: Add category name
)

private fun SaveTransaction.ClearedEnum.toTransactionDetailClearedEnum() = when (this) {
  SaveTransaction.ClearedEnum.CLEARED -> TransactionDetail.ClearedEnum.CLEARED
  SaveTransaction.ClearedEnum.UNCLEARED -> TransactionDetail.ClearedEnum.UNCLEARED
  SaveTransaction.ClearedEnum.RECONCILED -> TransactionDetail.ClearedEnum.RECONCILED
}

private fun SaveTransaction.FlagColorEnum.toRegularFlagColorEnum() = when (this) {
  SaveTransaction.FlagColorEnum.RED -> TransactionDetail.FlagColorEnum.RED
  SaveTransaction.FlagColorEnum.ORANGE -> TransactionDetail.FlagColorEnum.ORANGE
  SaveTransaction.FlagColorEnum.YELLOW -> TransactionDetail.FlagColorEnum.YELLOW
  SaveTransaction.FlagColorEnum.GREEN -> TransactionDetail.FlagColorEnum.GREEN
  SaveTransaction.FlagColorEnum.BLUE -> TransactionDetail.FlagColorEnum.BLUE
  SaveTransaction.FlagColorEnum.PURPLE -> TransactionDetail.FlagColorEnum.PURPLE
}
