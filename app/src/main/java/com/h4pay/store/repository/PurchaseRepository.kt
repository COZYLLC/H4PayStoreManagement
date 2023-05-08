package com.h4pay.store.repository

abstract class PurchaseRepository<T> {
    abstract suspend fun getPurchaseDetail(orderId: String): List<T>

    abstract suspend fun exchangePurchase(orderIds: ArrayList<String>)
}