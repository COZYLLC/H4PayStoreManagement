package com.h4pay.store.repository

import com.google.gson.JsonObject
import com.h4pay.store.model.School
import com.h4pay.store.model.dto.LoginDto
import com.h4pay.store.networking.RetrofitInstance
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

class SchoolRepository {
    suspend fun getSchools(): List<School> {
        return RetrofitInstance.service.getSchools()
    }

    suspend fun schoolLogin(data: LoginDto): Response<String> {
        return RetrofitInstance.service.schoolLogin(data)
    }
}