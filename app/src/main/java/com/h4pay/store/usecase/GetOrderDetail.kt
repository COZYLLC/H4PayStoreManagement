package com.h4pay.store.usecase

import com.h4pay.store.App
import com.h4pay.store.model.Order
import com.h4pay.store.model.School

class GetOrderDetail : ResultUseCase<String, Order?>() {
    override suspend fun onExecute(params: String): Order? {
        // 에러가 나면 ResultUseCase 의 run()이 잡아요.
        val orders = App.orderRepository.getPurchaseDetail(params)
        return if (orders.size == 1) orders[0] else null
    }
}