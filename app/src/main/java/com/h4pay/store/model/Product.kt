package com.h4pay.store.model

import com.google.gson.annotations.SerializedName

data class Product(
    @SerializedName("id")  val id:Int,
    @SerializedName("productName")  val productName:String,
    @SerializedName("price")  val price:Int,
    @SerializedName("desc")  val desc:String,
    @SerializedName("img")  val img:String,
    @SerializedName("soldout")  val soldout:Boolean,
    @SerializedName("barcode")  val barcode:String,
    @SerializedName("isVisible")  val isVisible:Boolean,
)