package com.h4pay.store.usecase

import com.h4pay.store.App
import com.h4pay.store.model.School
import com.h4pay.store.model.dto.ExchangeVoucherDto

class ExchangeVoucher : ResultUseCase<ExchangeVoucherDto, Boolean>() {
    override suspend fun onExecute(params: ExchangeVoucherDto): Boolean {
        // 에러가 나면 ResultUseCase 의 run()이 잡아요.
        App.voucherRepository.exchangePurchase(params)
        return true
    }
}