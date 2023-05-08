package com.h4pay.store.usecase

import com.h4pay.store.App
import com.h4pay.store.model.Order
import com.h4pay.store.model.Voucher

class GetVoucherDetail : ResultUseCase<String, Voucher?>() {
    override suspend fun onExecute(params: String): Voucher? {
        // 에러가 나면 ResultUseCase 의 run()이 잡아요.
        val vouchers = App.voucherRepository.getVoucherDetail(params)
        return if (vouchers.size == 1) vouchers[0] else null
    }
}