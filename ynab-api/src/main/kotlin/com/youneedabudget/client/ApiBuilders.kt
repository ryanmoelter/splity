package com.youneedabudget.client

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.youneedabudget.client.apis.AccountsApi
import com.youneedabudget.client.apis.BudgetsApi
import com.youneedabudget.client.tools.GeneratedCodeConverters
import com.youneedabudget.client.tools.TypesAdapterFactory
import com.youneedabudget.client.tools.XNullableAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit

val moshi: Moshi = Moshi.Builder()
  .add(XNullableAdapterFactory())
  .add(KotlinJsonAdapterFactory())
  .add(TypesAdapterFactory())
  .add(UuidAdapter())
  .build()


val client: OkHttpClient = OkHttpClient.Builder()
  .addInterceptor { chain ->
    val newRequest: Request = chain.request().newBuilder()
      .addHeader("Authorization", "Bearer $YNAB_API_KEY")
      .build()
    chain.proceed(newRequest)
  }
  .build()

val retrofit: Retrofit = Retrofit.Builder()
  .baseUrl("https://api.youneedabudget.com/v1/")
  .client(client)
  .addConverterFactory(GeneratedCodeConverters.converterFactory(moshi))
  .build()

class YnabClient {
  val budgets: BudgetsApi by lazy { retrofit.create(BudgetsApi::class.java) }
  val accounts: AccountsApi by lazy { retrofit.create(AccountsApi::class.java) }
}
