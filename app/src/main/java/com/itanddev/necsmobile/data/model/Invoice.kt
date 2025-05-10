package com.itanddev.necsmobile.data.model

data class Invoice(
    val customerName: String,
    val total: Double,
    val status: String,
    val invoiceDate: String
)