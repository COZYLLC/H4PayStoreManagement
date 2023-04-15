package com.h4pay.store.repository

import com.h4pay.store.model.Version
import com.h4pay.store.networking.RetrofitInstance
import okhttp3.RequestBody
import retrofit2.Response

class OtherRepository {
    suspend fun submitSupport(partMap: LinkedHashMap<String, RequestBody>): Response<String> {
        return RetrofitInstance.service.submitSupport(partMap)
    }

    suspend fun getVersionInfo(): Version {
        return RetrofitInstance.service.getVersionInfo()
    }
}