package com.h4pay.store.model

import com.google.gson.annotations.SerializedName

class Version(
    @SerializedName("version") val versionName: Double,
    @SerializedName("changes") val changes: String,
    @SerializedName("url") val url: String
)