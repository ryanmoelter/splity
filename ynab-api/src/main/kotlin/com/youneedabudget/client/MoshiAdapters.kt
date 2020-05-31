package com.youneedabudget.client

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.util.*

class UuidAdapter {
  @ToJson
  fun toJson(uuid: UUID) = uuid.toString()

  @FromJson
  fun fromJson(json: String): UUID = UUID.fromString(json)
}
