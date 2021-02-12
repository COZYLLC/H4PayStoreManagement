package com.h4pay.store.networking

import android.os.AsyncTask
import android.util.Log
import com.h4pay.store.BuildConfig
import com.h4pay.store.networking.tools.convertStreamToString
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownHostException

class cancelOrder(private var orderID: String): AsyncTask<Void, Boolean, Boolean>(){
    private val TAG = "NETWORK"
    override fun doInBackground(vararg params: Void?): Boolean? {
        var version = 0.0
        try {
            // 서버연결
            val url = URL(
                "${BuildConfig.API_URL}/orders/cancel/${orderID}"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.requestMethod = "GET"
            conn.doInput = true
            conn.connect()
            /* 서버 -> 안드로이드 파라메터값 전달 */
            val iss = conn.inputStream
            val inn = BufferedReader(InputStreamReader(iss))
            var line = inn.readLines()
            var page = String()
            for (element in line) {
                page += element
                Log.e("RECV DATA*", page)
            }
            val json = JSONObject(page)
            val canceled = json.getBoolean("cancelSuccess")
            return canceled
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
    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
    }
}