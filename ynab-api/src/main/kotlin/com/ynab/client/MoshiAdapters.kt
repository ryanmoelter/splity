@file:Suppress("ktlint:standard:filename")

package com.ynab.client

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.util.UUID

class UuidAdapter {
  @ToJson
  fun toJson(uuid: UUID) = uuid.toString()

  @FromJson
  fun fromJson(json: String): UUID = UUID.fromString(json)
}
