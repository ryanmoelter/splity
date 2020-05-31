package co.moelten.splity

import com.github.scribejava.core.model.OAuth1AccessToken
import com.youneedabudget.client.YnabClient
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

fun main() {
  runBlocking {
    val ynab = YnabClient()
    val splitwise = SplitwiseClient()

    copySplitwiseIntoYnab(ynab, splitwise)
  }
}

suspend fun copySplitwiseIntoYnab(ynab: YnabClient, splitwise: SplitwiseClient) = coroutineScope {
  splitwise.groups
}

private suspend fun connectToSplitwise() {
  val splitwise = SplitwiseClient()

  println(splitwise.groups)
}

private fun setUpSplitwiseConnection() {
  val splitwise = SplitwiseClient()
  println(splitwise.authorizationUrl)
  val verifier = readLine()!!
  val token = splitwise.getAccessToken(verifier) as OAuth1AccessToken
  println("""
    token: ${token.token}
    token secret: ${token.tokenSecret}
    rawResponse: ${token.rawResponse}
  """.trimIndent())
}
