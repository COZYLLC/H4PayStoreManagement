package com.h4pay.store

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.h4pay.store.model.School
import com.h4pay.store.repository.OtherRepository
import com.h4pay.store.repository.SchoolRepository

// 사용되는 파일에서 클래스 밖에 선언하면 싱글톤으로 사용 가능합니다.
private val Context.dataStore by preferencesDataStore(name = "prefs")

class App : Application() {
    companion object {
        var token: String? = null
        var savedUser: School? = null
        val gson = Gson()
        val schoolRepository = SchoolRepository()
        val otherRepository = OtherRepository()
    }


}

data class Prefs(val token: String?)
