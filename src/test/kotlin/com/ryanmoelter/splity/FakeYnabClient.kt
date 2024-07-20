package com.ryanmoelter.splity

import com.ryanmoelter.splity.database.ProcessedState
import com.ryanmoelter.splity.database.toAccountId
import com.ryanmoelter.splity.database.toBudgetId
import com.ryanmoelter.splity.database.toCategoryId
import com.ryanmoelter.splity.database.toPayeeId
import com.ryanmoelter.splity.database.toPublicSubTransaction
import com.ryanmoelter.splity.database.toPublicTransactionDetail
import com.ryanmoelter.splity.database.toTransactionId
import com.ryanmoelter.splity.models.PublicTransactionDetail
import com.ynab.client.MAX_IMPORT_ID_LENGTH
import com.ynab.client.YnabClient
import com.ynab.client.apis.AccountsApi
import com.ynab.client.apis.BudgetsApi
import com.ynab.client.apis.CategoriesApi
import com.ynab.client.apis.TransactionsApi
import com.ynab.client.models.AccountResponse
import com.ynab.client.models.AccountsResponse
import com.ynab.client.models.AccountsResponseData
import com.ynab.client.models.BudgetDetailResponse
import com.ynab.client.models.BudgetSettingsResponse
import com.ynab.client.models.BudgetSummaryResponse
import com.ynab.client.models.BudgetSummaryResponseData
import com.ynab.client.models.CategoriesResponse
import com.ynab.client.models.CategoriesResponseData
import com.ynab.client.models.CategoryResponse
import com.ynab.client.models.HybridTransactionsResponse
import com.ynab.client.models.PatchMonthCategoryWrapper
import com.ynab.client.models.PatchTransactionsWrapper
import com.ynab.client.models.PostAccountWrapper
import com.ynab.client.models.PostTransactionsWrapper
import com.ynab.client.models.PutTransactionWrapper
import com.ynab.client.models.SaveCategoryResponse
import com.ynab.client.models.SaveCategoryResponseData
import com.ynab.client.models.SaveSubTransaction
import com.ynab.client.models.SaveTransaction
import com.ynab.client.models.SaveTransactionsResponse
import com.ynab.client.models.SaveTransactionsResponseData
import com.ynab.client.models.SubTransaction
import com.ynab.client.models.TransactionDetail
import com.ynab.client.models.TransactionResponse
import com.ynab.client.models.TransactionResponseData
import com.ynab.client.models.TransactionsImportResponse
import com.ynab.client.models.TransactionsResponse
import com.ynab.client.models.TransactionsResponseData
import io.kotest.matchers.collections.shouldContain
import java.util.UUID
import org.threeten.bp.LocalDate

class FakeYnabClient(
  val fakeYnabServerDatabase: FakeYnabServerDatabase,
) : YnabClient {
  override val budgets: BudgetsApi = FakeBudgets(fakeYnabServerDatabase)
  override val accounts: AccountsApi = FakeAccounts(fakeYnabServerDatabase)
  override val transactions: TransactionsApi = FakeTransactions(fakeYnabServerDatabase)
  override val categories: CategoriesApi = FakeCategories(fakeYnabServerDatabase)
}

class FakeBudgets(
  private val fakeYnabServerDatabase: FakeYnabServerDatabase,
) : BudgetsApi {
  override suspend fun getBudgetById(
    budgetId: String,
    lastKnowledgeOfServer: Long?,
  ): BudgetDetailResponse {
    TODO("Not yet implemented")
  }

  override suspend fun getBudgetSettingsById(budgetId: String): BudgetSettingsResponse {
    TODO("Not yet implemented")
  }

  override suspend fun getBudgets(includeAccounts: Boolean?): BudgetSummaryResponse {
    val budgets =
      if (includeAccounts == true) {
        fakeYnabServerDatabase.budgets.map { budgetSummary ->
          budgetSummary.copy(
            accounts = fakeYnabServerDatabase.budgetToAccountsMap[budgetSummary.id.toBudgetId()],
          )
        }
      } else {
        fakeYnabServerDatabase.budgets
      }
    return BudgetSummaryResponse(BudgetSummaryResponseData(budgets = budgets))
  }
}

