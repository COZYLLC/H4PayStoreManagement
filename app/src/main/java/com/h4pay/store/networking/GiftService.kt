package com.h4pay.store.networking

import com.google.gson.JsonObject
import com.h4pay.store.model.Gift
import com.h4pay.store.model.dto.ExchangeRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface GiftService {

    // Gift
    @GET("gifts/filter")
    suspend fun getGiftDetail(@Query("orderId") orderId: String): List<Gift>

    @POST("gifts/exchange")
    suspend fun exchangeGift(@Body body: ExchangeRequestDto)

}