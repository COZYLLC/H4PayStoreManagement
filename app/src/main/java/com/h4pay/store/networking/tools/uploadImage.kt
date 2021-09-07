package com.h4pay.store.networking.tools

import android.util.Log
import com.h4pay.store.BuildConfig
import java.io.File

import okhttp3.Call; import okhttp3.Callback; import okhttp3.MultipartBody; import okhttp3.OkHttpClient; import okhttp3.Request; import okhttp3.RequestBody; import okhttp3.Response;
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.CountDownLatch

class uploadImage(){
    private val TAG = "uploadImage"

    fun upload(pdName:String, pdPrice:String, pdDesc:String, file: File): Boolean {

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
        var countDownLatch = CountDownLatch(1)
        var isSuccess = false
        try {

            val client = OkHttpClient()
            client.newCall(request).enqueue(object:Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    isSuccess = false
                    countDownLatch.countDown()
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.code == 200){
                        isSuccess = JSONObject(response.body!!.string()).getBoolean("status")
                        countDownLatch.countDown()
                    }
                }

            })
        } catch (e:Exception){
            e.printStackTrace()
        }
        countDownLatch.await()
        return isSuccess
    }
}