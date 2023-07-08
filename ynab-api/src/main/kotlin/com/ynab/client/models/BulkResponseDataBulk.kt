/**
 * NOTE: This class is auto generated by the Swagger Gradle Codegen for the following API: YNAB API Endpoints
 *
 * More info on this tool is available on https://github.com/Yelp/swagger-gradle-codegen
 */

package com.ynab.client.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @property transactionIds The list of Transaction ids that were created.
 * @property duplicateImportIds If any Transactions were not created because they had an `import_id` matching a transaction already on the same account, the specified import_id(s) will be included in this list.
 */
@JsonClass(generateAdapter = true)
data class BulkResponseDataBulk(
    @Json(name = "transaction_ids") @field:Json(name = "transaction_ids") var transactionIds: List<String>,
    @Json(name = "duplicate_import_ids") @field:Json(name = "duplicate_import_ids") var duplicateImportIds: List<String>
)