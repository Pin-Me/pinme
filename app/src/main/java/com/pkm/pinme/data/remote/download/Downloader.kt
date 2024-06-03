package com.pkm.pinme.data.remote.download

interface Downloader {
    fun downloadFile(url: String, fileName: String): Long
}