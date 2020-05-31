package com.youneedabudget.client

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.youneedabudget.client.apis.BudgetsApi
import com.youneedabudget.client.tools.GeneratedCodeConverters
import com.youneedabudget.client.tools.TypesAdapterFactory
import com.youneedabudget.client.tools.XNullableAdapterFactory
import retrofit2.Retrofit

val moshi = Moshi.Builder()
  .add(XNullableAdapterFactory())
  .add(KotlinJsonAdapterFactory())
  .add(TypesAdapterFactory())
  .add(UuidAdapter())
  .build()

val retrofit by lazy {
  Retrofit.Builder()
    .baseUrl("https://api.youneedabudget.com/v1/")
    .addConverterFactory(GeneratedCodeConverters.converterFactory(moshi))
    .build()
}

fun createBudgetsApi(): BudgetsApi = retrofit.create(BudgetsApi::class.java)
