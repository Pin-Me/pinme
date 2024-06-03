package com.pkm.pinme.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.pkm.pinme.data.remote.download.AndroidDownloader
import com.pkm.pinme.data.remote.network.ApiService
import com.pkm.pinme.data.remote.response.FilterModel
import com.pkm.pinme.utils.Result
import java.lang.Exception

class PinMeRepository(
    private var apiService: ApiService
) {


    fun getFilter(filterId: String, context: Context): LiveData<Result<FilterModel>> = liveData {
        emit(Result.Loading)
        try {
            val getFilterRes = apiService.getFilter(filterId)
            if (!getFilterRes.error) {
                val downloader = AndroidDownloader(context)
                downloader.downloadFile(getFilterRes.data.marker.toString(), getFilterRes.data.id.toString())
                emit(Result.Success(getFilterRes.data))
            }
            else {
                emit(Result.Error(getFilterRes.message))
            }
        } catch (e: Exception) {
            emit(Result.Error("AR Tidak Ditemukan"))
        }
    }

    companion object {
        @Volatile
        private var instance: PinMeRepository? = null
        fun getInstance(
            apiService: ApiService
        ): PinMeRepository =
            instance ?: synchronized(this) {
                instance ?: PinMeRepository(apiService)
            }.also { instance = it }
    }
}