package com.h4pay.store.model

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val tokenKey = stringPreferencesKey("token")

class School(
    var token: String,
    @SerializedName("name") val name: String,
    @SerializedName("id") val id: String,
    @SerializedName("seller") val seller: Seller,
)