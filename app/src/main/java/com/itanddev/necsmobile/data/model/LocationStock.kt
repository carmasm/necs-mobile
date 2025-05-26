package com.itanddev.necsmobile.data.model

data class LocationStock(
    val wharehouseName: String,
    val locationName: String,
    val productName: String,
    val quantityStock: Double,
    var quantityDelivery: Double = 0.0  // user‐entered
)
