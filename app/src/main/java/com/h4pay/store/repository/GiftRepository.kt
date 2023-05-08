package com.h4pay.store.repository

import com.google.gson.JsonObject
import com.h4pay.store.model.Gift
import com.h4pay.store.model.Order
import com.h4pay.store.model.dto.ExchangeRequestDto
import com.h4pay.store.model.dto.ReqBody
import com.h4pay.store.networking.RetrofitInstance
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


class GiftRepository : PurchaseRepository<Gift>() {
    override suspend fun getPurchaseDetail(orderId: String): List<Gift> {
        return RetrofitInstance.service.getGiftDetail(orderId)
    }

    override suspend fun exchangePurchase(orderIds: ArrayList<String>) {
        return RetrofitInstance.service.exchangeGift(ExchangeRequestDto(orderIds))
    }
}