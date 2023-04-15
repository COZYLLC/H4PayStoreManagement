package com.h4pay.store.util

import android.util.Log

class H4PayLogger {
    companion object {
        private const val prefix = "["
        @JvmStatic fun d(currentScope: Any, message: String) {
            Log.d("$prefix${currentScope::class.simpleName}]", message)
        }
        @JvmStatic fun i(currentScope: Any, message: String) {
            Log.i("$prefix${currentScope::class.simpleName}]", message)
        }
        @JvmStatic fun v(currentScope: Any, message: String) {
            Log.v("$prefix${currentScope::class.simpleName}]", message)
        }
        @JvmStatic fun e(currentScope: Any, message: String) {
            Log.e("$prefix${currentScope::class.simpleName}]", message)
        }
    }
}