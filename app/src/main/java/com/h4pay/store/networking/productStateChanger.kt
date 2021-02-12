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

class productStateChanger(private var state:JSONObject) : AsyncTask<Void, JSONObject, JSONObject>() {
    override fun doInBackground(vararg params: Void?): JSONObject {
        var inputStream: InputStream? = null

        var result: String? = ""

        try {
            val urlCon = URL("${BuildConfig.API_URL}/product/modify")
            val httpCon =
                urlCon.openConnection() as HttpURLConnection


            var json = state.toString()

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
        Log.d("productStateChanger", res.toString())
        return res
    }

    override fun onPostExecute(result: JSONObject?) {
        super.onPostExecute(result)
    }
}