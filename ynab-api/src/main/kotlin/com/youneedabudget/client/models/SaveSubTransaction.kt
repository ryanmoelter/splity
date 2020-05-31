/**
 * NOTE: This class is auto generated by the Swagger Gradle Codegen for the following API: YNAB API Endpoints
 *
 * More info on this tool is available on https://github.com/Yelp/swagger-gradle-codegen
 */

package com.youneedabudget.client.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

/**
 * @property amount The subtransaction amount in milliunits format.
 * @property payeeId The payee for the subtransaction.
 * @property payeeName The payee name.  If a `payee_name` value is provided and `payee_id` has a null value, the `payee_name` value will be used to resolve the payee by either (1) a matching payee rename rule (only if import_id is also specified on parent transaction) or (2) a payee with the same name or (3) creation of a new payee.
 * @property categoryId The category for the subtransaction.  Credit Card Payment categories are not permitted and will be ignored if supplied.
 * @property memo
 */
@JsonClass(generateAdapter = true)
data class SaveSubTransaction(
    @Json(name = "amount") @field:Json(name = "amount") var amount: Long,
    @Json(name = "payee_id") @field:Json(name = "payee_id") var payeeId: UUID? = null,
    @Json(name = "payee_name") @field:Json(name = "payee_name") var payeeName: String? = null,
    @Json(name = "category_id") @field:Json(name = "category_id") var categoryId: UUID? = null,
    @Json(name = "memo") @field:Json(name = "memo") var memo: String? = null
)
