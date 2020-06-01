package com.splitwise.client.apis

import Splitwise
import com.github.scribejava.core.model.OAuth1AccessToken
import com.splitwise.client.SPLITWISE_CONSUMER_KEY
import com.splitwise.client.SPLITWISE_CONSUMER_SECRET
import com.splitwise.client.SPLITWISE_OAUTH_TOKEN
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class SplitwiseApi {
  private val client: SplitwiseOauthClient = SplitwiseOauthClient()
  private val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  suspend fun getGroups(): String {
    return client.groups
  }

  fun reauthorizeSplitwise() {
    val splitwise = SplitwiseOauthClient()
    println(splitwise.authorizationUrl)
    val verifier = readLine()!!
    val token = splitwise.getAccessToken(verifier) as OAuth1AccessToken
    println("""
    token: ${token.token}
    token secret: ${token.tokenSecret}
    rawResponse: ${token.rawResponse}
  """.trimIndent())
  }
}

private class SplitwiseOauthClient : Splitwise(
  SPLITWISE_CONSUMER_KEY,
  SPLITWISE_CONSUMER_SECRET
) {
  init {
    setAccessToken(SPLITWISE_OAUTH_TOKEN)
  }

  private fun setAccessToken(accessToken: OAuth1AccessToken) {
    util.setAccessToken(accessToken.token, accessToken.tokenSecret, accessToken.rawResponse)
  }
}
