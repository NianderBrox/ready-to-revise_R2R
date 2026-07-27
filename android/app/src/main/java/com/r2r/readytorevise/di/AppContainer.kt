package com.r2r.readytorevise.di

import android.content.Context
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.r2r.readytorevise.data.local.TokenManager
import com.r2r.readytorevise.data.remote.AuthApi
import com.r2r.readytorevise.data.remote.NetworkConstants
import com.r2r.readytorevise.data.repository.AuthRepositoryImpl
import com.r2r.readytorevise.domain.repository.AuthRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

interface AppContainer {
    val authRepository: AuthRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
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

    private val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    private val tokenManager: TokenManager by lazy {
        TokenManager(context)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(authApi, tokenManager)
    }
}
