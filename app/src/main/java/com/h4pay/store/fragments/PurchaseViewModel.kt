package com.h4pay.store.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.h4pay.store.State
import com.h4pay.store.model.Product
import com.h4pay.store.model.Purchase
import com.h4pay.store.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PurchaseViewModel : ViewModel() {
    private val _exchangeState = MutableStateFlow<State<Boolean>>(State.Initial())
    val exchangeState: StateFlow<State<Boolean>>
        get() = _exchangeState
    private val _purchaseDetailState = MutableStateFlow<State<Purchase?>>(State.Initial())
    val purchaseDetailState: StateFlow<State<Purchase?>>
        get() = _purchaseDetailState
    private val _productListState = MutableStateFlow<State<List<Product>>>(State.Initial())
    val productListState: StateFlow<State<List<Product>>>
        get() = _productListState

    fun exchangeGift(orderIds: ArrayList<String>) {
        viewModelScope.launch {
            _exchangeState.emit(ExchangeGift().run(orderIds))
        }
    }

    fun exchangeOrder(orderIds: ArrayList<String>) {
        viewModelScope.launch {
            _exchangeState.emit(ExchangeOrder().run(orderIds))
        }
    }

    fun getOrderDetail(orderId: String) {
        viewModelScope.launch {
            _purchaseDetailState.emit(GetOrderDetail().run(orderId) as State<Purchase?>)
        }
    }

    fun getProducts() {
        viewModelScope.launch {
            _productListState.emit(GetProductList().run(null))
        }
    }

    fun getGiftDetail(orderId: String) {
        viewModelScope.launch {
            _purchaseDetailState.emit(GetOrderDetail().run(orderId) as State<Purchase?>)
        }
    }
}