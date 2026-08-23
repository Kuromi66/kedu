package com.pulse.checkin.data.cloud

import com.pulse.checkin.BuildConfig
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface CloudApi {
    @POST("auth/register")
    suspend fun register(@Body body: AuthRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: AuthRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String): Response<Unit>

    @POST("sync")
    suspend fun sync(
        @Header("Authorization") authorization: String,
        @Body body: SyncRequest,
    ): SyncResponse
}

object CloudApiFactory {
    fun create(baseUrl: String = BuildConfig.PULSE_API_BASE_URL): CloudApi {
        val effectiveBaseUrl = baseUrl.ifBlank { "https://pulse-sync.invalid/" }
            .let { if (it.endsWith("/")) it else "$it/" }
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(effectiveBaseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CloudApi::class.java)
    }
}
