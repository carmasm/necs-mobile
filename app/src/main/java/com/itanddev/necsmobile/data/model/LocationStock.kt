package com.itanddev.necsmobile.data.model

data class LocationStock(
    val productId: Int,
    val wharehouseId: Int,
    val locationId: Int,
    var deliveryLocationId: Int = -1,
    var salesDeliveryDetailid: Int,
    val wharehouseName: String,
    val locationName: String,
    val productName: String,
    val quantityStock: Double,
    var quantityDelivery: Double = 0.0  // user‐entered
)
