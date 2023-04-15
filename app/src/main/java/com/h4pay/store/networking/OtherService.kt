package com.h4pay.store.networking

import com.h4pay.store.model.Version
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PartMap

interface OtherService {


    @POST("uploads")
    @Multipart
    suspend fun submitSupport(@PartMap() partMap: LinkedHashMap<String, RequestBody>): Response<String>

    @GET("versions")
    suspend fun getVersionInfo(): Version
}