package co.moelten.splity.database

import co.moelten.splity.AccountAndBudget
import com.ryanmoelter.ynab.SyncData

val SyncData.firstAccountAndBudget
  get() = AccountAndBudget(firstAccountId, firstBudgetId)

val SyncData.secondAccountAndBudget
  get() = AccountAndBudget(secondAccountId, secondBudgetId)
