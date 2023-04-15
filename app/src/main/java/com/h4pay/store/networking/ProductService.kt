package com.h4pay.store.networking

import com.h4pay.store.model.Product
import retrofit2.http.GET

interface ProductService {

    // Product
    @GET("products")
    suspend fun getProducts(): List<Product>
}