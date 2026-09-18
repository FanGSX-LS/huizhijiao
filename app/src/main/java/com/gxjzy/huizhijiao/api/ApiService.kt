package com.gxjzy.huizhijiao.api

import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.*

data class ProxyRequestBody(val url: String, val data: Map<String, String>)

interface ApiService {

    @POST("api/login")
    suspend fun login(@Body body: Map<String, String>): Response<JsonObject>

    @POST("api/admin/login")
    suspend fun adminLogin(@Body body: Map<String, String>): Response<JsonObject>

    @GET("api/me")
    suspend fun getMe(): Response<JsonObject>

    @GET("api/checkin-progress")
    suspend fun getCheckinProgress(): Response<JsonObject>

    @GET("api/checkin-dates")
    suspend fun getCheckinDates(@Query("year") year: Int, @Query("month") month: Int): Response<JsonObject>

    @GET("api/checkin-calendar")
    suspend fun getCheckinCalendar(@Query("year") year: Int, @Query("month") month: Int): Response<JsonObject>

    @POST("api/proxy")
    suspend fun proxy(@Body body: ProxyRequestBody): Response<JsonObject>

    @GET("api/presets/{type}")
    suspend fun getPresets(@Path("type") type: String): Response<JsonObject>

    @POST("api/presets/{type}")
    suspend fun savePreset(@Path("type") type: String, @Body body: okhttp3.RequestBody): Response<JsonObject>

    @PUT("api/presets/{id}")
    suspend fun updatePreset(@Path("id") id: Long, @Body body: okhttp3.RequestBody): Response<JsonObject>

    @DELETE("api/presets/{id}")
    suspend fun deletePreset(@Path("id") id: Long): Response<JsonObject>

    @GET("api/templates/{type}")
    suspend fun getTemplates(@Path("type") type: String): Response<com.google.gson.JsonElement>

    @POST("api/templates/{type}")
    suspend fun saveTemplates(@Path("type") type: String, @Body body: okhttp3.RequestBody): Response<JsonObject>

    @PUT("api/templates/{type}/{id}")
    suspend fun updateTemplate(@Path("type") type: String, @Path("id") id: Long, @Body body: okhttp3.RequestBody): Response<JsonObject>

    @DELETE("api/templates/{type}/{id}")
    suspend fun deleteTemplate(@Path("type") type: String, @Path("id") id: Long): Response<JsonObject>

    @POST("api/templates/{type}/import")
    suspend fun importTemplates(@Path("type") type: String, @Body body: okhttp3.RequestBody): Response<JsonObject>

    @GET("api/auto-checkin")
    suspend fun getAutoCheckin(): Response<JsonObject>

    @POST("api/auto-checkin/start")
    suspend fun startAutoCheckin(@Body body: okhttp3.RequestBody): Response<JsonObject>

    @POST("api/auto-checkin/stop")
    suspend fun stopAutoCheckin(): Response<JsonObject>

    @GET("api/auto-checkin/logs")
    suspend fun getAutoCheckinLogs(): Response<JsonObject>

    @DELETE("api/auto-checkin/logs")
    suspend fun clearAutoCheckinLogs(): Response<JsonObject>

    @GET("api/admin/users")
    suspend fun getAdminUsers(): Response<JsonObject>

    @DELETE("api/admin/users/{id}")
    suspend fun deleteAdminUser(@Path("id") id: Long): Response<JsonObject>

    @POST("api/admin/users/{id}/toggle-active")
    suspend fun toggleUserActive(@Path("id") id: Long, @Body body: Map<String, Int>): Response<JsonObject>

    @GET("api/admin/auto-checkins")
    suspend fun getAdminAutoCheckins(): Response<JsonObject>

    @POST("api/admin/stop-checkin/{id}")
    suspend fun stopAdminCheckin(@Path("id") id: Long): Response<JsonObject>

    @GET("api/admin/user/{id}/logs")
    suspend fun getAdminUserLogs(@Path("id") id: Long): Response<JsonObject>

    @GET("api/auto-report")
    suspend fun getAutoReport(): Response<JsonObject>

    @POST("api/auto-report/start")
    suspend fun startAutoReport(@Body body: okhttp3.RequestBody): Response<JsonObject>

    @POST("api/auto-report/stop")
    suspend fun stopAutoReport(@Body body: okhttp3.RequestBody): Response<JsonObject>

    @GET("api/auto-report/logs")
    suspend fun getAutoReportLogs(@Query("type") type: String?): Response<JsonObject>

    @DELETE("api/auto-report/logs")
    suspend fun clearAutoReportLogs(@Query("type") type: String?): Response<JsonObject>

    @PUT("api/admin/users/{id}/advanced-mode")
    suspend fun setAdvancedMode(@Path("id") id: Long, @Body body: okhttp3.RequestBody): Response<JsonObject>

    @DELETE("api/admin/users/{id}/advanced-mode")
    suspend fun clearAdvancedMode(@Path("id") id: Long): Response<JsonObject>
}
