package com.pkm.pinme.ui.main

import android.app.Activity
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Session
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.pkm.common.helper.VideoRecorder
import com.pkm.pinme.repository.PinMeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

class MainViewModel(private val repository: PinMeRepository) : ViewModel() {

    var arModel: ModelRenderable? = null
    var arSound: MediaPlayer? = null
    var augmentedImageDatabase: AugmentedImageDatabase? = null
    val isConfigurationCompleted = MutableLiveData(false)
    val videoRecorder : VideoRecorder = VideoRecorder()

    val isLoading = MutableLiveData(true)

    fun configureAr(arUrl: String, soundUrl: String, imgUrl: String, activity: Activity) {
        viewModelScope.launch {
            loadAugmentedImageDatabaseWithBitmap(imgUrl, activity)
            loadArModel(arUrl, activity)
            loadArSound(soundUrl)

            // Emit the AR configuration with the loaded resources
            isLoading.postValue(false)
            isConfigurationCompleted.postValue(true)
            Toast.makeText(activity, "DONE", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun loadArModel(arUrl: String, activity: Activity) {
        withContext(Dispatchers.Main) {
            ModelRenderable.builder()
                .setSource(activity, Uri.parse(arUrl))
                .setIsFilamentGltf(true)
                .setAsyncLoadEnabled(true)
                .build()
                .thenAccept { rabbitModel: ModelRenderable? ->
                    arModel = rabbitModel
                }
                .exceptionally {
                    Toast.makeText(activity, it.message, Toast.LENGTH_LONG).show()
                    Log.e("ERROR LOAD AR", it.message.toString())
                    null
                }
        }
    }

    private suspend fun loadArSound(soundUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                arSound = MediaPlayer().apply {
                    setDataSource(soundUrl)
                    prepare()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun loadAugmentedImageDatabaseWithBitmap(imgUrl: String, activity: Activity) {
        withContext(Dispatchers.IO) {
            try {
                val session = Session(activity)
                augmentedImageDatabase = AugmentedImageDatabase(session)

                val inputStream = URL(imgUrl).openStream()
                BitmapFactory.decodeStream(inputStream)?.let {
                    augmentedImageDatabase?.addImage("AR", it)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    //Take Photo
    fun takePhoto(activity: Activity, arFragment:ArFragment) = repository.takePhoto(activity, arFragment)
}
