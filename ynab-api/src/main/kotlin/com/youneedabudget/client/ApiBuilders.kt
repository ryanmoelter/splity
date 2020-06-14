package com.youneedabudget.client

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.youneedabudget.client.apis.AccountsApi
import com.youneedabudget.client.apis.BudgetsApi
import com.youneedabudget.client.apis.CategoriesApi
import com.youneedabudget.client.apis.TransactionsApi
import com.youneedabudget.client.tools.GeneratedCodeConverters
import com.youneedabudget.client.tools.TypesAdapterFactory
import com.youneedabudget.client.tools.XNullableAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import retrofit2.Retrofit

val moshi: Moshi = Moshi.Builder()
  .add(XNullableAdapterFactory())
  .add(KotlinJsonAdapterFactory())
  .add(TypesAdapterFactory())
  .add(UuidAdapter())
  .build()


fun createOkHttpClient(apiKey: String): OkHttpClient = OkHttpClient.Builder()
  .addInterceptor(HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
    override fun log(message: String) {
      println(message)
    }
  }).apply {
    level = BODY
  })
  .addInterceptor { chain ->
    val newRequest: Request = chain.request().newBuilder()
      .addHeader("Authorization", "Bearer $apiKey")
      .build()
    chain.proceed(newRequest)
  }
  .build()

fun createRetrofit(apiKey: String): Retrofit = Retrofit.Builder()
  .baseUrl("https://api.youneedabudget.com/v1/")
  .client(createOkHttpClient(apiKey))
  .addConverterFactory(GeneratedCodeConverters.converterFactory(moshi))
  .build()

interface YnabClient {
  val budgets: BudgetsApi
  val accounts: AccountsApi
  val transactions: TransactionsApi
  val categories: CategoriesApi
}

class YnabClientImpl(apiKey: String) : YnabClient {
  private val retrofit by lazy { createRetrofit(apiKey) }
  override val budgets: BudgetsApi by lazy { retrofit.create(BudgetsApi::class.java) }
  override val accounts: AccountsApi by lazy { retrofit.create(AccountsApi::class.java) }
  override val transactions: TransactionsApi by lazy { retrofit.create(TransactionsApi::class.java) }
  override val categories: CategoriesApi by lazy { retrofit.create(CategoriesApi::class.java) }
}
