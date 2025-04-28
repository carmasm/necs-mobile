package com.itanddev.necsmobile.data.api

import com.itanddev.necsmobile.data.model.LoginRequest
import com.itanddev.necsmobile.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Add other API endpoints here
}