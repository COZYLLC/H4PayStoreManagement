package com.h4pay.store.usecase

import android.util.Log
import com.h4pay.store.util.H4PayLogger

abstract class UseCase<ParamT, ResultT>() {
    private val TAG = "[UseCase]"

    open suspend fun run(params: ParamT): ResultT {
        try {
            Log.d(TAG, "Running UseCase $this with following params: $params")
            return onExecute(params)
        } catch (e: Exception) {
            H4PayLogger.e(
                this,
                "Exception ${e.javaClass.canonicalName} occurred while running UseCase: $this\nDetail: ${e.stackTraceToString()}"
            )
            throw e;
        }

    }

    abstract suspend fun onExecute(params: ParamT): ResultT
}