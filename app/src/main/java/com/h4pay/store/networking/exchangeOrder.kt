package com.h4pay.store.networking

import android.os.AsyncTask
import android.util.Log
import com.h4pay.store.BuildConfig
import com.h4pay.store.networking.tools.convertStreamToString
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class exchangeOrder(private var orderID: String): AsyncTask<Void, Boolean, Boolean>(){ //Post
    private val TAG = "NETWORK"
    override fun doInBackground(vararg params: Void?): Boolean? {
        var inputStream: InputStream? = null

        var result: String? = ""

        try {
            val urlCon = URL("${BuildConfig.API_URL}/orders/exchange")
            val httpCon =
                urlCon.openConnection() as HttpURLConnection
            var json = ""


            // build jsonObject
            val jsonObject = JSONObject()
            jsonObject.accumulate("orderId", orderID)

            // convert JSONObject to JSON to String
            json = jsonObject.toString()

            // Set some headers to inform server about the type of the content
            httpCon.setRequestProperty("Accept", "application/json")
            httpCon.setRequestProperty("Content-type", "application/json")

            // OutputStream으로 POST 데이터를 넘겨주겠다는 옵션.
            httpCon.doOutput = true

            // InputStream으로 서버로 부터 응답을 받겠다는 옵션.
            httpCon.doInput = true
            val os: OutputStream = httpCon.outputStream
            os.write(json.toByteArray(charset("UTF-8")))
            os.flush()

            // receive response as inputStream
            try {
                inputStream = httpCon.inputStream

                // convert inputstream to string
                if (inputStream != null) result = convertStreamToString.sts(inputStream) else result =
                    "Did not work!"
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                httpCon.disconnect()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            Log.d("InputStream", e.localizedMessage)
        }
        val res = JSONObject(result!!)
        val success = res.getBoolean("exchangeSuccess")
        return success
    }
    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
    }
}