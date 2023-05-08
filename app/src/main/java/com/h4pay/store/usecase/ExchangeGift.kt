package com.h4pay.store.usecase

import com.h4pay.store.App
import com.h4pay.store.model.School

class ExchangeGift : ResultUseCase<ArrayList<String>, Boolean>() {
    override suspend fun onExecute(params: ArrayList<String>): Boolean {
        // 에러가 나면 ResultUseCase 의 run()이 잡아요.
        App.giftRepository.exchangePurchase(params)
        return true
    }
}