package com.h4pay.store.model

import com.google.gson.annotations.SerializedName


class Seller (
    @SerializedName("founderName") val founderName:String,
    @SerializedName("address")  val address:String,
    @SerializedName("businessId")  val businessId:String,
    @SerializedName("sellerId")  val sellerId:String,
    @SerializedName("tel")  val tel:String
)