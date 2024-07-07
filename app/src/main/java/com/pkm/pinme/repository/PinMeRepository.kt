package com.pkm.pinme.repository

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.google.android.material.snackbar.Snackbar
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.pkm.pinme.data.remote.network.ApiService
import com.pkm.pinme.data.remote.response.FilterModel
import com.pkm.pinme.ui.main.MainActivity
import com.pkm.pinme.utils.Result
import kotlinx.coroutines.awaitAll
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class PinMeRepository(
    private var apiService: ApiService
) {


    fun getFilter(filterId: String): LiveData<Result<FilterModel>> = liveData {
        emit(Result.Loading)
        try {
            val getFilterRes = apiService.getFilter(filterId)
            if (!getFilterRes.error) {
                emit(Result.Success(getFilterRes.data))
            }
            else {
                emit(Result.Error(getFilterRes.message))
            }
        } catch (e: Exception) {
            emit(Result.Error("Terjadi kesalahan, hubungi admin"))
        }
    }

    fun takePhoto(activity: MainActivity, arFragment: ArFragment) {
        val filename = generateFilename()
        val view: ArSceneView = arFragment.arSceneView

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val handlerThread = HandlerThread("PixelCopier")
        handlerThread.start()

        PixelCopy.request(view, bitmap, { copyResult ->
            activity.runOnUiThread {
                if (copyResult == PixelCopy.SUCCESS) {
                    try {
                        val savedPath = saveBitmapToDisk(activity, bitmap, filename)
                        MediaScannerConnection.scanFile(
                            activity.applicationContext,
                            arrayOf(savedPath),
                            null
                        ) { path: String, uri: Uri ->
                            activity.showRecentGalery()
                        }
                        activity.hideBlackOverlay()
                    } catch (e: IOException) {
                        Log.e("HIYA ERROR", e.message.toString())
                        val toast = Toast.makeText(activity.applicationContext, e.toString(), Toast.LENGTH_LONG)
                        toast.show()
                    }
                } else {
                    val toast = Toast.makeText(activity.applicationContext, "Failed to copyPixels: $copyResult", Toast.LENGTH_LONG)
                    toast.show()
                }
                handlerThread.quitSafely()
            }
        }, Handler(handlerThread.looper))
    }

    fun saveBitmapToDisk(context: Context, bitmap: Bitmap, filename: String): String {
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PinMe")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val photoFile = File(directory, filename)
        FileOutputStream(photoFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        return photoFile.absolutePath
    }

    fun generateFilename(): String {
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        val date = Date()
        return "PinMePicture_${dateFormat.format(date)}.jpg"
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