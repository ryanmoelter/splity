/**
 * NOTE: This class is auto generated by the Swagger Gradle Codegen for the following API: YNAB API Endpoints
 *
 * More info on this tool is available on https://github.com/Yelp/swagger-gradle-codegen
 */

package com.youneedabudget.client.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @property `data`
 */
@JsonClass(generateAdapter = true)
data class BudgetDetailResponse(
    @Json(name = "data") @field:Json(name = "data") var `data`: BudgetDetailResponseData
)
