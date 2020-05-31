/**
 * NOTE: This class is auto generated by the Swagger Gradle Codegen for the following API: YNAB API Endpoints
 *
 * More info on this tool is available on https://github.com/Yelp/swagger-gradle-codegen
 */

package com.youneedabudget.client.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID
import org.threeten.bp.LocalDate

/**
 * @property accountId
 * @property date The transaction date in ISO format (e.g. 2016-12-01).  Future dates (scheduled transactions) are not permitted.  Split transaction dates cannot be changed and if a different date is supplied it will be ignored.
 * @property amount The transaction amount in milliunits format.  Split transaction amounts cannot be changed and if a different amount is supplied it will be ignored.
 * @property payeeId The payee for the transaction.  To create a transfer between two accounts, use the account transfer payee pointing to the target account.  Account transfer payees are specified as `tranfer_payee_id` on the account resource.
 * @property payeeName The payee name.  If a `payee_name` value is provided and `payee_id` has a null value, the `payee_name` value will be used to resolve the payee by either (1) a matching payee rename rule (only if `import_id` is also specified) or (2) a payee with the same name or (3) creation of a new payee.
 * @property categoryId The category for the transaction.  To configure a split transaction, you can specify null for `category_id` and provide a `subtransactions` array as part of the transaction object.  If an existing transaction is a split, the `category_id` cannot be changed.  Credit Card Payment categories are not permitted and will be ignored if supplied.
 * @property memo
 * @property cleared The cleared status of the transaction
 * @property approved Whether or not the transaction is approved.  If not supplied, transaction will be unapproved by default.
 * @property flagColor The transaction flag
 * @property importId If specified, the new transaction will be assigned this `import_id` and considered \&quot;imported\&quot;.  We will also attempt to match this imported transaction to an existing \&quot;user-entered\&quot; transation on the same account, with the same amount, and with a date +/-10 days from the imported transaction date.&lt;br&gt;&lt;br&gt;Transactions imported through File Based Import or Direct Import (not through the API) are assigned an import_id in the format: &#39;YNAB:[milliunit_amount]:[iso_date]:[occurrence]&#39;. For example, a transaction dated 2015-12-30 in the amount of -$294.23 USD would have an import_id of &#39;YNAB:-294230:2015-12-30:1&#39;.  If a second transaction on the same account was imported and had the same date and same amount, its import_id would be &#39;YNAB:-294230:2015-12-30:2&#39;.  Using a consistent format will prevent duplicates through Direct Import and File Based Import.&lt;br&gt;&lt;br&gt;If import_id is omitted or specified as null, the transaction will be treated as a \&quot;user-entered\&quot; transaction. As such, it will be eligible to be matched against transactions later being imported (via DI, FBI, or API).
 * @property subtransactions An array of subtransactions to configure a transaction as a split.  Updating `subtransactions` on an existing split transaction is not supported.
 */
@JsonClass(generateAdapter = true)
data class SaveTransaction(
    @Json(name = "account_id") @field:Json(name = "account_id") var accountId: UUID,
    @Json(name = "date") @field:Json(name = "date") var date: LocalDate,
    @Json(name = "amount") @field:Json(name = "amount") var amount: Long,
    @Json(name = "payee_id") @field:Json(name = "payee_id") var payeeId: UUID? = null,
    @Json(name = "payee_name") @field:Json(name = "payee_name") var payeeName: String? = null,
    @Json(name = "category_id") @field:Json(name = "category_id") var categoryId: UUID? = null,
    @Json(name = "memo") @field:Json(name = "memo") var memo: String? = null,
    @Json(name = "cleared") @field:Json(name = "cleared") var cleared: SaveTransaction.ClearedEnum? = null,
    @Json(name = "approved") @field:Json(name = "approved") var approved: Boolean? = null,
    @Json(name = "flag_color") @field:Json(name = "flag_color") var flagColor: SaveTransaction.FlagColorEnum? = null,
    @Json(name = "import_id") @field:Json(name = "import_id") var importId: String? = null,
    @Json(name = "subtransactions") @field:Json(name = "subtransactions") var subtransactions: List<SaveSubTransaction>? = null
) {
    /**
     * The cleared status of the transaction
     * Values: CLEARED, UNCLEARED, RECONCILED
     */
    @JsonClass(generateAdapter = false)
    enum class ClearedEnum(val value: String) {
        @Json(name = "cleared") CLEARED("cleared"),
        @Json(name = "uncleared") UNCLEARED("uncleared"),
        @Json(name = "reconciled") RECONCILED("reconciled")
    }
    /**
     * The transaction flag
     * Values: RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE
     */
    @JsonClass(generateAdapter = false)
    enum class FlagColorEnum(val value: String) {
        @Json(name = "red") RED("red"),
        @Json(name = "orange") ORANGE("orange"),
        @Json(name = "yellow") YELLOW("yellow"),
        @Json(name = "green") GREEN("green"),
        @Json(name = "blue") BLUE("blue"),
        @Json(name = "purple") PURPLE("purple")
    }
}
