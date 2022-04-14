package com.h4pay.store.model

import com.google.gson.annotations.SerializedName

data class Version(
    @SerializedName("version") val versionName: String,
    @SerializedName("versionCode") val versionCode:Double,
    @SerializedName("changes") val changes: String,
    @SerializedName("url") val url: String
)