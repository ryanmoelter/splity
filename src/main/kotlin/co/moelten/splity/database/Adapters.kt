package co.moelten.splity.database

import com.squareup.sqldelight.ColumnAdapter
import org.threeten.bp.LocalDate

val localDateAdapter = object : ColumnAdapter<LocalDate, String> {
  override fun decode(databaseValue: String) = LocalDate.parse(databaseValue)
  override fun encode(value: LocalDate) = value.toString()
}
