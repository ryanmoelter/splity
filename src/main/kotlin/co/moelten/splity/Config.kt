package co.moelten.splity

import com.sksamuel.hoplite.ConfigFailure
import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.DecoderContext
import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.StringNode
import com.sksamuel.hoplite.decoder.Decoder
import com.sksamuel.hoplite.fp.Validated.Invalid
import com.sksamuel.hoplite.fp.Validated.Valid
import org.threeten.bp.LocalDate
import kotlin.reflect.KType

data class Config(
  val version: String,
  val ynabToken: String,
  val firstAccount: AccountConfig,
  val secondAccount: AccountConfig,
  val startDate: LocalDate = LocalDate.of(1900, 1, 1), // LocalDate.MIN causes exceptions
  val sentryConfig: SentryConfig? = null,
  val ensureZeroBalanceOnCreditCards: Boolean = false
)

data class AccountConfig(
  val budgetName: String,
  val accountName: String
)

data class SentryConfig(
  val dsn: String
)

class DateDecoder : Decoder<LocalDate> {
  override fun decode(node: Node, type: KType, context: DecoderContext): ConfigResult<LocalDate> {
    return try {
      val date = LocalDate.parse((node as StringNode).value)
      Valid(date)
    } catch (e: Throwable) {
      Invalid(ConfigFailure.DecodeError(node, type))
    }
  }

  override fun supports(type: KType): Boolean {
    return type.classifier == LocalDate::class
  }
}
