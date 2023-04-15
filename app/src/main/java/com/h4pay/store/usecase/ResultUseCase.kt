package com.h4pay.store.usecase

import com.h4pay.store.State
import com.h4pay.store.util.H4PayLogger


abstract class ResultUseCase<P, R> {
    abstract suspend fun onExecute(params: P): R

    suspend fun run(params: P): State<R> {
        try {
            H4PayLogger.d(this, "Running UseCase $this with following params: $params")
            return kotlin.runCatching {
                State.Success(onExecute(params))
            }.getOrElse { State.Error(it) }
        } catch (e: Exception) {
            H4PayLogger.e(
                this,
                "Exception ${e.javaClass.canonicalName} occurred while running UseCase: $this\nDetail: ${e.stackTraceToString()}"
            )
            throw e;
        }

    }

}