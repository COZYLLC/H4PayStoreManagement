package com.h4pay.store.networking.tools

import android.os.AsyncTask
import android.util.Log
import com.h4pay.store.BuildConfig
import okhttp3.*
import java.io.File

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.CountDownLatch

class uploadSupport (private val title:String, private val content:String, private val file:File?) : AsyncTask<JSONObject, JSONObject, JSONObject>() {
    private val TAG = "uploadSupport"
    override fun doInBackground(vararg params: JSONObject?): JSONObject? {
        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("uid", "storeExchanger")
            .addFormDataPart("email", "storeExchanger")
            .addFormDataPart("title", title)
            .addFormDataPart("content", content)
            .addFormDataPart("category", "category")
        if (file != null) requestBody.addFormDataPart("img", file.name, RequestBody.create(MultipartBody.FORM, file))
        val builtBody:RequestBody = requestBody.build()

        val request = Request.Builder()
            .url("${BuildConfig.API_URL}/upload")
            .addHeader("Content-Type", "multipart/form-data; charset=utf-8")
            .post(builtBody)
            .build()
        try {
            val response = client.newCall(request).execute().body!!.string();
            var res:JSONObject = JSONObject()
            Log.d(TAG, response)
            res = JSONObject(response)
            return res
        } catch (e:Exception) {
            return null
        }
    }

    override fun onPostExecute(result: JSONObject) {
        super.onPostExecute(result)
    }

}