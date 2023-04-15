package com.h4pay.store.networking

import com.google.gson.JsonObject
import com.h4pay.store.model.School
import com.h4pay.store.model.dto.LoginDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SchoolService {


    // School
    @GET("schools")
    suspend fun getSchools(): List<School>

    @POST("schools/login")
    suspend fun schoolLogin(@Body body: LoginDto): Response<String>

}