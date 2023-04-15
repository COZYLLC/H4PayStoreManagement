package com.h4pay.store.networking

import com.google.gson.JsonObject
import com.h4pay.store.model.Order
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface OrderService {

    // Order
    @GET("orders/filter")
    suspend fun getOrderDetail(@Query("orderId") orderId: String): List<Order>

    @POST("orders/exchange")
    suspend fun exchangeOrder(@Body body: JsonObject)

}