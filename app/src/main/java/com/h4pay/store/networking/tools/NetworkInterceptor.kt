package com.h4pay.store.networking.tools

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.h4pay.store.dataStore
import com.h4pay.store.model.tokenFromStorageFlow
import com.h4pay.store.networking.ResponseWrapper
import com.h4pay.store.token
import kotlinx.coroutines.flow.collect
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

private val TAG = "NetworkInterceptor"


val networkInterceptor = Interceptor { chain ->
    val gson = Gson()
    val request = chain.request().newBuilder()
        .addHeader("x-access-token", token ?: "")
        .build()
    // Request
    val response = chain.proceed(request)
    Log.d(TAG, response.toString())
    // Get raw json response
    val rawJsonResponse = response.body?.string() ?: "{}"

    // Convert json to data object
    val type = object : TypeToken<ResponseWrapper<*>>() {}.type
    val res = try {
        val result = gson.fromJson<ResponseWrapper<*>>(rawJsonResponse, type)
            ?: throw JsonParseException("Failed to parse json")
        if (!result.status) ResponseWrapper<Any>(response.code, false) else result
    } catch (e: JsonParseException) {
        ResponseWrapper(-900, false, "Failed to parse json")
    } catch (t: Throwable) {
        ResponseWrapper(-901, false, "Unknown Error")
    }

    // Re-transform result to json and return
    val resultJson = gson.toJson(res.result)
    val newResponse = response.newBuilder()
        .message(res.message ?: response.message)
        .body(resultJson.toResponseBody())
        .build()
    Log.d(TAG, newResponse.toString())
    newResponse
}