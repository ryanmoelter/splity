/**
 * NOTE: This class is auto generated by the Swagger Gradle Codegen for the following API: YNAB API Endpoints
 *
 * More info on this tool is available on https://github.com/Yelp/swagger-gradle-codegen
 */

package com.ynab.client.apis

import com.ynab.client.models.BudgetDetailResponse
import com.ynab.client.models.BudgetSettingsResponse
import com.ynab.client.models.BudgetSummaryResponse
import retrofit2.http.GET
import retrofit2.http.Headers

@JvmSuppressWildcards
interface BudgetsApi {
  /**
   * Single budget
   * Returns a single budget with all related entities.  This resource is effectively a full budget export.
   * The endpoint is owned by defaultname service owner
   * @param budgetId The id of the budget. \&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.ynab.com/#oauth-default-budget). (required)
   * @param lastKnowledgeOfServer The starting server knowledge.  If provided, only entities that have changed since `last_knowledge_of_server` will be included. (optional)
   */
  @Headers(
    "X-Operation-ID: getBudgetById",
  )
  @GET("budgets/{budget_id}")
  suspend fun getBudgetById(
    @retrofit2.http.Path("budget_id") budgetId: String,
    @retrofit2.http.Query("last_knowledge_of_server") lastKnowledgeOfServer: Long?,
  ): BudgetDetailResponse

  /**
   * Budget Settings
   * Returns settings for a budget
   * The endpoint is owned by defaultname service owner
   * @param budgetId The id of the budget. \&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.ynab.com/#oauth-default-budget). (required)
   */
  @Headers(
    "X-Operation-ID: getBudgetSettingsById",
  )
  @GET("budgets/{budget_id}/settings")
  suspend fun getBudgetSettingsById(
    @retrofit2.http.Path("budget_id") budgetId: String,
  ): BudgetSettingsResponse

  /**
   * List budgets
   * Returns budgets list with summary information
   * The endpoint is owned by defaultname service owner
   * @param includeAccounts Whether to include the list of budget accounts (optional)
   */
  @Headers(
    "X-Operation-ID: getBudgets",
  )
  @GET("budgets")
  suspend fun getBudgets(
    @retrofit2.http.Query("include_accounts") includeAccounts: Boolean?,
  ): BudgetSummaryResponse
}
