package com.ryanmoelter.splity

import com.ryanmoelter.splity.database.toAccountId
import com.ryanmoelter.splity.database.toBudgetId
import com.ryanmoelter.splity.database.toPayeeId
import com.ynab.client.models.Account
import com.ynab.client.models.AccountType
import com.ynab.client.models.BudgetSummary

val FROM_BUDGET_ID = "fa5b22c0-9ea4-4ea4-bb6f-bfac601a4bb0".toBudgetId()
const val FROM_BUDGET_NAME = "First budget"
val FROM_ACCOUNT_ID = "60fe9e61-e16d-451c-9f46-8f95f02fc0df".toAccountId()
const val FROM_ACCOUNT_NAME = "Split - From"
val FROM_ACCOUNT_PAYEE_ID = "ba2524ba-60f6-442a-9d64-c8210e512100".toPayeeId()

val FROM_ACCOUNT_AND_BUDGET =
  AccountAndBudget(FROM_ACCOUNT_ID, FROM_ACCOUNT_PAYEE_ID, FROM_BUDGET_ID)
val FROM_ACCOUNT =
  Account(
    id = FROM_ACCOUNT_ID.plainUuid,
    name = FROM_ACCOUNT_NAME,
    type = AccountType.CHECKING,
    onBudget = true,
    closed = false,
    balance = 0L,
    clearedBalance = 0L,
    unclearedBalance = 0L,
    transferPayeeId = FROM_ACCOUNT_PAYEE_ID.plainUuid,
    deleted = false,
    note = null,
  )

val TO_BUDGET_ID = "d5badd43-0f0e-4c52-b965-f9ccfbd2d06d".toBudgetId()
const val TO_BUDGET_NAME = "Second budget"
val TO_ACCOUNT_ID = "3d17b188-4b5c-4e14-88f2-4c29d76d6b34".toAccountId()
const val TO_ACCOUNT_NAME = "Split - To"
val TO_ACCOUNT_PAYEE_ID = "d197cede-256b-40ee-bd94-25a37ee5c742".toPayeeId()

val TO_ACCOUNT_AND_BUDGET =
  AccountAndBudget(TO_ACCOUNT_ID, TO_ACCOUNT_PAYEE_ID, TO_BUDGET_ID)
val TO_ACCOUNT =
  Account(
    id = TO_ACCOUNT_ID.plainUuid,
    name = TO_ACCOUNT_NAME,
    type = AccountType.CHECKING,
    onBudget = true,
    closed = false,
    balance = 0L,
    clearedBalance = 0L,
    unclearedBalance = 0L,
    transferPayeeId = TO_ACCOUNT_PAYEE_ID.plainUuid,
    deleted = false,
    note = null,
  )

val FROM_TRANSFER_SOURCE_ACCOUNT_ID = "be0ccddd-464e-4e76-acac-27f29dab4599".toAccountId()
const val FROM_TRANSFER_SOURCE_ACCOUNT_NAME = "Checking"
val FROM_TRANSFER_SOURCE_ACCOUNT_PAYEE_ID = "11bcce60-549a-4545-be1d-cf06d5a4d062".toPayeeId()
val FROM_TRANSFER_SOURCE_ACCOUNT =
  Account(
    id = FROM_TRANSFER_SOURCE_ACCOUNT_ID.plainUuid,
    name = FROM_TRANSFER_SOURCE_ACCOUNT_NAME,
    type = AccountType.CHECKING,
    onBudget = true,
    closed = false,
    balance = 0L,
    clearedBalance = 0L,
    unclearedBalance = 0L,
    transferPayeeId = FROM_TRANSFER_SOURCE_ACCOUNT_PAYEE_ID.plainUuid,
    deleted = false,
    note = null,
  )

val firstAccountConfig = AccountConfig(FROM_BUDGET_NAME, FROM_ACCOUNT_NAME)
val secondAccountConfig = AccountConfig(TO_BUDGET_NAME, TO_ACCOUNT_NAME)
val fakeConfig =
  Config(
    version = "0.0.0-TEST",
    ynabToken = "fakeToken",
    firstAccount = firstAccountConfig,
    secondAccount = secondAccountConfig,
  )

val fromBudget =
  BudgetSummary(
    id = FROM_BUDGET_ID.plainUuid,
    name = FROM_BUDGET_NAME,
    lastModifiedOn = null,
    firstMonth = null,
    lastMonth = null,
    dateFormat = null,
    currencyFormat = null,
    accounts = emptyList(),
  )

val toBudget =
  BudgetSummary(
    id = TO_BUDGET_ID.plainUuid,
    name = TO_BUDGET_NAME,
    lastModifiedOn = null,
    firstMonth = null,
    lastMonth = null,
    dateFormat = null,
    currencyFormat = null,
    accounts = emptyList(),
  )
