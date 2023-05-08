package com.h4pay.store.repository

import com.h4pay.store.model.Product
import com.h4pay.store.networking.RetrofitInstance
import okhttp3.RequestBody
import retrofit2.Response

class ProductRepository {
    suspend fun getProductList(): List<Product> {
        return RetrofitInstance.service.getProducts()
    }
}