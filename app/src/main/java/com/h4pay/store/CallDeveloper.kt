package com.h4pay.store

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.h4pay.store.networking.RetrofitInstance
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.RuntimeException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.LinkedHashMap

class CallDeveloper : AppCompatActivity() {
    private lateinit var titleInput: EditText;
    private lateinit var contentInput: EditText;
    private lateinit var uploadedTextView: TextView;
    private lateinit var uploadButton: LinearLayout;
    private lateinit var submitButton: LinearLayout;
    var file: File? = null;

    private fun uiInit() {
        titleInput = findViewById(R.id.titleInput)
        contentInput = findViewById(R.id.contentInput)
        uploadedTextView = findViewById(R.id.uploadedFile)
        uploadButton = findViewById(R.id.uploadImage)
        submitButton = findViewById(R.id.submit)

        uploadButton.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(intent, 1)
        }

        submitButton.setOnClickListener {
            val title = titleInput.text.toString()
            val content = contentInput.text.toString()
            val bodyMap:LinkedHashMap<String, RequestBody> = LinkedHashMap()
            val fileBody = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file!!)
            bodyMap["img\"; filename=\"${file!!.name}"] = fileBody
            bodyMap["uid"] =  RequestBody.create("text/plain".toMediaTypeOrNull(), "storeExchanger")
            bodyMap["email"] =  RequestBody.create("text/plain".toMediaTypeOrNull(), "storeExchanger")
            bodyMap["title"] =  RequestBody.create("text/plain".toMediaTypeOrNull(), title)
            bodyMap["content"] =  RequestBody.create("text/plain".toMediaTypeOrNull(), content)
            bodyMap["category"] = RequestBody.create("text/plain".toMediaTypeOrNull(), "exchanger")
            lifecycleScope.launch {
                kotlin.runCatching {
                    RetrofitInstance.service.submitSupport(bodyMap)
                }.onSuccess {
                    if (it.isSuccessful) {
                        Toast.makeText(this@CallDeveloper, "제출이 완료되었습니다.", Toast.LENGTH_SHORT)
                            .show()
                        makeEmpty()
                    }
                }.onFailure {
                    Log.e("Error", it.message!!)
                    Toast.makeText(
                        this@CallDeveloper,
                        "제출에 실패했습니다. support@cozyllc.co.kr 등 개발사 연락처로 문의 부탁드립니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }
    }

    private fun makeEmpty() {
        titleInput.setText("")
        contentInput.setText("")
        uploadedTextView.text = ""
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 1 || resultCode != RESULT_OK) {
            return;
        }
        val dataUri = data?.data

        try {
            val ins = contentResolver.openInputStream(dataUri!!)
            var image = BitmapFactory.decodeStream(ins)
            val date = SimpleDateFormat("yyyy_MM_dd_hh_mm_ss").format(Date())
            uploadedTextView.text = dataUri.lastPathSegment;
            File.createTempFile("support_${date}.png", null, this.cacheDir)
            file = File(this.cacheDir, "support_${date}.png")
            val out = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.JPEG, 100, out)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        uiInit()

    }
}