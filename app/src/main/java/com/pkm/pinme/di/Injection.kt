package com.pkm.pinme.di

import android.content.Context
import com.pkm.pinme.data.remote.network.ApiConfig
import com.pkm.pinme.repository.PinMeRepository

object Injection {
    fun provideRepository(context: Context): PinMeRepository {
        val apiService = ApiConfig.getApiService()
        return PinMeRepository.getInstance(apiService)
    }
}