package com.ryanmoelter.splity

import com.ryanmoelter.splity.database.toAccountId
import com.ryanmoelter.splity.database.toCategoryId
import com.ynab.client.models.Account
import com.ynab.client.models.AccountType
import com.ynab.client.models.Category
import com.ynab.client.models.CategoryGroupWithCategories
import java.util.UUID.randomUUID

val FIRST_CREDIT_CARD_CATEGORY_GROUP_ID = randomUUID().toCategoryId()
const val CREDIT_CARD_CATEGORY_GROUP_NAME = "Credit Card Payments"
val FIRST_CREDIT_CARD_CATEGORY_SAPPHIRE_ID = randomUUID().toCategoryId()
const val FIRST_CREDIT_CARD_CATEGORY_SAPPHIRE_NAME = "Chase Sapphire"
val FIRST_CREDIT_CARD_CATEGORY_FREEDOM_ID = randomUUID().toCategoryId()
const val FIRST_CREDIT_CARD_CATEGORY_FREEDOM_NAME = "Chase Freedom"

val FIRST_CREDIT_CARD_ACCOUNT_SAPPHIRE_ID = randomUUID().toAccountId()
val FIRST_CREDIT_CARD_ACCOUNT_FREEDOM_ID = randomUUID().toAccountId()

val firstCreditCardCategorySapphire =
  Category(
    id = FIRST_CREDIT_CARD_CATEGORY_SAPPHIRE_ID.plainUuid,
    categoryGroupId = FIRST_CREDIT_CARD_CATEGORY_GROUP_ID.plainUuid,
    name = FIRST_CREDIT_CARD_CATEGORY_SAPPHIRE_NAME,
    hidden = false,
    budgeted = 0,
    activity = 0,
    balance = 0,
    deleted = false,
    originalCategoryGroupId = null,
    note = null,
    goalType = null,
    goalCreationMonth = null,
    goalTarget = null,
    goalTargetMonth = null,
    goalPercentageComplete = null,
  )

val firstCreditCardCategoryFreedom =
  Category(
    id = FIRST_CREDIT_CARD_CATEGORY_FREEDOM_ID.plainUuid,
    categoryGroupId = FIRST_CREDIT_CARD_CATEGORY_GROUP_ID.plainUuid,
    name = FIRST_CREDIT_CARD_CATEGORY_FREEDOM_NAME,
    hidden = false,
    budgeted = 0,
    activity = 0,
    balance = 0,
    deleted = false,
    originalCategoryGroupId = null,
    note = null,
    goalType = null,
    goalCreationMonth = null,
    goalTarget = null,
    goalTargetMonth = null,
    goalPercentageComplete = null,
  )

val firstCreditCardCategoryGroup =
  CategoryGroupWithCategories(
    id = FIRST_CREDIT_CARD_CATEGORY_GROUP_ID.plainUuid,
    name = CREDIT_CARD_CATEGORY_GROUP_NAME,
    hidden = false,
    deleted = false,
    categories =
      listOf(
        firstCreditCardCategorySapphire,
        firstCreditCardCategoryFreedom,
      ),
  )

val firstNonCreditCardCategoryGroup =
  CategoryGroupWithCategories(
    id = randomUUID(),
    name = "Occasional",
    hidden = false,
    deleted = false,
    categories =
      listOf(
        firstCreditCardCategorySapphire.copy(
          id = randomUUID(),
          categoryGroupId = randomUUID(),
          name = "Haircuts",
        ),
      ),
  )

val firstCreditCardAccountSapphire =
  Account(
    id = FIRST_CREDIT_CARD_ACCOUNT_SAPPHIRE_ID.plainUuid,
    name = FIRST_CREDIT_CARD_CATEGORY_SAPPHIRE_NAME,
    type = AccountType.CREDITCARD,
    onBudget = true,
    closed = false,
    balance = 0,
    clearedBalance = 0,
    unclearedBalance = 0,
    transferPayeeId = randomUUID(),
    deleted = false,
    note = null,
  )

val firstCreditCardAccountFreedom =
  Account(
    id = FIRST_CREDIT_CARD_ACCOUNT_FREEDOM_ID.plainUuid,
    name = FIRST_CREDIT_CARD_CATEGORY_FREEDOM_NAME,
    type = AccountType.CREDITCARD,
    onBudget = true,
    closed = false,
    balance = 0,
    clearedBalance = 0,
    unclearedBalance = 0,
    transferPayeeId = randomUUID(),
    deleted = false,
    note = null,
  )
