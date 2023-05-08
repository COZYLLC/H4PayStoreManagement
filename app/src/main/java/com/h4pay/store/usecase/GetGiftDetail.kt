package com.h4pay.store.usecase

import com.h4pay.store.App
import com.h4pay.store.model.Gift
import com.h4pay.store.model.School

class GetGiftDetail : ResultUseCase<String, Gift?>() {
    override suspend fun onExecute(params: String): Gift? {
        // 에러가 나면 ResultUseCase 의 run()이 잡아요.
        val gifts = App.giftRepository.getPurchaseDetail(params)
        return if (gifts.size == 1) gifts[0] else null
    }
}