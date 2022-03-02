package com.h4pay.store

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.h4pay.store.networking.tools.uploadSupport
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.RuntimeException
import java.text.SimpleDateFormat
import java.util.*

class CallDeveloper : AppCompatActivity() {
    private lateinit var titleInput: EditText;
    private lateinit var contentInput: EditText;
    private lateinit var uploadedTextView: TextView;
    private lateinit var uploadButton: LinearLayout;
    private lateinit var submitButton: LinearLayout;
    var file: File? = null;

    fun UiInit() {
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
            val submitRes = uploadSupport(
                titleInput.text.toString(),
                contentInput.text.toString(),
                file
            ).execute().get()
            if (submitRes != null && submitRes.getBoolean("status")) {
                Toast.makeText(this, "제출이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                makeEmpty()
            }
        }
    }

    fun makeEmpty() {
        titleInput.setText("")
        contentInput.setText("")
        uploadedTextView.setText("")
    }

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
        UiInit()

    }
}