package com.h4pay.store.usecase

import com.h4pay.store.App
import com.h4pay.store.model.Version

class GetVersionInfo : ResultUseCase<Nothing?, Version>() {
    override suspend fun onExecute(params: Nothing?): Version {
        return App.otherRepository.getVersionInfo()
    }

}