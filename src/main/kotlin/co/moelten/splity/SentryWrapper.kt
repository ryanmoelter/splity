package co.moelten.splity

import io.sentry.Sentry
import io.sentry.SpanStatus

interface SentryWrapper {
  fun doInTransaction(
    operation: String,
    name: String,
    action: () -> Unit
  )

  fun <T> doInSpan(
    operation: String,
    action: () -> T
  ): T

  suspend fun doInSpan(
    operation: String,
    action: suspend () -> Unit
  )
}

class SentryWrapperImpl(
  sentryConfig: SentryConfig,
  version: String
) : SentryWrapper {

  init {
    Sentry.init { options ->
      options.dsn = sentryConfig.dsn
      options.tracesSampleRate = 1.0
      options.release = version
    }
  }

  override fun doInTransaction(
    operation: String,
    name: String,
    action: () -> Unit
  ) {
    val transaction = Sentry.startTransaction(name, operation, true)
    try {
      action().also { transaction.status = SpanStatus.OK }
    } catch (e: Throwable) {
      transaction.throwable = e
      transaction.status = SpanStatus.INTERNAL_ERROR
      throw e
    } finally {
      transaction.finish()
    }
  }

  override fun <T> doInSpan(
    operation: String,
    action: () -> T
  ): T {
    val span = Sentry.getSpan()!!.startChild(operation)
    return try {
      action().also { span.status = SpanStatus.OK }
    } catch (e: Throwable) {
      span.throwable = e
      span.status = SpanStatus.INTERNAL_ERROR
      throw e
    } finally {
      span.finish()
    }
  }

  override suspend fun doInSpan(
    operation: String,
    action: suspend () -> Unit
  ) {
    val span = Sentry.getSpan()!!.startChild(operation)
    try {
      action().also { span.status = SpanStatus.OK }
    } catch (e: Throwable) {
      span.throwable = e
      span.status = SpanStatus.INTERNAL_ERROR
      throw e
    } finally {
      span.finish()
    }
  }
}

class NoSentryWrapper : SentryWrapper {
  override fun doInTransaction(operation: String, name: String, action: () -> Unit) = action()

  override fun <T> doInSpan(operation: String, action: () -> T): T = action()

  override suspend fun doInSpan(operation: String, action: suspend () -> Unit) = action()
}

fun setUpSentry(sentryConfig: SentryConfig?, version: String): SentryWrapper = if (sentryConfig != null) {
  SentryWrapperImpl(sentryConfig, version)
} else {
  NoSentryWrapper()
}
