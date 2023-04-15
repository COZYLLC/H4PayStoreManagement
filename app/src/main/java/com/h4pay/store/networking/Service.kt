package com.h4pay.store.networking

import com.h4pay.store.BuildConfig
import com.h4pay.store.networking.tools.networkInterceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 여러 서비스들을 합친 Service Interface 입니다.
 */
interface Service : StoreService, SchoolService, ProductService, OrderService, GiftService, VoucherService, OtherService

object RetrofitInstance {
    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addNetworkInterceptor(networkInterceptor)
        .build()
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("${BuildConfig.API_URL}/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    val service: Service by lazy {
        retrofit.create(Service::class.java)
    }
}