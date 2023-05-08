package com.h4pay.store.usecase

import com.h4pay.store.App
import com.h4pay.store.model.Product

class ChangeStoreStatus : ResultUseCase<Boolean, Boolean>() {
    override suspend fun onExecute(params: Boolean): Boolean {
        // 에러가 나면 ResultUseCase 의 run()이 잡아요.
        return App.schoolRepository.changeStoreStatus(params)
    }
}