class FakeAccounts(
  private val fakeYnabServerDatabase: FakeYnabServerDatabase,
) : AccountsApi {
  override suspend fun createAccount(
    budgetId: String,
    data: PostAccountWrapper,
  ): AccountResponse {
    TODO("Not yet implemented")
  }

  override suspend fun getAccountById(
    budgetId: String,
    accountId: UUID,
  ): AccountResponse {
    TODO("Not yet implemented")
  }

  override suspend fun getAccounts(
    budgetId: String,
    lastKnowledgeOfServer: Long?,
  ): AccountsResponse =
    AccountsResponse(
      AccountsResponseData(
        accounts = fakeYnabServerDatabase.budgetToAccountsMap.getValue(budgetId.toBudgetId()),
        serverKnowledge = fakeYnabServerDatabase.currentServerKnowledge,
      ),
    )
}

class FakeTransactions(
  private val fakeYnabServerDatabase: FakeYnabServerDatabase,
) : TransactionsApi {
  override suspend fun createTransaction(
    budgetId: String,
    data: PostTransactionsWrapper,
  ): SaveTransactionsResponse {
    require((data.transaction!!.importId?.length ?: 0) <= 36) {
      "import_id (${data.transaction!!.importId}) is too long (maximum is $MAX_IMPORT_ID_LENGTH characters)"
    }
    val newTransactionDetail = data.transaction!!.toNewTransactionDetail()
    val accountId = data.transaction!!.accountId.toAccountId()
    fakeYnabServerDatabase.addOrUpdateTransactionsForAccount(
      accountId,
      listOf(newTransactionDetail),
    )

    return SaveTransactionsResponse(
      SaveTransactionsResponseData(
        listOf(newTransactionDetail.id),
        fakeYnabServerDatabase.currentServerKnowledge,
        newTransactionDetail,
        null,
        null, // TODO: Fail on duplicate import ids
      ),
    )
  }

  override suspend fun deleteTransaction(
    budgetId: String,
    transactionId: String,
  ): TransactionResponse {
    TODO("Not yet implemented")
  }

  override suspend fun getTransactionById(
    budgetId: String,
    transactionId: String,
  ): TransactionResponse =
    TransactionResponse(
      TransactionResponseData(
        fakeYnabServerDatabase.getTransactionById(transactionId.toTransactionId())!!,
      ),
    )

  override suspend fun getTransactions(
    budgetId: String,
    sinceDate: LocalDate?,
    type: String?,
    lastKnowledgeOfServer: Long?,
  ): TransactionsResponse =
    TransactionsResponse(
      TransactionsResponseData(
        fakeYnabServerDatabase
          .budgetToAccountsMap
          .getValue(budgetId.toBudgetId())
          .flatMap { account ->
            fakeYnabServerDatabase.getTransactionsForAccount(
              account.id.toAccountId(),
              lastKnowledgeOfServer ?: NO_SERVER_KNOWLEDGE,
            )
          },
        fakeYnabServerDatabase.currentServerKnowledge,
      ),
    )

  override suspend fun getTransactionsByAccount(
    budgetId: String,
    accountId: String,
    sinceDate: LocalDate?,
    type: String?,
    lastKnowledgeOfServer: Long?,
  ): TransactionsResponse =
    TransactionsResponse(
      TransactionsResponseData(
        fakeYnabServerDatabase.getTransactionsForAccount(
          accountId.toAccountId(),
          lastKnowledgeOfServer ?: NO_SERVER_KNOWLEDGE,
        ),
        fakeYnabServerDatabase.currentServerKnowledge,
      ),
    )

  override suspend fun getTransactionsByCategory(
    budgetId: String,
    categoryId: String,
    sinceDate: LocalDate?,
    type: String?,
    lastKnowledgeOfServer: Long?,
  ): HybridTransactionsResponse {
    TODO("Not yet implemented")
  }

  override suspend fun getTransactionsByPayee(
    budgetId: String,
    payeeId: String,
    sinceDate: LocalDate?,
    type: String?,
    lastKnowledgeOfServer: Long?,
  ): HybridTransactionsResponse {
    TODO("Not yet implemented")
  }

  override suspend fun importTransactions(budgetId: String): TransactionsImportResponse {
    TODO("Not yet implemented")
  }

  override suspend fun updateTransaction(
    budgetId: String,
    transactionId: String,
    data: PutTransactionWrapper,
  ): TransactionResponse {
    val splitTransactionWithTransfer =
      (
        fakeYnabServerDatabase.getTransactionById(transactionId.toTransactionId())
          ?: error("Cannot find transaction to update in server database with id $transactionId")
      ).let { transaction ->
        if (transaction.transferTransactionId != null) {
          fakeYnabServerDatabase
            .getTransactionsForAccount(transaction.accountId.toAccountId())
            .find { transactionInAccount ->
              transactionInAccount.subtransactions.any { subTransaction ->
                subTransaction.id == transaction.transferTransactionId
              }
            }
        } else {
          null
        }
      }

    assert(splitTransactionWithTransfer == null) {
      "Updating a transfer with a split source will cause them to silently break on the real " +
        "API: $splitTransactionWithTransfer"
    }

    fakeYnabServerDatabase.budgetToAccountsMap
      .getValue(budgetId.toBudgetId())
      .map { account -> account.id.toAccountId() }
      .shouldContain(data.transaction.accountId.toAccountId())

    val newTransaction = fakeYnabServerDatabase.updateTransaction(transactionId, data.transaction)

    val payeeIds =
      fakeYnabServerDatabase.budgetToAccountsMap
        .getValue(budgetId.toBudgetId())
        .map { account -> account.transferPayeeId.toPayeeId() }
        .toSet()

    // Create transfers for subtransactions
    val subTransactionUpdates =
      newTransaction.subtransactions
        .map { subTransaction ->
          if (payeeIds.contains(subTransaction.payeeId?.toPayeeId())) {
            val transferTargetAccount = (
              fakeYnabServerDatabase.budgetToAccountsMap[budgetId.toBudgetId()]!!
                .find { it.transferPayeeId == subTransaction.payeeId }
                ?: error(
                  "Cannot split into non-existent account with payeeId: ${subTransaction.payeeId}",
                )
            )
            val transferTransactionId = UUID.randomUUID().toTransactionId()
            fakeYnabServerDatabase.addTransactions(
              with(subTransaction) {
                PublicTransactionDetail(
                  id = transferTransactionId,
                  date = newTransaction.date,
                  accountId = transferTargetAccount.id.toAccountId(),
                  accountName = transferTargetAccount.name,
                  amount = -amount,
                  payeeId = null,
                  payeeName = "Transfer : ${newTransaction.accountName}",
                  categoryId = null,
                  categoryName = null,
                  memo = memo,
                  cleared = TransactionDetail.ClearedEnum.UNCLEARED,
                  approved = newTransaction.approved,
                  flagColor = null,
                  importId = null,
                  transferAccountId = newTransaction.accountId.toAccountId(),
                  transferTransactionId = subTransaction.id.toTransactionId(),
                  matchedTransactionId = null,
                  subTransactions = emptyList(),
                  processedState = ProcessedState.CREATED,
                  budgetId = budgetId.toBudgetId(),
                )
              },
            )

            subTransaction.copy(
              transferAccountId = transferTargetAccount.id,
              transferTransactionId = transferTransactionId.string,
            )
          } else {
            subTransaction
          }
        }

    fakeYnabServerDatabase.addTransactions(
      newTransaction
        .toPublicTransactionDetail(budgetId.toBudgetId())
        .copy(
          subTransactions =
            subTransactionUpdates.map {
              it.toPublicSubTransaction(
                newTransaction.accountId.toAccountId(),
                budgetId.toBudgetId(),
              )
            },
        ),
    )

    return getTransactionById(budgetId = budgetId, transactionId = newTransaction.id)
  }

  override suspend fun updateTransactions(
    budgetId: String,
    data: PatchTransactionsWrapper,
  ): SaveTransactionsResponse {
    TODO("Not yet implemented")
  }
}

