package com.h4pay.store.util

import android.util.Base64
import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

object Encryption {
    fun encryptToSha256Md5(rawString: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(rawString.toByteArray(StandardCharsets.UTF_8))
        val encrypted = messageDigest.digest()
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun generateSessionId(): String {
        val secureRandom = SecureRandom()
        var rawSessionId = ""
        for (x in 0..30) {
            rawSessionId += secureRandom.nextInt().toChar()
        }
        return rawSessionId
    }

}