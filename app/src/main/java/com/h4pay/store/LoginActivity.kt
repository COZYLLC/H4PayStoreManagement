package com.h4pay.store

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.h4pay.store.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var view:ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = DataBindingUtil.setContentView(this, R.layout.activity_login)
        fetchSchools()

    }

    private fun fetchSchools() : List<String> {
        return listOf("");
    }

}