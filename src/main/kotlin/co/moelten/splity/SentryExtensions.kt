package co.moelten.splity

import io.sentry.Sentry
import io.sentry.SpanStatus

fun doInTransaction(
  operation: String,
  name: String,
  action: () -> Unit
) {
  val transaction = Sentry.startTransaction(name, operation, true)
  try {
    action()
  } catch (e: Throwable) {
    transaction.throwable = e
    transaction.status = SpanStatus.INTERNAL_ERROR
    throw e
  } finally {
    transaction.finish()
  }
}

fun <T> doInSpan(
  operation: String,
  action: () -> T
): T {
  val span = Sentry.getSpan()!!.startChild(operation)
  return try {
    action()
  } catch (e: Throwable) {
    span.throwable = e
    span.status = SpanStatus.INTERNAL_ERROR
    throw e
  } finally {
    span.finish()
  }
}

suspend fun doInSpan(
  operation: String,
  action: suspend () -> Unit
) {
  val span = Sentry.getSpan()!!.startChild(operation)
  try {
    action()
  } catch (e: Throwable) {
    span.throwable = e
    span.status = SpanStatus.INTERNAL_ERROR
    throw e
  } finally {
    span.finish()
  }
}

fun setUpSentry(sentryConfig: SentryConfig, version: String) {
  Sentry.init { options ->
    options.dsn = sentryConfig.dsn
    options.tracesSampleRate = 1.0
    options.release = version
  }
}
