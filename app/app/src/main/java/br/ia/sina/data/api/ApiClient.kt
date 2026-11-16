package br.ia.sina.data.api

import br.ia.sina.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class ApiError(val code: String, message: String, val httpStatus: Int) : Exception(message)

val SinaJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

fun createSinaApi(tokenProvider: suspend () -> String?): SinaApi {
    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = kotlinx.coroutines.runBlocking { tokenProvider() }
            val request = chain.request().newBuilder().apply {
                if (token != null) header("Authorization", "Bearer $token")
            }.build()
            chain.proceed(request)
        }
        .build()
    return Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(SinaJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(SinaApi::class.java)
}

fun Throwable.toApiError(): ApiError? {
    if (this is ApiError) return this
    if (this !is HttpException) return null
    val body = response()?.errorBody()?.string()
    val detail = body?.let { runCatching { SinaJson.decodeFromString<ApiErrorBody>(it).error }.getOrNull() }
    return ApiError(detail?.code ?: "HTTP_${code()}", detail?.message ?: message(), code())
}
