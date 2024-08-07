/**
 * NOTE: This class is auto generated by the Swagger Gradle Codegen for the following API: YNAB API Endpoints
 *
 * More info on this tool is available on https://github.com/Yelp/swagger-gradle-codegen
 */

package com.ynab.client.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

/**
 * @property id
 * @property name
 * @property hidden Whether or not the category group is hidden
 * @property deleted Whether or not the category group has been deleted.  Deleted category groups will only be included in delta requests.
 * @property categories Category group categories.  Amounts (budgeted, activity, balance, etc.) are specific to the current budget month (UTC).
 */
@JsonClass(generateAdapter = true)
data class CategoryGroupWithCategories(
  @Json(name = "id") @field:Json(name = "id") var id: UUID,
  @Json(name = "name") @field:Json(name = "name") var name: String,
  @Json(name = "hidden") @field:Json(name = "hidden") var hidden: Boolean,
  @Json(name = "deleted") @field:Json(name = "deleted") var deleted: Boolean,
  @Json(name = "categories") @field:Json(name = "categories") var categories: List<Category>,
)
