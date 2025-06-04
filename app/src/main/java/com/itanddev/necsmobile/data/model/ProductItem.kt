package com.itanddev.necsmobile.data.model

data class ProductItem(
    val salesDeliveryDetailid: Int,
    val productBarCode: String,
    val productName: String,
    val quantityOrder: Double,
    var quantityDelivery: Double,
    val productId: Int
)