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
import com.pkm.pinme.repository.PinMeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

class MainViewModel(private val repository: PinMeRepository) : ViewModel() {

    private var arModel: ModelRenderable? = null
    private var arSound: MediaPlayer? = null
    private var augmentedImageDatabase: AugmentedImageDatabase? = null

    val isConfigurationCompleted = MutableLiveData(false)

    val isLoading = MutableLiveData(true)

    fun getArModel() = arModel
    fun getArSound() = arSound
    fun getAugmentedImageDatabase() = augmentedImageDatabase

    fun configureAr(arUrl: String, soundUrl: String, imgUrl: String, activity: Activity) {
        viewModelScope.launch {
            async { loadAugmentedImageDatabase(activity) }.await()
            val loadArModelJob = launch { loadArModel(arUrl, activity) }
            val loadArSoundJob = launch { loadArSound(soundUrl) }
            val loadArMarkerJob = launch { loadAugmentedImageUrlBitmap(imgUrl) }

            // Wait for all tasks to complete
            loadArModelJob.join()
            loadArSoundJob.join()
            loadArMarkerJob.join()

            // Emit the AR configuration with the loaded resources

            isLoading.postValue(false)
            isConfigurationCompleted.postValue(true)
        }
    }

    private fun loadArModel(arUrl: String, activity: Activity) {
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

    private suspend fun loadAugmentedImageUrlBitmap(imgUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = URL(imgUrl).openStream()
                BitmapFactory.decodeStream(inputStream)?.let {
                    augmentedImageDatabase?.addImage("AR", it)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun loadAugmentedImageDatabase(activity: Activity) {
        withContext(Dispatchers.IO) {
            val session = Session(activity)
            augmentedImageDatabase = AugmentedImageDatabase(session)
        }
    }



}

data class ARConfiguration(
    val arModel: ModelRenderable?,
    val arSound: MediaPlayer?,
    val augmentedImageDatabase: AugmentedImageDatabase?
)
