package com.h4pay.store.model

import com.google.gson.annotations.SerializedName

data class ResponseWrapper<T>(
    var code: Int,
    @SerializedName("status") var status: Boolean,
    @SerializedName("result") var result: T? = null,
    @SerializedName("message") var message: String? = null
)