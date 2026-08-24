package com.r2r.readytorevise.di

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.r2r.readytorevise.data.local.TokenManager
import com.r2r.readytorevise.data.remote.AuthApi
import com.r2r.readytorevise.data.remote.DashboardApi
import com.r2r.readytorevise.data.remote.DocumentsApi
import com.r2r.readytorevise.data.remote.NetworkConstants
import com.r2r.readytorevise.data.remote.RecommendationsApi
import com.r2r.readytorevise.data.remote.ReviewsApi
import com.r2r.readytorevise.data.remote.StudyItemsApi
import com.r2r.readytorevise.data.repository.AuthRepositoryImpl
import com.r2r.readytorevise.data.repository.DashboardRepositoryImpl
import com.r2r.readytorevise.data.repository.DocumentsRepositoryImpl
import com.r2r.readytorevise.data.repository.RecommendationsRepositoryImpl
import com.r2r.readytorevise.data.repository.ReviewsRepositoryImpl
import com.r2r.readytorevise.data.repository.StudyItemsRepositoryImpl
import com.r2r.readytorevise.domain.repository.AuthRepository
import com.r2r.readytorevise.domain.repository.DashboardRepository
import com.r2r.readytorevise.domain.repository.DocumentsRepository
import com.r2r.readytorevise.domain.repository.RecommendationsRepository
import com.r2r.readytorevise.domain.repository.ReviewsRepository
import com.r2r.readytorevise.domain.repository.StudyItemsRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit

interface AppContainer {
    val authRepository: AuthRepository
    val studyItemsRepository: StudyItemsRepository
    val reviewsRepository: ReviewsRepository
    val recommendationsRepository: RecommendationsRepository
    val documentsRepository: DocumentsRepository
    val dashboardRepository: DashboardRepository
    val imageLoader: coil.ImageLoader
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val tokenManager: TokenManager by lazy {
        TokenManager(context)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = runBlocking { tokenManager.token.first() }

            val original = chain.request()

            val request = original.newBuilder()
                .apply {
                    if (token != null) {
                        header("Authorization", "Bearer $token")
                    }
                }
                .header("X-Tunnel-Skip-AntiPhishing-Page", "true")
                .build()

            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(NetworkConstants.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val studyItemsApi: StudyItemsApi by lazy {
        retrofit.create(StudyItemsApi::class.java)
    }

    val reviewsApi: ReviewsApi by lazy {
        retrofit.create(ReviewsApi::class.java)
    }

    val recommendationsApi: RecommendationsApi by lazy {
        retrofit.create(RecommendationsApi::class.java)
    }

    val documentsApi: DocumentsApi by lazy {
        retrofit.create(DocumentsApi::class.java)
    }

    val dashboardApi: DashboardApi by lazy {
        retrofit.create(DashboardApi::class.java)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(authApi, tokenManager)
    }

    override val studyItemsRepository: StudyItemsRepository by lazy {
        StudyItemsRepositoryImpl(studyItemsApi, tokenManager)
    }

    override val reviewsRepository: ReviewsRepository by lazy {
        ReviewsRepositoryImpl(reviewsApi, tokenManager)
    }

    override val recommendationsRepository: RecommendationsRepository by lazy {
        RecommendationsRepositoryImpl(recommendationsApi, tokenManager)
    }

    override val documentsRepository: DocumentsRepository by lazy {
        DocumentsRepositoryImpl(documentsApi, tokenManager)
    }

    override val dashboardRepository: DashboardRepository by lazy {
        DashboardRepositoryImpl(dashboardApi, tokenManager)
    }

    override val imageLoader: coil.ImageLoader by lazy {
        coil.ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .build()
    }
}
