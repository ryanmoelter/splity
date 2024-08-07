/**
 * NOTE: This class is auto generated by the Swagger Gradle Codegen for the following API: YNAB API Endpoints
 *
 * More info on this tool is available on https://github.com/Yelp/swagger-gradle-codegen
 */

package com.ynab.client.apis

import com.ynab.client.models.CategoriesResponse
import com.ynab.client.models.CategoryResponse
import com.ynab.client.models.PatchMonthCategoryWrapper
import com.ynab.client.models.SaveCategoryResponse
import org.threeten.bp.LocalDate
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH

@JvmSuppressWildcards
interface CategoriesApi {
  /**
   * List categories
   * Returns all categories grouped by category group.  Amounts (budgeted, activity, balance, etc.) are specific to the current budget month (UTC).
   * The endpoint is owned by defaultname service owner
   * @param budgetId The id of the budget. \&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.ynab.com/#oauth-default-budget). (required)
   * @param lastKnowledgeOfServer The starting server knowledge.  If provided, only entities that have changed since `last_knowledge_of_server` will be included. (optional)
   */
  @Headers(
    "X-Operation-ID: getCategories",
  )
  @GET("budgets/{budget_id}/categories")
  suspend fun getCategories(
    @retrofit2.http.Path("budget_id") budgetId: String,
    @retrofit2.http.Query("last_knowledge_of_server") lastKnowledgeOfServer: Long?,
  ): CategoriesResponse

  /**
   * Single category
   * Returns a single category.  Amounts (budgeted, activity, balance, etc.) are specific to the current budget month (UTC).
   * The endpoint is owned by defaultname service owner
   * @param budgetId The id of the budget. \&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.ynab.com/#oauth-default-budget). (required)
   * @param categoryId The id of the category (required)
   */
  @Headers(
    "X-Operation-ID: getCategoryById",
  )
  @GET("budgets/{budget_id}/categories/{category_id}")
  suspend fun getCategoryById(
    @retrofit2.http.Path("budget_id") budgetId: String,
    @retrofit2.http.Path("category_id") categoryId: String,
  ): CategoryResponse

  /**
   * Single category for a specific budget month
   * Returns a single category for a specific budget month.  Amounts (budgeted, activity, balance, etc.) are specific to the current budget month (UTC).
   * The endpoint is owned by defaultname service owner
   * @param budgetId The id of the budget. \&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.ynab.com/#oauth-default-budget). (required)
   * @param month The budget month in ISO format (e.g. 2016-12-01) (\&quot;current\&quot; can also be used to specify the current calendar month (UTC)) (required)
   * @param categoryId The id of the category (required)
   */
  @Headers(
    "X-Operation-ID: getMonthCategoryById",
  )
  @GET("budgets/{budget_id}/months/{month}/categories/{category_id}")
  suspend fun getMonthCategoryById(
    @retrofit2.http.Path("budget_id") budgetId: String,
    @retrofit2.http.Path("month") month: LocalDate,
    @retrofit2.http.Path("category_id") categoryId: String,
  ): CategoryResponse

  /**
   * Update a category for a specific month
   * Update a category for a specific month.  Only `budgeted` amount can be updated.
   * The endpoint is owned by defaultname service owner
   * @param budgetId The id of the budget. \&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.ynab.com/#oauth-default-budget). (required)
   * @param month The budget month in ISO format (e.g. 2016-12-01) (\&quot;current\&quot; can also be used to specify the current calendar month (UTC)) (required)
   * @param categoryId The id of the category (required)
   * @param `data` The category to update.  Only `budgeted` amount can be updated and any other fields specified will be ignored. (required)
   */
  @Headers(
    "X-Operation-ID: updateMonthCategory",
  )
  @PATCH("budgets/{budget_id}/months/{month}/categories/{category_id}")
  suspend fun updateMonthCategory(
    @retrofit2.http.Path("budget_id") budgetId: String,
    @retrofit2.http.Path("month") month: LocalDate,
    @retrofit2.http.Path("category_id") categoryId: String,
    @retrofit2.http.Body `data`: PatchMonthCategoryWrapper,
  ): SaveCategoryResponse
}
