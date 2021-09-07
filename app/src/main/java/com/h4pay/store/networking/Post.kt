package com.h4pay.store.networking

import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import com.h4pay.store.BuildConfig
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception

class Post (private val url:String, private val jsonObject: JSONObject) : AsyncTask<JSONObject, JSONObject, JSONObject>() {
    private val TAG = "Post"
    override fun doInBackground(vararg params: JSONObject?): JSONObject? {
        val JSON: MediaType = "application/json; charset=utf-8".toMediaTypeOrNull()!!
        val client = OkHttpClient()
        val reqBody = jsonObject.toString().toRequestBody(JSON)

        val request = Request.Builder()
            .url(url)
            .post(reqBody)
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