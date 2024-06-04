package com.pkm.pinme.data.remote.download

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri

class AndroidDownloader(private val context: Context) : Downloader {

    private val downloadManager = context.getSystemService(DownloadManager::class.java)

    override fun downloadFile(url: String, fileName: String): Long {
        val extension = url.substringAfterLast('.', "")
        val completeFileName = if (extension.isNotEmpty()) {
            "$fileName.$extension"
        } else {
            fileName
        }
        Log.e("URLURL", completeFileName)

        val request = DownloadManager.Request(url.toUri())
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "PinMe/Marker/$completeFileName")
        return downloadManager.enqueue(request)
    }
}
