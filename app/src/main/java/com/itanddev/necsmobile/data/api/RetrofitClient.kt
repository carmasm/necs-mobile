package com.itanddev.necsmobile.data.api

import android.content.Context
import com.itanddev.necsmobile.App
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val MOCAB_URL = "https://mocabapi.ddns.net/"
    private const val GITHUB_API_URL = "https://api.github.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
            // Add common headers here if needed
            chain.proceed(request.build())
        }.build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(MOCAB_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private val necsOkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
            // Add common headers here if needed
            chain.proceed(request.build())
        }.build()

    val necsApiService: ApiService
        get() {
            val baseUrl = Prefs.getBaseUrl(App.instance) // see note about App
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(necsOkHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }

    private val gitHubClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "NECSMobile") // GitHub requires User-Agent
                .build()
            chain.proceed(request)
        }.build()

    val gitHubApiService: GitHubApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GITHUB_API_URL)
            .client(gitHubClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApiService::class.java)
    }
}