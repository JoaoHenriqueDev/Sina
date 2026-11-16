package br.ia.sina.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class RegisterRequest(val role: String, val fcmToken: String? = null, val displayName: String, val email: String, val phone: String)

@Serializable
data class RegisterResponse(val deviceId: String, val deviceToken: String)

@Serializable
data class FcmTokenRequest(val fcmToken: String)

@Serializable
data class HeartbeatRequest(val listenerEnabled: Boolean)

@Serializable
data class CodeResponse(val code: String, val expiresAt: String)

@Serializable
data class RedeemRequest(val code: String)

@Serializable
data class PeerDevice(
    val deviceId: String,
    val displayName: String? = null,
    val protectionStatus: String? = null,
    val lastSeenAt: String? = null,
    val phone: String? = null,
)

@Serializable
data class PairingItem(
    val pairingId: String,
    @SerialName("protected") val protectedDevice: PeerDevice? = null,
    val guardian: PeerDevice? = null,
    val createdAt: String? = null,
)

@Serializable
data class PairingList(val items: List<PairingItem>)

@Serializable
data class AnalyzeRequest(
    val source: String,
    val text: String,
    val localSignals: List<String>,
    val receivedAt: String,
    val messageCount: Int? = null,
    val context: String? = null,
)

@Serializable
data class AnalyzeResponse(
    val verdict: String,
    val scamType: String? = null,
    val confidence: Double = 0.0,
    val elderlyMessage: String? = null,
    val engine: String? = null,
    val alertId: String? = null,
    val guardianNotified: Boolean = false,
)

@Serializable
data class AlertItem(
    val id: String,
    val protectedDeviceId: String,
    val source: String,
    val verdict: String,
    val scamType: String? = null,
    val confidence: Double = 0.0,
    val familySummary: String? = null,
    val messageExcerpt: String? = null,
    val createdAt: String,
)

@Serializable
data class AlertPage(val items: List<AlertItem>, val nextCursor: String? = null)

@Serializable
data class ApiErrorBody(val error: ApiErrorDetail)

@Serializable
data class ApiErrorDetail(val code: String, val message: String)

interface SinaApi {
    @POST("v1/devices")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @PUT("v1/devices/me/fcm-token")
    suspend fun updateFcmToken(@Body body: FcmTokenRequest)

    @POST("v1/devices/me/heartbeat")
    suspend fun heartbeat(@Body body: HeartbeatRequest)

    @DELETE("v1/devices/me")
    suspend fun deleteAccount()

    @POST("v1/pairing-codes")
    suspend fun createPairingCode(): CodeResponse

    @POST("v1/pairings")
    suspend fun redeem(@Body body: RedeemRequest): PairingItem

    @GET("v1/pairings")
    suspend fun pairings(): PairingList

    @DELETE("v1/pairings/{id}")
    suspend fun deletePairing(@Path("id") id: String)

    @GET("v1/alerts")
    suspend fun alerts(
        @Query("protectedId") protectedId: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null,
    ): AlertPage

    @POST("v1/analyze")
    suspend fun analyze(@Body body: AnalyzeRequest): AnalyzeResponse
}
