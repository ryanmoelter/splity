package co.moelten.splity.database

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver.Companion.IN_MEMORY
import me.tatarka.inject.annotations.Component

@Component
abstract class FakeDatabaseModule : DatabaseModule {
  override fun sqlDriver(): SqlDriver = JdbcSqliteDriver(IN_MEMORY)
}
