/**
 * NOTE: This class is auto generated by the Swagger Gradle Codegen for the following API: YNAB API Endpoints
 *
 * More info on this tool is available on https://github.com/Yelp/swagger-gradle-codegen
 */

package com.ynab.client.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @property accounts
 * @property serverKnowledge The knowledge of the server
 */
@JsonClass(generateAdapter = true)
data class AccountsResponseData(
  @Json(name = "accounts") @field:Json(name = "accounts") var accounts: List<Account>,
  @Json(name = "server_knowledge") @field:Json(name = "server_knowledge") var serverKnowledge: Long,
)
