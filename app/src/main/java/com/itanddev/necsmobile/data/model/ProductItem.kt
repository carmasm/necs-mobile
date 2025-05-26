package com.itanddev.necsmobile.data.model

data class ProductItem(
    val productBarCode: String,
    val productName: String,
    val quantityOrder: Double,
    val quantityDelivery: Double,
    val productId: Int
)