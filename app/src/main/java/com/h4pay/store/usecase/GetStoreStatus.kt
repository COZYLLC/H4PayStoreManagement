package com.h4pay.store.usecase

import com.h4pay.store.App
import com.h4pay.store.model.Product

class GetStoreStatus : ResultUseCase<Nothing?, Boolean>() {
    override suspend fun onExecute(params: Nothing?): Boolean {
        // 에러가 나면 ResultUseCase 의 run()이 잡아요.
        return App.schoolRepository.getStoreStatus()
    }
}