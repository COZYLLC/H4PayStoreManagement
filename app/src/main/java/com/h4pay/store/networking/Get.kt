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

class Get (private val url:String) : AsyncTask<String, JSONObject?, JSONObject?>() {
    private val TAG = "Get"
    override fun doInBackground(vararg params: String): JSONObject? {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .get()
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

    override fun onPostExecute(result: JSONObject?) {
        super.onPostExecute(result)
    }

}