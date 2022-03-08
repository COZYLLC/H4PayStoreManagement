package com.h4pay.store.networking

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.h4pay.store.BuildConfig
import com.h4pay.store.model.*
import com.h4pay.store.networking.tools.networkInterceptor
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

fun initService(): H4PayService {
    val client = OkHttpClient.Builder()
        .addNetworkInterceptor(networkInterceptor)
        .build()
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("${BuildConfig.API_URL}/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()
    return retrofit.create(H4PayService::class.java)
}

public interface H4PayService {
    // Store Status
    @GET("stores")
    suspend fun getStoreStatus():Boolean

    @POST("stores/change")
    suspend fun changeStoreStatus(@Body body:JsonObject) : Boolean

    // School
    @GET("schools")
    suspend fun getSchools(): List<School>

    @POST("schools/login")
    suspend fun schoolLogin(@Body body:JsonObject):Response<String>

    // Product
    @GET("products")
    suspend fun getProducts(): List<Product>

    // Order
    @GET("orders/filter")
    suspend fun getOrderDetail(@Query("orderId") orderId:String) : List<Order>

    @POST("orders/exchange")
    suspend fun exchangeOrder(@Body body:JsonObject)

    // Gift
    @GET("gifts/filter")
    suspend fun getGiftDetail(@Query("orderId") orderId: String): List<Gift>

    @POST("gifts/exchange")
    suspend fun exchangeGift(@Body body:JsonObject)

    //Voucher
    @GET("vouchers/filter")
    suspend fun getVoucherDetail(@Query("id") voucherId:String): List<Voucher>

    @POST("vouchers/exchange")
    suspend fun exchangeVoucher(@Body body:JsonObject)
}

data class ResponseWrapper<T>(
    var code:Int,
    @SerializedName("status") var status: Boolean,
    @SerializedName("result") var result: T? = null,
    @SerializedName("message") var message: String? = null
)