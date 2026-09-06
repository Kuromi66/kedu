package com.pulse.checkin.data.cloud

import com.pulse.checkin.BuildConfig
import com.pulse.checkin.data.update.VersionManifest
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.GzipSink
import okio.buffer
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface CloudApi {
    @GET("version")
    suspend fun fetchVersion(): VersionManifest

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

    @POST("sync/meta")
    suspend fun syncMeta(@Header("Authorization") authorization: String): MetaResponse
}

object CloudApiFactory {
    fun create(baseUrl: String = BuildConfig.PULSE_API_BASE_URL): CloudApi {
        val effectiveBaseUrl = baseUrl.ifBlank { "https://pulse-sync.invalid/" }
            .let { if (it.endsWith("/")) it else "$it/" }
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(GzipRequestInterceptor())
            .build()
        return Retrofit.Builder()
            .baseUrl(effectiveBaseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CloudApi::class.java)
    }
}

// 对请求体做 gzip 压缩：大幅减小同步等大 payload 的上传体积。
// 仅对未指定 Content-Encoding 的非空请求体生效，避免重复压缩
private class GzipRequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val body = request.body
        if (body == null || request.header("Content-Encoding") != null) {
            return chain.proceed(request)
        }
        val gzipped = GzipRequestBody(body)
        return chain.proceed(
            request.newBuilder()
                .header("Content-Encoding", "gzip")
                .method(request.method, gzipped)
                .build(),
        )
    }
}

private class GzipRequestBody(private val delegate: RequestBody) : RequestBody() {
    // 构造时一次性压缩到内存，使 contentLength() 返回真实长度，
    // 避免 OkHttp 因长度未知走 chunked——Cloudflare 对 chunked+gzip 无法正确解压
    private val gzipped: ByteArray = compress(delegate)

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = gzipped.size.toLong()

    override fun writeTo(sink: BufferedSink) {
        sink.write(gzipped)
    }

    private fun compress(body: RequestBody): ByteArray {
        val buffer = okio.Buffer()
        val gzipSink = GzipSink(buffer)
        val buffered = gzipSink.buffer()
        body.writeTo(buffered)
        buffered.close()
        return buffer.readByteArray()
    }
}
