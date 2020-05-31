package co.moelten.splity

import Splitwise
import com.github.scribejava.core.model.OAuth1AccessToken

class SplitwiseClient : Splitwise(SPLITWISE_CONSUMER_KEY, SPLITWISE_CONSUMER_SECRET) {
  init {
    setAccessToken(SPLITWISE_OAUTH_TOKEN)
  }

  private fun setAccessToken(accessToken: OAuth1AccessToken) {
    util.setAccessToken(accessToken.token, accessToken.tokenSecret, accessToken.rawResponse)
  }
}
