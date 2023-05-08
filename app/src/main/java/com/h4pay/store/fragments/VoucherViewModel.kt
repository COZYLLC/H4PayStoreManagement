package com.h4pay.store.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.h4pay.store.State
import com.h4pay.store.model.Product
import com.h4pay.store.model.Purchase
import com.h4pay.store.model.Voucher
import com.h4pay.store.model.dto.ExchangeVoucherDto
import com.h4pay.store.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VoucherViewModel : ViewModel() {
    private val _exchangeVoucherResultState = MutableStateFlow<State<Boolean>>(State.Initial())
    val exchangeVoucherResultState: StateFlow<State<Boolean>>
        get() = _exchangeVoucherResultState
    private val _voucherDetailState = MutableStateFlow<State<Voucher?>>(State.Initial())
    val voucherDetailState: StateFlow<State<Voucher?>>
        get() = _voucherDetailState

    fun exchangeVoucher(voucherId: String, item: JsonObject) {
        viewModelScope.launch {
            _exchangeVoucherResultState.emit(
                ExchangeVoucher().run(
                    ExchangeVoucherDto(
                        voucherId,
                        item
                    )
                )
            )
        }
    }

    fun getVoucherDetail(orderId: String) {
        viewModelScope.launch {
            _voucherDetailState.emit(GetVoucherDetail().run(orderId))
        }
    }
}