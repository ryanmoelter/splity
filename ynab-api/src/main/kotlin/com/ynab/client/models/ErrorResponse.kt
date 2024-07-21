/**
 * NOTE: This class is auto generated by the Swagger Gradle Codegen for the following API: YNAB API Endpoints
 *
 * More info on this tool is available on https://github.com/Yelp/swagger-gradle-codegen
 */

package com.ynab.client.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @property error
 */
@JsonClass(generateAdapter = true)
data class ErrorResponse(
  @Json(name = "error") @field:Json(name = "error") var error: ErrorDetail,
)
