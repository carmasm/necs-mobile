package com.itanddev.necsmobile.data.model

import com.google.gson.annotations.SerializedName

data class Warehouse(
    @SerializedName("WharehoseID") val warehouseId: Int,
    @SerializedName("WharehouseName") val name: String
)