package com.ryanmoelter.splity.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.Companion.IN_MEMORY
import me.tatarka.inject.annotations.Component

@Component
abstract class FakeDatabaseModule : DatabaseModule {
  override fun sqlDriver(): SqlDriver = JdbcSqliteDriver(IN_MEMORY)
}
