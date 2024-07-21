/**
 * NOTE: This class is auto generated by the Swagger Gradle Codegen for the following API: YNAB API Endpoints
 *
 * More info on this tool is available on https://github.com/Yelp/swagger-gradle-codegen
 */

package com.ynab.client.apis

import com.ynab.client.models.AccountResponse
import com.ynab.client.models.AccountsResponse
import com.ynab.client.models.PostAccountWrapper
import java.util.UUID
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

@JvmSuppressWildcards
interface AccountsApi {
  /**
   * Create a new account
   * Creates a new account
   * The endpoint is owned by defaultname service owner
   * @param budgetId The id of the budget (\&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.ynab.com/#oauth-default-budget) (required)
   * @param `data` The account to create. (required)
   */
  @Headers(
    "X-Operation-ID: createAccount",
  )
  @POST("budgets/{budget_id}/accounts")
  suspend fun createAccount(
    @retrofit2.http.Path("budget_id") budgetId: String,
    @retrofit2.http.Body `data`: PostAccountWrapper,
  ): AccountResponse

  /**
   * Single account
   * Returns a single account
   * The endpoint is owned by defaultname service owner
   * @param budgetId The id of the budget. \&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.ynab.com/#oauth-default-budget). (required)
   * @param accountId The id of the account (required)
   */
  @Headers(
    "X-Operation-ID: getAccountById",
  )
  @GET("budgets/{budget_id}/accounts/{account_id}")
  suspend fun getAccountById(
    @retrofit2.http.Path("budget_id") budgetId: String,
    @retrofit2.http.Path("account_id") accountId: UUID,
  ): AccountResponse

  /**
   * Account list
   * Returns all accounts
   * The endpoint is owned by defaultname service owner
   * @param budgetId The id of the budget. \&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.ynab.com/#oauth-default-budget). (required)
   * @param lastKnowledgeOfServer The starting server knowledge.  If provided, only entities that have changed since `last_knowledge_of_server` will be included. (optional)
   */
  @Headers(
    "X-Operation-ID: getAccounts",
  )
  @GET("budgets/{budget_id}/accounts")
  suspend fun getAccounts(
    @retrofit2.http.Path("budget_id") budgetId: String,
    @retrofit2.http.Query("last_knowledge_of_server") lastKnowledgeOfServer: Long?,
  ): AccountsResponse
}
