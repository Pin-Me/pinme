package com.pkm.pinme.ui.main

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.CamcorderProfile
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Sceneform
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.BaseArFragment.OnSessionConfigurationListener
import com.google.ar.sceneform.ux.TransformableNode
import com.pkm.common.helper.VideoRecorder
import com.pkm.pinme.R
import com.pkm.pinme.databinding.ActivitMainBinding
import com.pkm.pinme.ui.scan.ScanQRActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CompletableFuture

class MainActivit : AppCompatActivity(), FragmentOnAttachListener,
    OnSessionConfigurationListener {

    private lateinit var binding: ActivitMainBinding

    private val futures: MutableList<CompletableFuture<Void>> = ArrayList()
    private lateinit var arFragment: ArFragment
    private var rabbitDetected = false
    private lateinit var database: AugmentedImageDatabase
    private var marker : Bitmap? = null
    private val videoRecorder : VideoRecorder = VideoRecorder()
    private var arUrl : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val intent = intent
        val url = intent.getStringExtra("url").toString()
        arUrl = intent.getStringExtra("ar").toString()
        runBlocking {
            marker = loadAugmentedImageUrlBitmap(url)
        }

        super.onCreate(savedInstanceState)
        binding = ActivitMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportFragmentManager.addFragmentOnAttachListener(this)

        if (Sceneform.isSupported(this)) {
            supportFragmentManager.beginTransaction()
                .add(R.id.arFragment, ArFragment::class.java, null)
                .commit()
        }

        val displayMetrics = DisplayMetrics()

        // Get the current window manager and default display
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        // Get the screen width and height in pixels
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        binding.fabRecordBtn.setOnClickListener {

            videoRecorder.setSceneView(arFragment.arSceneView)
            val orientation = this.getResources().configuration.orientation;
            videoRecorder.setVideoQuality(CamcorderProfile.QUALITY_720P, orientation)
            videoRecorder.setVideoSize(width, height)
            videoRecorder.setFrameRate(60)

            val isRecording = videoRecorder.onToggleRecord()
            if (isRecording){
                Toast.makeText(this,"Recording",Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this,"Stop Recording",Toast.LENGTH_LONG).show()
            }
        }

        binding.fabShotBtn.setOnClickListener {
            takePhoto()
        }
    }
    override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment) {
        if (fragment.id == R.id.arFragment) {
            arFragment = fragment as ArFragment
            arFragment.setOnSessionConfigurationListener(this)
        }
    }


    override fun onSessionConfiguration(session: Session, config: Config) {
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED)

        database = AugmentedImageDatabase(session)

        database.addImage("AR", marker)
        config.setAugmentedImageDatabase(database)
        config.setFocusMode(Config.FocusMode.AUTO)

        // Check for image detection
        arFragment.setOnAugmentedImageUpdateListener { augmentedImage: AugmentedImage ->
            onAugmentedImageTrackingUpdate(augmentedImage)
        }

        session.resume()
        session.pause()
        session.resume()
    }

    private fun onAugmentedImageTrackingUpdate(augmentedImage: AugmentedImage) {
        if (rabbitDetected) {
            return
        }

        if (augmentedImage.trackingState == TrackingState.TRACKING
            && augmentedImage.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING
        ) {
            // Setting anchor to the center of Augmented Image
            val anchorNode = AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose))

            if (!rabbitDetected && augmentedImage.name == "AR") {
                rabbitDetected = true
                Toast.makeText(this, "AR tag detected", Toast.LENGTH_LONG).show()

                anchorNode.worldScale = Vector3(0.25f, 0.25f, 0.25f)
                arFragment.arSceneView.scene.addChild(anchorNode)

                futures.add(ModelRenderable.builder()
                    .setSource(this, Uri.parse(arUrl))
                    .setIsFilamentGltf(true)
                    .setAsyncLoadEnabled(true)
                    .build()
                    .thenAccept { rabbitModel: ModelRenderable? ->
                        val modelNode = TransformableNode(
                            arFragment.transformationSystem
                        )
                        modelNode.setRenderable(rabbitModel)
                        anchorNode.addChild(modelNode)
                    }
                    .exceptionally {
                        Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                        Log.e("ERROR LOAD AR", it.message.toString())
                        null
                    })
            }
        }
    }

    private suspend fun loadAugmentedImageUrlBitmap(imgUrl: String): Bitmap? {
        return try {
            withContext(Dispatchers.IO) {
                BitmapFactory.decodeStream(URL(imgUrl).openConnection().getInputStream())
            }
        } catch (e: IOException) {
            println(e)
            null
        }
    }

    private fun generateFilename(): String {
        val date =
            SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                .format(Date())
        return Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        ).toString() + "/PinMe/" + "PinMePicture_" + date + ".jpg"
    }

    private fun takePhoto() {
        val filename = generateFilename()
        val view: ArSceneView = arFragment.arSceneView

        // Create a bitmap the size of the scene view.
        val bitmap = Bitmap.createBitmap(
            view.width, view.height,
            Bitmap.Config.ARGB_8888
        )

        // Create a handler thread to offload the processing of the image.
        val handlerThread = HandlerThread("PixelCopier")
        handlerThread.start()
        // Make the request to copy.
        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult === PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename)
                } catch (e: IOException) {
                    val toast = Toast.makeText(
                        this, e.toString(),
                        Toast.LENGTH_LONG
                    )
                    toast.show()
                    return@request
                }
                val snackbar = Snackbar.make(
                    findViewById(android.R.id.content),
                    "Photo saved", Snackbar.LENGTH_LONG
                )
                snackbar.setAction(
                    "Open in Photos"
                ) { v: View? ->
                    val photoFile = File(filename)
                    val photoURI = FileProvider.getUriForFile(
                        this,
                        this.packageName
                            .toString() + ".ar.codelab.name.provider",
                        photoFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW, photoURI)
                    intent.setDataAndType(photoURI, "image/*")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }
                snackbar.show()
            } else {
                val toast = Toast.makeText(
                    this,
                    "Failed to copyPixels: $copyResult", Toast.LENGTH_LONG
                )
                toast.show()
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }

    @Throws(IOException::class)
    private fun saveBitmapToDisk(bitmap: Bitmap, filename: String) {
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
            this,
            arrayOf<String>(filename),
            null
        ) { path: String, uri: Uri ->
            Log.i("INFO", "-> uri=$uri")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        futures.forEach { future ->
            if (!future.isDone) {
                future.cancel(true)
            }
        }
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        val intentScan = Intent(this@MainActivit, ScanQRActivity::class.java)
        intentScan.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intentScan)
        finish()
    }
}