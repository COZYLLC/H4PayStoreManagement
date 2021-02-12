package com.h4pay.store.networking

import android.os.AsyncTask
import android.os.Build
import android.util.Log
import com.h4pay.store.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownHostException

class orderLookupByID(private var orderID: String): AsyncTask<Void, JSONObject, JSONObject>(){
    private val TAG = "NETWORK"
    override fun doInBackground(vararg params: Void?): JSONObject? {
        var version = 0.0
        try {
            // 서버연결
            val url = URL(
                    "${BuildConfig.API_URL}/orders/fromorderid/$orderID"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Content-Type", "application/json")
            conn.requestMethod = "GET"
            conn.connect()
            /* 서버 -> 안드로이드 파라메터값 전달 */
            val iss = conn.inputStream
            val inn = BufferedReader(InputStreamReader(iss))
            var line = inn.readLines()
            var page = String()
            for (x in 0..line.size - 1) {
                page += line[x]
            }
            Log.e(TAG, page)
            val jsonObject = JSONObject(page)
            if (jsonObject.get("lookupSuccess") == false){
                return null
            }
            else{
                return jsonObject.getJSONObject("order")
            }

        } catch (e: UnknownHostException){
            e.printStackTrace()
            return null
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
    override fun onPostExecute(result: JSONObject?) {
        super.onPostExecute(result)
    }
}