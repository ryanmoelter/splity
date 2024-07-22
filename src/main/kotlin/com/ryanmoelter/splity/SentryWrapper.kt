package com.ryanmoelter.splity

import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.TransactionOptions

interface SentryWrapper {
  fun doInTransaction(
    operation: String,
    name: String,
    action: () -> Unit,
  )

  fun <T> doInImmediateSpan(
    operation: String,
    action: () -> T,
  ): T

  suspend fun <T> doInSpan(
    operation: String,
    action: suspend () -> T,
  ): T
}

class SentryWrapperImpl(
  sentryConfig: SentryConfig,
  version: String,
  dist: String,
) : SentryWrapper {
  init {
    Sentry.init { options ->
      options.dsn = sentryConfig.dsn
      options.tracesSampleRate = 0.1
      options.release = version
      options.dist = dist
    }
  }

  override fun doInTransaction(
    operation: String,
    name: String,
    action: () -> Unit,
  ) {
    val transaction =
      Sentry.startTransaction(
        name,
        operation,
        TransactionOptions().apply {
          isBindToScope = true
        },
      )
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

  override fun <T> doInImmediateSpan(
    operation: String,
    action: () -> T,
  ): T {
    val span = Sentry.getSpan()!!.startChild(operation, operation)
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

  override suspend fun <T> doInSpan(
    operation: String,
    action: suspend () -> T,
  ): T {
    val span = Sentry.getSpan()!!.startChild(operation, operation)
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
}

class NoSentryWrapper : SentryWrapper {
  override fun doInTransaction(
    operation: String,
    name: String,
    action: () -> Unit,
  ) = action()

  override fun <T> doInImmediateSpan(
    operation: String,
    action: () -> T,
  ): T = action()

  override suspend fun <T> doInSpan(
    operation: String,
    action: suspend () -> T,
  ) = action()
}

fun setUpSentry(
  sentryConfig: SentryConfig?,
  version: String,
  dist: String,
): SentryWrapper =
  if (sentryConfig !=
    null
  ) {
    SentryWrapperImpl(sentryConfig, version, dist)
  } else {
    NoSentryWrapper()
  }
