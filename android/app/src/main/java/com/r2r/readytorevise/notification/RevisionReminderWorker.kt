package com.r2r.readytorevise.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.r2r.readytorevise.data.local.TokenManager
import com.r2r.readytorevise.data.remote.DashboardApi
import com.r2r.readytorevise.data.remote.NetworkConstants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

class RevisionReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = NotificationPreferences(applicationContext)
        val enabled = prefs.notificationsEnabled.first()
        if (!enabled) return Result.success()

        return try {
            val count = fetchDueCount()
            if (count > 0) {
                NotificationHelper.sendRevisionDueNotification(applicationContext, count)
            } else {
                NotificationHelper.sendNoRevisionNotification(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun fetchDueCount(): Int {
        val tokenManager = TokenManager(applicationContext)
        val token = tokenManager.token.first() ?: return 0

        val json = Json { ignoreUnknownKeys = true }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(NetworkConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val dashboardApi = retrofit.create(DashboardApi::class.java)
        val response = dashboardApi.getDashboard()
        return response.data.reviews.slippingSoon
    }
}
