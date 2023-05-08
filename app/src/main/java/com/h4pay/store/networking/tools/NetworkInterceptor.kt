package com.h4pay.store.networking.tools

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.h4pay.store.App.Companion.gson
import com.h4pay.store.App.Companion.token
import com.h4pay.store.model.ResponseWrapper
import com.h4pay.store.util.H4PayLogger
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody


val networkInterceptor = Interceptor { chain ->
    val copy = chain.request()
    val buffer = okio.Buffer()
    copy.body?.writeTo(buffer)
    H4PayLogger.d("Interceptor", token.toString())
    val request = chain.request()
        .newBuilder()
        .addHeader("x-access-token", token ?: "")
        .build()
    // Request
    chain.proceed(request)
}

val responseInterceptor = Interceptor { chain ->
    // Get raw json response
    val request = chain.request()
    // Request
    val response = chain.proceed(request)
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
    newResponse
}