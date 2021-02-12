package com.h4pay.store.networking.tools

import android.util.Log
import com.h4pay.store.BuildConfig
import java.io.File

import okhttp3.Call; import okhttp3.Callback; import okhttp3.MultipartBody; import okhttp3.OkHttpClient; import okhttp3.Request; import okhttp3.RequestBody; import okhttp3.Response;
import java.io.IOException

class uploadImage(){
    private val TAG = "uploadImage"

    fun upload(pdName:String, pdPrice:String, pdDesc:String, file: File){

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("productName", pdName)
            .addFormDataPart("desc", pdDesc)
            .addFormDataPart("price", pdPrice)
            .addFormDataPart("file", UnicodeKorean.main(pdName) + ".png", RequestBody.create(MultipartBody.FORM, file))
            .build()

        val request = Request.Builder()
            .url("${BuildConfig.API_URL}/product/add")
            .addHeader("Content-Type", "multipart/form-data; charset=utf-8")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object:Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d(TAG, response.body!!.string())
            }

        })
    }
}