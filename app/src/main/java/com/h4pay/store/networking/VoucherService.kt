package com.h4pay.store.networking

import com.google.gson.JsonObject
import com.h4pay.store.model.Voucher
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface VoucherService {

    //Voucher
    @GET("vouchers/filter")
    suspend fun getVoucherDetail(@Query("id") voucherId: String): List<Voucher>

    @POST("vouchers/exchange")
    suspend fun exchangeVoucher(@Body body: JsonObject)

}