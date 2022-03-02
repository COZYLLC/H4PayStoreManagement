package com.h4pay.store.model

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.annotations.SerializedName
import com.h4pay.store.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val tokenKey = stringPreferencesKey("token")

class School (
    var token:String,
    @SerializedName("name")  val name:String,
    @SerializedName("id")  val id:String,
    @SerializedName("seller")  val seller:Seller,
) {

    suspend fun saveToStorage(context:Context) {
        Log.d("SchoolSave", this.token)
        context.dataStore.edit { store ->
            store[tokenKey] = this.token
        }
    }
}


fun tokenFromStorageFlow(context: Context): Flow<String?> {
    return context.dataStore.data.map { store ->
        store[tokenKey]
    }
}

suspend fun signOut(context: Context) {
    context.dataStore.edit { store ->
        store[tokenKey] = ""
    }
}