class FakeCategories(
  private val fakeYnabServerDatabase: FakeYnabServerDatabase,
) : CategoriesApi {
  override suspend fun getCategories(
    budgetId: String,
    lastKnowledgeOfServer: Long?,
  ): CategoriesResponse =
    CategoriesResponse(
      CategoriesResponseData(
        fakeYnabServerDatabase.budgetToCategoryGroupsMap.getValue(
          budgetId.toBudgetId(),
        ),
        fakeYnabServerDatabase.currentServerKnowledge,
      ),
    )

  override suspend fun getCategoryById(
    budgetId: String,
    categoryId: String,
  ): CategoryResponse {
    TODO("Not yet implemented")
  }

  override suspend fun getMonthCategoryById(
    budgetId: String,
    month: LocalDate,
    categoryId: String,
  ): CategoryResponse {
    TODO("Not yet implemented")
  }

  override suspend fun updateMonthCategory(
    budgetId: String,
    month: LocalDate,
    categoryId: String,
    data: PatchMonthCategoryWrapper,
  ): SaveCategoryResponse {
    val category =
      fakeYnabServerDatabase.budgetToCategoryGroupsMap
        .getValue(budgetId.toBudgetId())
        .flatMap { it.categories }
        .find { it.id == categoryId.toCategoryId().plainUuid }!!
    category.budgeted = data.category.budgeted
    return SaveCategoryResponse(
      SaveCategoryResponseData(
        category,
        fakeYnabServerDatabase.currentServerKnowledge,
      ),
    )
  }
}

