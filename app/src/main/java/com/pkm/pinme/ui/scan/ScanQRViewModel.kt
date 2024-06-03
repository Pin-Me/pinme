package com.pkm.pinme.ui.scan

import android.content.Context
import androidx.lifecycle.ViewModel
import com.pkm.pinme.repository.PinMeRepository

class ScanQRViewModel(private val repository:PinMeRepository): ViewModel() {
    fun getFilter(filterId: String, context: Context) = repository.getFilter(filterId, context)
}