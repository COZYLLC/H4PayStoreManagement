package com.h4pay.store.usecase

import com.h4pay.store.App
import com.h4pay.store.State
import com.h4pay.store.model.dto.LoginDto
import retrofit2.Response

class LoginSchool : ResultUseCase<LoginDto, Response<String>>() {
    override suspend fun onExecute(params: LoginDto): Response<String> {
        return App.schoolRepository.schoolLogin(params)
    }

}