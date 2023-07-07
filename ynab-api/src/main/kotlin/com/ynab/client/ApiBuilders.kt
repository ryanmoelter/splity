package com.ynab.client

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ynab.client.apis.AccountsApi
import com.ynab.client.apis.BudgetsApi
import com.ynab.client.apis.CategoriesApi
import com.ynab.client.apis.TransactionsApi
import com.ynab.client.tools.GeneratedCodeConverters
import com.ynab.client.tools.TypesAdapterFactory
import com.ynab.client.tools.XNullableAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import retrofit2.Retrofit

val moshi: Moshi = Moshi.Builder()
  .add(XNullableAdapterFactory())
  .add(KotlinJsonAdapterFactory())
  .add(TypesAdapterFactory())
  .add(UuidAdapter())
  .build()

fun createOkHttpClient(
  apiKey: String,
  doInSpan: (operation: String, () -> Response) -> Response
): OkHttpClient = OkHttpClient.Builder()
  .addInterceptor(HttpLoggingInterceptor { message -> println(message) }.apply {
    level = BODY
  })
  .addInterceptor { chain ->
    val newRequest: Request = chain.request().newBuilder()
      .addHeader("Authorization", "Bearer $apiKey")
      .build()
    chain.proceed(newRequest)
  }
  .addInterceptor { chain ->
    doInSpan("${chain.request().method} ${chain.request().url}") {
      chain.proceed(chain.request())
    }
  }
  .build()

fun createRetrofit(
  apiKey: String,
  doInSpan: (operation: String, () -> Response) -> Response
): Retrofit = Retrofit.Builder()
  .baseUrl("https://api.ynab.com/v1/")
  .client(createOkHttpClient(apiKey, doInSpan))
  .addConverterFactory(GeneratedCodeConverters.converterFactory(moshi))
  .build()

interface YnabClient {
  val budgets: BudgetsApi
  val accounts: AccountsApi
  val transactions: TransactionsApi
  val categories: CategoriesApi
}

class YnabClientImpl(apiKey: String, doInSpan: (operation: String, () -> Response) -> Response) : YnabClient {
  private val retrofit by lazy { createRetrofit(apiKey, doInSpan) }
  override val budgets: BudgetsApi by lazy { retrofit.create(BudgetsApi::class.java) }
  override val accounts: AccountsApi by lazy { retrofit.create(AccountsApi::class.java) }
  override val transactions: TransactionsApi by lazy { retrofit.create(TransactionsApi::class.java) }
  override val categories: CategoriesApi by lazy { retrofit.create(CategoriesApi::class.java) }
}

const val MAX_IMPORT_ID_LENGTH = 36
