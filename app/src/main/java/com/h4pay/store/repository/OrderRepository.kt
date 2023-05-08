package com.h4pay.store.repository

import com.h4pay.store.model.Order
import com.h4pay.store.model.dto.ExchangeRequestDto
import com.h4pay.store.networking.RetrofitInstance
import retrofit2.http.Query


class OrderRepository : PurchaseRepository<Order>() {
    override suspend fun getPurchaseDetail(orderId: String): List<Order> {
        return RetrofitInstance.service.getOrderDetail(orderId)
    }

    override suspend fun exchangePurchase(orderIds: ArrayList<String>) {
        return RetrofitInstance.service.exchangeOrder(ExchangeRequestDto(orderIds))
    }
}