/**
 * NOTE: This class is auto generated by the Swagger Gradle Codegen for the following API: YNAB API Endpoints
 *
 * More info on this tool is available on https://github.com/Yelp/swagger-gradle-codegen
 */

package com.youneedabudget.client.apis

import com.youneedabudget.client.models.ErrorResponse
import com.youneedabudget.client.models.HybridTransactionsResponse
import com.youneedabudget.client.models.SaveTransactionWrapper
import com.youneedabudget.client.models.SaveTransactionsResponse
import com.youneedabudget.client.models.SaveTransactionsWrapper
import com.youneedabudget.client.models.TransactionResponse
import com.youneedabudget.client.models.TransactionsImportResponse
import com.youneedabudget.client.models.TransactionsResponse
import com.youneedabudget.client.models.UpdateTransactionsWrapper
import org.threeten.bp.LocalDate
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT

@JvmSuppressWildcards
interface TransactionsApi {
    /**
     * Create a single transaction or multiple transactions
     * Creates a single transaction or multiple transactions.  If you provide a body containing a `transaction` object, a single transaction will be created and if you provide a body containing a `transactions` array, multiple transactions will be created.  Scheduled transactions cannot be created on this endpoint.
     * The endpoint is owned by defaultname service owner
     * @param budgetId The id of the budget (\&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.youneedabudget.com/#oauth-default-budget) (required)
     * @param `data` The transaction or transactions to create.  To create a single transaction you can specify a value for the `transaction` object and to create multiple transactions you can specify an array of `transactions`.  It is expected that you will only provide a value for one of these objects. (required)
     */
    @Headers(
        "X-Operation-ID: createTransaction"
    )
    @POST("budgets/{budget_id}/transactions")
    suspend fun createTransaction(
        @retrofit2.http.Path("budget_id") budgetId: String,
        @retrofit2.http.Body `data`: SaveTransactionsWrapper
    ): SaveTransactionsResponse
    /**
     * Single transaction
     * Returns a single transaction
     * The endpoint is owned by defaultname service owner
     * @param budgetId The id of the budget (\&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.youneedabudget.com/#oauth-default-budget) (required)
     * @param transactionId The id of the transaction (required)
     */
    @Headers(
        "X-Operation-ID: getTransactionById"
    )
    @GET("budgets/{budget_id}/transactions/{transaction_id}")
    suspend fun getTransactionById(
        @retrofit2.http.Path("budget_id") budgetId: String,
        @retrofit2.http.Path("transaction_id") transactionId: String
    ): TransactionResponse
    /**
     * List transactions
     * Returns budget transactions
     * The endpoint is owned by defaultname service owner
     * @param budgetId The id of the budget (\&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.youneedabudget.com/#oauth-default-budget) (required)
     * @param sinceDate If specified, only transactions on or after this date will be included.  The date should be ISO formatted (e.g. 2016-12-30). (optional)
     * @param type If specified, only transactions of the specified type will be included. \&quot;uncategorized\&quot; and \&quot;unapproved\&quot; are currently supported. (optional)
     * @param lastKnowledgeOfServer The starting server knowledge.  If provided, only entities that have changed since `last_knowledge_of_server` will be included. (optional)
     */
    @Headers(
        "X-Operation-ID: getTransactions"
    )
    @GET("budgets/{budget_id}/transactions")
    suspend fun getTransactions(
        @retrofit2.http.Path("budget_id") budgetId: String,
        @retrofit2.http.Query("since_date") sinceDate: LocalDate?,
        @retrofit2.http.Query("type") type: String?,
        @retrofit2.http.Query("last_knowledge_of_server") lastKnowledgeOfServer: Long?
    ): TransactionsResponse
    /**
     * List account transactions
     * Returns all transactions for a specified account
     * The endpoint is owned by defaultname service owner
     * @param budgetId The id of the budget (\&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.youneedabudget.com/#oauth-default-budget) (required)
     * @param accountId The id of the account (required)
     * @param sinceDate If specified, only transactions on or after this date will be included.  The date should be ISO formatted (e.g. 2016-12-30). (optional)
     * @param type If specified, only transactions of the specified type will be included. \&quot;uncategorized\&quot; and \&quot;unapproved\&quot; are currently supported. (optional)
     * @param lastKnowledgeOfServer The starting server knowledge.  If provided, only entities that have changed since `last_knowledge_of_server` will be included. (optional)
     */
    @Headers(
        "X-Operation-ID: getTransactionsByAccount"
    )
    @GET("budgets/{budget_id}/accounts/{account_id}/transactions")
    suspend fun getTransactionsByAccount(
        @retrofit2.http.Path("budget_id") budgetId: String,
        @retrofit2.http.Path("account_id") accountId: String,
        @retrofit2.http.Query("since_date") sinceDate: LocalDate?,
        @retrofit2.http.Query("type") type: String?,
        @retrofit2.http.Query("last_knowledge_of_server") lastKnowledgeOfServer: Long?
    ): TransactionsResponse
    /**
     * List category transactions
     * Returns all transactions for a specified category
     * The endpoint is owned by defaultname service owner
     * @param budgetId The id of the budget (\&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.youneedabudget.com/#oauth-default-budget) (required)
     * @param categoryId The id of the category (required)
     * @param sinceDate If specified, only transactions on or after this date will be included.  The date should be ISO formatted (e.g. 2016-12-30). (optional)
     * @param type If specified, only transactions of the specified type will be included. \&quot;uncategorized\&quot; and \&quot;unapproved\&quot; are currently supported. (optional)
     * @param lastKnowledgeOfServer The starting server knowledge.  If provided, only entities that have changed since `last_knowledge_of_server` will be included. (optional)
     */
    @Headers(
        "X-Operation-ID: getTransactionsByCategory"
    )
    @GET("budgets/{budget_id}/categories/{category_id}/transactions")
    suspend fun getTransactionsByCategory(
        @retrofit2.http.Path("budget_id") budgetId: String,
        @retrofit2.http.Path("category_id") categoryId: String,
        @retrofit2.http.Query("since_date") sinceDate: LocalDate?,
        @retrofit2.http.Query("type") type: String?,
        @retrofit2.http.Query("last_knowledge_of_server") lastKnowledgeOfServer: Long?
    ): HybridTransactionsResponse
    /**
     * List payee transactions
     * Returns all transactions for a specified payee
     * The endpoint is owned by defaultname service owner
     * @param budgetId The id of the budget (\&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.youneedabudget.com/#oauth-default-budget) (required)
     * @param payeeId The id of the payee (required)
     * @param sinceDate If specified, only transactions on or after this date will be included.  The date should be ISO formatted (e.g. 2016-12-30). (optional)
     * @param type If specified, only transactions of the specified type will be included. \&quot;uncategorized\&quot; and \&quot;unapproved\&quot; are currently supported. (optional)
     * @param lastKnowledgeOfServer The starting server knowledge.  If provided, only entities that have changed since `last_knowledge_of_server` will be included. (optional)
     */
    @Headers(
        "X-Operation-ID: getTransactionsByPayee"
    )
    @GET("budgets/{budget_id}/payees/{payee_id}/transactions")
    suspend fun getTransactionsByPayee(
        @retrofit2.http.Path("budget_id") budgetId: String,
        @retrofit2.http.Path("payee_id") payeeId: String,
        @retrofit2.http.Query("since_date") sinceDate: LocalDate?,
        @retrofit2.http.Query("type") type: String?,
        @retrofit2.http.Query("last_knowledge_of_server") lastKnowledgeOfServer: Long?
    ): HybridTransactionsResponse
    /**
     * Import transactions
     * Imports transactions.
     * The endpoint is owned by defaultname service owner
     * @param budgetId The id of the budget (\&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.youneedabudget.com/#oauth-default-budget) (required)
     */
    @Headers(
        "X-Operation-ID: importTransactions"
    )
    @POST("budgets/{budget_id}/transactions/import")
    suspend fun importTransactions(
        @retrofit2.http.Path("budget_id") budgetId: String
    ): TransactionsImportResponse
    /**
     * Updates an existing transaction
     * Updates a transaction
     * The endpoint is owned by defaultname service owner
     * @param budgetId The id of the budget (\&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.youneedabudget.com/#oauth-default-budget) (required)
     * @param transactionId The id of the transaction (required)
     * @param `data` The transaction to update (required)
     */
    @Headers(
        "X-Operation-ID: updateTransaction"
    )
    @PUT("budgets/{budget_id}/transactions/{transaction_id}")
    suspend fun updateTransaction(
        @retrofit2.http.Path("budget_id") budgetId: String,
        @retrofit2.http.Path("transaction_id") transactionId: String,
        @retrofit2.http.Body `data`: SaveTransactionWrapper
    ): TransactionResponse
    /**
     * Update multiple transactions
     * Updates multiple transactions, by `id` or `import_id`.
     * The endpoint is owned by defaultname service owner
     * @param budgetId The id of the budget (\&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.youneedabudget.com/#oauth-default-budget) (required)
     * @param `data` The transactions to update. Each transaction must have either an `id` or `import_id` specified. If `id` is specified as null an `import_id` value can be provided which will allow transaction(s) to be updated by their `import_id`. If an `id` is specified, it will always be used for lookup. (required)
     */
    @Headers(
        "X-Operation-ID: updateTransactions"
    )
    @PATCH("budgets/{budget_id}/transactions")
    suspend fun updateTransactions(
        @retrofit2.http.Path("budget_id") budgetId: String,
        @retrofit2.http.Body `data`: UpdateTransactionsWrapper
    ): SaveTransactionsResponse
}
