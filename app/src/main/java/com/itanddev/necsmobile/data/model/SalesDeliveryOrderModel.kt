package com.itanddev.necsmobile.data.model

import com.google.gson.annotations.SerializedName


/**
 * Matches exactly the JSON your PUT expects:
 *   - “detail” is the array of ProductItem (with updated quantityDelivery).
 *   - “detailLocations” is the list of individual location entries the user entered.
 */
data class SalesDeliveryOrderModel (
    @SerializedName("salesDeliveryOrderId")
    val salesDeliveryOrderId: Int,

    @SerializedName("deliveryNumber")
    val deliveryNumber: String,

    @SerializedName("salesQuoteId")
    val salesQuoteId: Int?,

    @SerializedName("salesinInvoceId")
    val salesinInvoceId: Int?,

    @SerializedName("dateCreated")
    val dateCreated: String, // ISO string, e.g. "2023-10-15T08:30:00"

    @SerializedName("createdBy")
    val createdBy: Int,

    @SerializedName("deliveryDate")
    val deliveryDate: String, // ISO string

    @SerializedName("customerId")
    val customerId: Int,

    @SerializedName("enterpriseId")
    val enterpriseId: Int,

    @SerializedName("branchId")
    val branchId: Int?,

    @SerializedName("docType")
    val docType: String,

    @SerializedName("customerName")
    val customerName: String,

    @SerializedName("soNumber")
    val soNumber: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("soStatus")
    val soStatus: String,

    @SerializedName("salesInvoiceID")
    val salesInvoiceID: Int?,

    @SerializedName("wharehouseId")
    val wharehouseId: Int,

    @SerializedName("hasBackOrder")
    val hasBackOrder: Boolean,

    @SerializedName("detail")
    val detail: List<SalesDeliveryDetailModel>,

    @SerializedName("detailLocations")
    val detailLocations: List<SalesDeliveryDetailLocationsModel>?
)

/**
 * Each product (dispatch detail) in the “detail” array. We only need fields that back-end expects.
 * You already have ProductItem; you can either reuse it (adding @SerializedName for missing fields)
 * or define a smaller wrapper. Below is a minimal version.
 */
data class SalesDeliveryDetailModel(
    @SerializedName("salesDeliveryDetailid")
    val salesDeliveryDetailid: Int,

    @SerializedName("salesOrderDeliveryId")
    val salesOrderDeliveryId: Int,

    @SerializedName("productId")
    val productId: Int,

    @SerializedName("quantityOrder")
    val quantityOrder: Double,

    @SerializedName("quantityInvoice")
    val quantityInvoice: Double,

    @SerializedName("quantityDelivery")
    val quantityDelivery: Double,

    @SerializedName("wharehouseName")
    val wharehouseName: String,

    @SerializedName("locationName")
    val locationName: String,

    @SerializedName("deteDelivery")
    val deteDelivery: String?,

    @SerializedName("wharehouseId")
    val wharehouseId: Int,

    @SerializedName("locationId")
    val locationId: Int,

    @SerializedName("productName")
    val productName: String,

    @SerializedName("productBarCode")
    val productBarCode: String,

    @SerializedName("cost")
    val cost: Double,

    @SerializedName("quantityCheck")
    val quantityCheck: Double,

    @SerializedName("image")
    val image: String?,

    @SerializedName("isLoan")
    val isLoan: Int,

    @SerializedName("quantityStock")
    val quantityStock: Double
)

data class SalesDeliveryDetailLocationsModel(
    @SerializedName("deliveryLocationId")
    val deliveryLocationId: Int, // you can send `-1` if new

    @SerializedName("salesOrderDeliveryId")
    val salesOrderDeliveryId: Int,

    @SerializedName("salesDeliveryDetailid")
    val salesDeliveryDetailid: Int,

    @SerializedName("wharehouseId")
    val wharehouseId: Int,

    @SerializedName("locationId")
    val locationId: Int,

    @SerializedName("productId")
    val productId: Int,

    @SerializedName("quantity")
    val quantity: Double
)

data class ConfirmDeliveryRequest(
    @SerializedName("model")
    val model: SalesDeliveryOrderModel,
    @SerializedName("model2")
    val model2: SalesDeliveryOrderModel
)