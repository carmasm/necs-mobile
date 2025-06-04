package com.itanddev.necsmobile.data.model

import com.google.gson.annotations.SerializedName

data class SaveDeliveryResponse(
    @SerializedName("type")
    val type: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("message")
    val message: String,

    @SerializedName("trace")
    val trace: String?,

    @SerializedName("comodin")
    val comodin: String?,

    @SerializedName("documentId")
    val documentId: Int
)