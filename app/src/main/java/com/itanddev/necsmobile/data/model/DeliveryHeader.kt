package com.itanddev.necsmobile.data.model

data class DeliveryHeader(
    val salesDeliveryOrderId: Int,
    val deliveryNumber: String,
    val dateCreated: String,
    val customerName: String,
    val soNumber: String,
    val status: String,
    val branchId: Int?,
    val salesInvoiceID: Int?,
    val detail: List<ProductItem>,
    var detailLocations: List<SalesDeliveryDetailLocationsModel>
)