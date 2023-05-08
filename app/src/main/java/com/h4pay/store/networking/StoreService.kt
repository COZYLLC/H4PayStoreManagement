package com.h4pay.store.networking

import com.google.gson.JsonObject
import com.h4pay.store.model.dto.ChangeStoreStatusDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface StoreService {
    // Store Status
    @GET("stores")
    suspend fun getStoreStatus(): Boolean

    @POST("stores/change")
    suspend fun changeStoreStatus(@Body changeStatusDto: ChangeStoreStatusDto): Boolean
}