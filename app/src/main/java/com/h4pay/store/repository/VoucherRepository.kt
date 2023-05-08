package com.h4pay.store.repository

import com.google.gson.JsonObject
import com.h4pay.store.model.dto.ExchangeVoucherDto
import com.h4pay.store.model.Voucher
import com.h4pay.store.networking.RetrofitInstance


class VoucherRepository {
    suspend fun getVoucherDetail(orderId: String): List<Voucher> {
        return RetrofitInstance.service.getVoucherDetail(orderId)
    }

    suspend fun exchangePurchase(params: ExchangeVoucherDto) {
        return RetrofitInstance.service.exchangeVoucher(params)
    }
}