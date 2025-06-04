package com.itanddev.necsmobile.data.api

import com.itanddev.necsmobile.data.model.LoginRequest
import com.itanddev.necsmobile.data.model.LoginResponse
import com.itanddev.necsmobile.data.model.SalesDeliveryOrderModel
import com.itanddev.necsmobile.data.model.SaveDeliveryResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("Api/salesDeliveyApi/invoiceHeader/{invoiceId}")
    suspend fun getInvoiceDetails(
        @Path("invoiceId") invoiceId: String
    ): Response<ResponseBody>

    @GET("Api/salesDeliveyApi/getSDByInvoiceId")
    suspend fun getDeliveriesByInvoice(
        @Query("invoiceId") invoiceId: String
    ): Response<ResponseBody>

    @GET("Api/salesDeliveyApi/getDeliveryByInvoiceId")
    suspend fun getDeliveryDetailByInvoice(
        @Query("invoiceId") invoiceId: String
    ): Response<ResponseBody>

    @GET("Api/salesDeliveyApi/getDeliveryById")
    suspend fun getDeliveryById(
        @Query("id") deliveryId: String
    ): Response<ResponseBody>

    @GET("Api/salesDeliveyApi/getWharehouses")
    suspend fun getWarehouses(
        @Query("branchId") branchId: String
    ): Response<ResponseBody>

    @GET("Api/salesDeliveyApi/getProductLocaionStock")
    suspend fun getProductLocationStock(
        @Query("pid") productId: String,
        @Query("wId") warehouseId: String,
        @Query("deliveryId") deliveryId: Int?
    ): Response<ResponseBody>

    @PUT("Api/salesDeliveyApi/saveDelivery")
    suspend fun saveDelivery(
        @Body request: SalesDeliveryOrderModel
    ): Response<SaveDeliveryResponse>
}