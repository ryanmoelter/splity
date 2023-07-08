/**
 * NOTE: This class is auto generated by the Swagger Gradle Codegen for the following API: YNAB API Endpoints
 *
 * More info on this tool is available on https://github.com/Yelp/swagger-gradle-codegen
 */

package com.ynab.client.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.threeten.bp.LocalDate

/**
 * @property month
 * @property note
 * @property income The total amount of transactions categorized to &#39;Inflow: Ready to Assign&#39; in the month
 * @property budgeted The total amount budgeted in the month
 * @property activity The total amount of transactions in the month, excluding those categorized to &#39;Inflow: Ready to Assign&#39;
 * @property toBeBudgeted The available amount for &#39;Ready to Assign&#39;
 * @property ageOfMoney The Age of Money as of the month
 * @property deleted Whether or not the month has been deleted.  Deleted months will only be included in delta requests.
 */
@JsonClass(generateAdapter = true)
data class MonthSummary(
    @Json(name = "month") @field:Json(name = "month") var month: LocalDate,
    @Json(name = "income") @field:Json(name = "income") var income: Long,
    @Json(name = "budgeted") @field:Json(name = "budgeted") var budgeted: Long,
    @Json(name = "activity") @field:Json(name = "activity") var activity: Long,
    @Json(name = "to_be_budgeted") @field:Json(name = "to_be_budgeted") var toBeBudgeted: Long,
    @Json(name = "deleted") @field:Json(name = "deleted") var deleted: Boolean,
    @Json(name = "note") @field:Json(name = "note") var note: String? = null,
    @Json(name = "age_of_money") @field:Json(name = "age_of_money") var ageOfMoney: Int? = null
)