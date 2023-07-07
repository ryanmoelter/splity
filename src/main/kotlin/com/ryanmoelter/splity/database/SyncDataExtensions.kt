package com.ryanmoelter.splity.database

import com.ryanmoelter.splity.AccountAndBudget
import com.ryanmoelter.ynab.SyncData
import com.ryanmoelter.ynab.SyncDataQueries

val SyncData.firstAccountAndBudget
  get() = AccountAndBudget(firstAccountId, firstAccountPayeeId, firstBudgetId)

val SyncData.secondAccountAndBudget
  get() = AccountAndBudget(secondAccountId, secondAccountPayeeId, secondBudgetId)

fun SyncDataQueries.replaceOnly(syncData: SyncData) {
  transaction {
    clear()
    insert(syncData)
  }
}
