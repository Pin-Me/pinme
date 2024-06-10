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
import com.pkm.pinme.utils.Result
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


    fun getFilter(filterId: String, context: Context): LiveData<Result<FilterModel>> = liveData {
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
            emit(Result.Error("AR Tidak Ditemukan"))
        }
    }

    fun takePhoto(activity: Activity, arFragment: ArFragment) {
        val filename = generateFilename()
        val view: ArSceneView = arFragment.arSceneView

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val handlerThread = HandlerThread("PixelCopier")
        handlerThread.start()

        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(activity, bitmap, filename)
                } catch (e: IOException) {
                    val toast = Toast.makeText(activity.applicationContext, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                    return@request
                }
                val toast = Toast.makeText(activity.applicationContext, "Photo Captured", Toast.LENGTH_LONG)


            } else {
                val toast = Toast.makeText(activity.applicationContext, "Failed to copyPixels: $copyResult", Toast.LENGTH_LONG)
                toast.show()
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }

    private fun generateFilename(): String {
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .toString() + "/PinMe/" + "PinMePicture_" + date + ".jpg"
    }

    private fun saveBitmapToDisk(activity: Activity, bitmap: Bitmap, filename: String) {
        val out = File(filename)
        if (!out.parentFile.exists()) {
            out.parentFile.mkdirs()
        }
        try {
            FileOutputStream(filename).use { outputStream ->
                ByteArrayOutputStream().use { outputData ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData)
                    outputData.writeTo(outputStream)
                    outputStream.flush()
                    outputStream.close()
                }
            }
        } catch (ex: IOException) {
            throw IOException("Failed to save bitmap to disk", ex)
        }

        MediaScannerConnection.scanFile(
            activity.applicationContext,
            arrayOf(filename),
            null
        ) { path: String, uri: Uri ->
            Log.i("INFO", "-> uri=$uri")
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