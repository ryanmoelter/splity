/**
 * NOTE: This class is auto generated by the Swagger Gradle Codegen for the following API: YNAB API Endpoints
 *
 * More info on this tool is available on https://github.com/Yelp/swagger-gradle-codegen
 */

package com.ynab.client.apis

import com.ynab.client.models.BulkResponse
import com.ynab.client.models.BulkTransactions
import retrofit2.http.Headers
import retrofit2.http.POST

@JvmSuppressWildcards
interface DeprecatedApi {
    /**
     * Bulk create transactions
     * Creates multiple transactions.  Although this endpoint is still supported, it is recommended to use 'POST /budgets/{budget_id}/transactions' to create multiple transactions.
     * The endpoint is owned by defaultname service owner
     * @param budgetId The id of the budget. \&quot;last-used\&quot; can be used to specify the last used budget and \&quot;default\&quot; can be used if default budget selection is enabled (see: https://api.ynab.com/#oauth-default-budget). (required)
     * @param transactions The list of transactions to create (required)
     */
    @Headers(
        "X-Operation-ID: bulkCreateTransactions"
    )
    @POST("budgets/{budget_id}/transactions/bulk")
    suspend fun bulkCreateTransactions(
        @retrofit2.http.Path("budget_id") budgetId: String,
        @retrofit2.http.Body transactions: BulkTransactions
    ): BulkResponse
}