fun SaveTransaction.toNewTransactionDetail(
  id: String = UUID.randomUUID().toString(),
  oldTransaction: TransactionDetail? = null,
) = TransactionDetail(
  id = id,
  date = date,
  amount = amount,
  cleared = cleared?.toTransactionDetailClearedEnum() ?: TransactionDetail.ClearedEnum.UNCLEARED,
  approved = approved ?: false,
  accountId = accountId,
  deleted = false,
  accountName = oldTransaction?.accountName.orEmpty(),
  subtransactions =
    if (subtransactions.isNullOrEmpty()) {
      emptyList()
    } else {
      subtransactions!!.map { it.toNewSubTransaction(id) }
    },
  memo = memo,
  flagColor = flagColor?.toRegularFlagColorEnum(),
  payeeId = payeeId,
  categoryId = categoryId,
  transferAccountId = null,
  transferTransactionId = null,
  matchedTransactionId = null,
  importId = importId,
  payeeName = payeeName ?: oldTransaction?.payeeName,
  categoryName = oldTransaction?.categoryName.orEmpty(),
)

fun SaveSubTransaction.toNewSubTransaction(transactionId: String) =
  SubTransaction(
    id = UUID.randomUUID().toString(),
    transactionId = transactionId,
    amount = amount,
    deleted = false,
    memo = memo,
    payeeId = payeeId,
    payeeName = payeeName,
    categoryId = categoryId,
    categoryName = null,
    transferAccountId = null, // TODO
    transferTransactionId = null, // TODO
  )

private fun SaveTransaction.ClearedEnum.toTransactionDetailClearedEnum() =
  when (this) {
    SaveTransaction.ClearedEnum.CLEARED -> TransactionDetail.ClearedEnum.CLEARED
    SaveTransaction.ClearedEnum.UNCLEARED -> TransactionDetail.ClearedEnum.UNCLEARED
    SaveTransaction.ClearedEnum.RECONCILED -> TransactionDetail.ClearedEnum.RECONCILED
  }

private fun SaveTransaction.FlagColorEnum.toRegularFlagColorEnum() =
  when (this) {
    SaveTransaction.FlagColorEnum.RED -> TransactionDetail.FlagColorEnum.RED
    SaveTransaction.FlagColorEnum.ORANGE -> TransactionDetail.FlagColorEnum.ORANGE
    SaveTransaction.FlagColorEnum.YELLOW -> TransactionDetail.FlagColorEnum.YELLOW
    SaveTransaction.FlagColorEnum.GREEN -> TransactionDetail.FlagColorEnum.GREEN
    SaveTransaction.FlagColorEnum.BLUE -> TransactionDetail.FlagColorEnum.BLUE
    SaveTransaction.FlagColorEnum.PURPLE -> TransactionDetail.FlagColorEnum.PURPLE
    SaveTransaction.FlagColorEnum.NONE -> TransactionDetail.FlagColorEnum.NONE
  }
