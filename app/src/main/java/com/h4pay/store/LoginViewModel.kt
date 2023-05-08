package com.h4pay.store

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.h4pay.store.model.School
import com.h4pay.store.model.Version
import com.h4pay.store.model.dto.LoginDto
import com.h4pay.store.repository.PrefsRepository
import com.h4pay.store.usecase.GetSchools
import com.h4pay.store.usecase.GetVersionInfo
import com.h4pay.store.usecase.LoginSchool
import com.h4pay.store.util.Encryption
import com.h4pay.store.util.H4PayLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import retrofit2.Response

sealed class State<T> {
    class Initial<T> : State<T>()
    data class Success<T>(val data: T?) : State<T>()
    class Loading<T> : State<T>()
    data class Error<T>(val error: Throwable) : State<T>()

    fun isError() = this is Error
    fun isSuccess() = this is Success
}

class LoginViewModel(private val prefsRepository: PrefsRepository) : ViewModel() {
    private val _loginState: MutableStateFlow<State<Response<String>>> =
        MutableStateFlow(State.Initial())
    val loginState: StateFlow<State<Response<String>>>
        get() = _loginState

    private val _versionState: MutableStateFlow<State<Version>> = MutableStateFlow(State.Initial())
    val versionState: StateFlow<State<Version>>
        get() = _versionState

    private val _schoolsState: MutableStateFlow<State<List<School>>> =
        MutableStateFlow(State.Initial())
    val schoolsState: StateFlow<State<List<School>>>
        get() = _schoolsState


    fun fetchVersionInfo() {
        viewModelScope.launch {
            _versionState.emit(GetVersionInfo().run(null))
        }
    }

    fun fetchSchools() {
        viewModelScope.launch {
            _schoolsState.emit(GetSchools().run(null))
        }
    }


    fun login(schoolId: String?, passwordRawString: String?) {
        if (schoolId == null || passwordRawString == null) {
            return
        }
        val encryptedPassword = Encryption.encryptToSha256Md5(passwordRawString)
        var sessionId = Encryption.generateSessionId()
        sessionId = Encryption.encryptToSha256Md5(sessionId)

        viewModelScope.launch {
            _loginState.emit(
                LoginSchool().run(
                    LoginDto(
                        schoolId,
                        encryptedPassword,
                        sessionId
                    )
                )
            )

        }
    }

}

class LoginViewModelFactory(private val prefsRepository: PrefsRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(prefsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}