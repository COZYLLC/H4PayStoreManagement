package com.h4pay.store.model

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.util.*

data class Voucher (
    @SerializedName("id")  val id:String,
    @SerializedName("issuer")  val issuer:UserData,
    @SerializedName("receiver")  val receiver:UserData,
    @SerializedName("amount")  val amount:Int,
    @SerializedName("message")  val message:String,
    @SerializedName("date")  val date: Date,
    @SerializedName("expire")  val expire:Date,
    @SerializedName("exchanged")  val exchanged:Boolean,
    @SerializedName("item")  val item:JsonObject?
    )

data class UserData(
    @SerializedName("name")  val name:String,
    @SerializedName("tel")  val tel:String,
)