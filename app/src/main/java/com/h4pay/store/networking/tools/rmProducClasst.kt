package com.h4pay.store.networking.tools

import android.util.Log
import com.h4pay.store.BuildConfig
import okhttp3.*
import java.io.IOException
import java.lang.Exception

class rmProductClass {
    private val TAG = "rmProduct"
    fun rm(pID:Int){
        val client  = OkHttpClient()
        val formBody = FormBody.Builder()
            .add("id", pID.toString())
            .build()
        val request = Request.Builder()
            .url("${BuildConfig.API_URL}/product/remove")
            .post(formBody)
            .build()
        try {
            val response = client.newCall(request).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d(TAG, response.body!!.string())
                }

            })
        } catch(e:Exception) {
            e.printStackTrace()
        }
    }
}