package co.moelten.splity.database

import com.squareup.sqldelight.ColumnAdapter
import org.threeten.bp.LocalDate
import java.util.UUID

val uuidAdapter = object : ColumnAdapter<UUID, String> {
  override fun decode(databaseValue: String) = UUID.fromString(databaseValue)
  override fun encode(value: UUID) = value.toString()
}

val localDateAdapter = object : ColumnAdapter<LocalDate, String> {
  override fun decode(databaseValue: String) = LocalDate.parse(databaseValue)
  override fun encode(value: LocalDate) = value.toString()
}
