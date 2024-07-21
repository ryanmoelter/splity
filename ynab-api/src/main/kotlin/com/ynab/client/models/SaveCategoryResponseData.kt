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
 * @property serverKnowledge The knowledge of the server
 */
@JsonClass(generateAdapter = true)
data class SaveCategoryResponseData(
  @Json(name = "category") @field:Json(name = "category") var category: Category,
  @Json(name = "server_knowledge") @field:Json(name = "server_knowledge") var serverKnowledge: Long,
)
