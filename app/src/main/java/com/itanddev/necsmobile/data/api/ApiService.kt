package com.itanddev.necsmobile.data.api

import com.itanddev.necsmobile.data.model.LoginRequest
import com.itanddev.necsmobile.data.model.LoginResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("Api/salesDeliveyApi/invoiceHeader/{invoiceId}")
    suspend fun getInvoiceDetails(
        @Path("invoiceId") invoiceId: String
    ): Response<ResponseBody>
}