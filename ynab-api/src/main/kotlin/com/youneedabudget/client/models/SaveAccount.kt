/**
 * NOTE: This class is auto generated by the Swagger Gradle Codegen for the following API: YNAB API Endpoints
 *
 * More info on this tool is available on https://github.com/Yelp/swagger-gradle-codegen
 */

package com.youneedabudget.client.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @property name The name of the account
 * @property type
 * @property balance The current balance of the account in milliunits format
 */
@JsonClass(generateAdapter = true)
data class SaveAccount(
    @Json(name = "name") @field:Json(name = "name") var name: String,
    @Json(name = "type") @field:Json(name = "type") var type: AccountType,
    @Json(name = "balance") @field:Json(name = "balance") var balance: Long
)
