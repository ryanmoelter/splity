/**
 * NOTE: This class is auto generated by the Swagger Gradle Codegen for the following API: YNAB API Endpoints
 *
 * More info on this tool is available on https://github.com/Yelp/swagger-gradle-codegen
 */

package com.ynab.client.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @property category
 */
@JsonClass(generateAdapter = true)
data class PatchMonthCategoryWrapper(
  @Json(name = "category") @field:Json(name = "category") var category: SaveMonthCategory,
)
