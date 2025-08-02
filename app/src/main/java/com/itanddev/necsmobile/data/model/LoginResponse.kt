package com.itanddev.necsmobile.data.model

data class LoginResponse(
    val type: String,
    val title: String,
    val message: String,
    val trace: String,
    val comodin: String,
    val documentId: Int
)