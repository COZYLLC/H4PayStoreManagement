package com.h4pay.store.usecase

import com.h4pay.store.App
import com.h4pay.store.model.School

class GetSchools : ResultUseCase<Nothing?, List<School>>() {
    override suspend fun onExecute(params: Nothing?): List<School> {
        return App.schoolRepository.getSchools()
    }
}