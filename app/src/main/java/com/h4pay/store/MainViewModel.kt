package com.h4pay.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.h4pay.store.repository.PrefsRepository
import com.h4pay.store.usecase.ChangeStoreStatus
import com.h4pay.store.usecase.GetStoreStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel() : ViewModel() {
    private val _storeStatus: MutableStateFlow<State<Boolean>> = MutableStateFlow(State.Initial())
    val storeStatus: StateFlow<State<Boolean>>
        get() = _storeStatus

    fun getStoreStatus() {
        viewModelScope.launch {
            _storeStatus.emit(GetStoreStatus().run(null))
        }
    }

    fun changeStoreStatus(isOpened: Boolean) {
        viewModelScope.launch {
            _storeStatus.emit(ChangeStoreStatus().run(isOpened))
        }
    }
}