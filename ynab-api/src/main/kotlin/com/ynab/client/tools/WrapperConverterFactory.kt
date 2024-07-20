package com.ynab.client.tools

import java.lang.reflect.Type
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

internal class WrapperConverterFactory(
  private vararg val factories: Converter.Factory,
) : Converter.Factory() {
  override fun responseBodyConverter(
    type: Type,
    annotations: Array<Annotation>,
    retrofit: Retrofit,
  ): Converter<ResponseBody, *>? =
    factories.mapFirstNonNull {
      it.responseBodyConverter(type, annotations, retrofit)
    }

  override fun requestBodyConverter(
    type: Type,
    parameterAnnotations: Array<Annotation>,
    methodAnnotations: Array<Annotation>,
    retrofit: Retrofit,
  ): Converter<*, RequestBody>? =
    factories.mapFirstNonNull {
      it.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
    }

  override fun stringConverter(
    type: Type,
    annotations: Array<Annotation>,
    retrofit: Retrofit,
  ): Converter<*, String>? =
    factories.mapFirstNonNull {
      it.stringConverter(type, annotations, retrofit)
    }

  private inline fun <T, R> Array<out T>.mapFirstNonNull(transform: (T) -> R?): R? {
    for (element in this) {
      val transformed = transform(element)
      if (transformed != null) {
        return transformed
      }
    }
    return null
  }
}
