package com.h4pay.store.model

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.util.*

data class Order(
    @SerializedName("uid") override val uid: String,
    override val uidfrom: String? = null,
    override val uidto: String? = null,
    @SerializedName("orderId") override val orderId: String,
    @SerializedName("amount") override val amount: Int,
    @SerializedName("date") override val date: Date,
    @SerializedName("expire") override val expire: Date,
    @SerializedName("item") override val item: JsonObject,
    @SerializedName("exchanged") override val exchanged: Boolean
) : Purchase

data class Gift(
    override val uid: String? = null,
    @SerializedName("uidfrom") override val uidfrom: String,
    @SerializedName("uidto") override val uidto: String,
    @SerializedName("orderId") override val orderId: String,
    @SerializedName("amount") override val amount: Int,
    @SerializedName("date") override val date: Date,
    @SerializedName("expire") override val expire: Date,
    @SerializedName("item") override val item: JsonObject,
    @SerializedName("exchanged") override val exchanged: Boolean
) : Purchase

interface Purchase {
    val uid: String?
    val uidto: String?
    val uidfrom: String?
    val orderId: String
    val amount: Int
    val date: Date
    val expire: Date
    val item: JsonObject
    val exchanged: Boolean
}