package com.pkm.pinme.ui.main

import android.content.Intent
import android.graphics.Bitmap
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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Sceneform
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.pkm.pinme.R
import com.pkm.pinme.databinding.ActivitMainBinding
import com.pkm.pinme.factory.ViewModelFactory
import com.pkm.pinme.ui.scan.ScanQRActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CompletableFuture

class MainActivit : AppCompatActivity(), FragmentOnAttachListener {

    private lateinit var binding: ActivitMainBinding

    private val futures: MutableList<CompletableFuture<Void>> = ArrayList()
    private lateinit var arFragment: ArFragment
    private var rabbitDetected = false
    private var arUrl: String? = null
    private var soundUrl: String? = null

    // ViewModel
    private lateinit var factory: ViewModelFactory
    private val viewModel: MainViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setViewModelFactory()
        setupUI()

        val url = "https://yt3.googleusercontent.com/cFv0K1fb4MjrZcnq0ntEkRU99ADunvfvFZ1APEr6nmJlRsAMwKPYVpiUb2C1UxfUjc14ajM=s900-c-k-c0x00ffffff-no-rj"
        arUrl = "https://github.com/Pin-Me/dummy-files/blob/main/Rabbit.glb?raw=true"
        soundUrl = "https://github.com/Pin-Me/dummy-files/blob/main/guiro-sweep-156002.mp3?raw=true"

        if (Sceneform.isSupported(this)) {
            supportFragmentManager.addFragmentOnAttachListener(this)
            supportFragmentManager.beginTransaction()
                .add(R.id.arFragment, ArFragment::class.java, null)
                .commit()
        }

        viewModel.configureAr(arUrl!!, soundUrl!!, url, this)

        viewModel.isConfigurationCompleted.observe(this) {
            if(it) {
                applyARConfiguration()
            } else {
                Log.e("ArConfig", "MASIH KOSONG")
            }
        }
    }

    private fun setupUI() {
        viewModel.isLoading.observe(this) {
            if (it){
                binding.blocLoading.visibility = View.VISIBLE
            } else {
                binding.blocLoading.visibility = View.GONE
            }
        }
        val displayMetrics = DisplayMetrics()
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        binding.fabRecordBtn.setOnClickListener {
            // Record button logic
        }

        binding.fabShotBtn.setOnClickListener {
            // Shot button logic
        }
    }

    override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment) {
        if (fragment.id == R.id.arFragment) {
            arFragment = fragment as ArFragment
        }
    }

    private fun onAugmentedImageTrackingUpdate(augmentedImage: AugmentedImage) {
        if (rabbitDetected) {
            return
        }

        if (augmentedImage.trackingState == TrackingState.TRACKING
            && augmentedImage.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING
        ) {
            val anchorNode = AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose))

            if (!rabbitDetected && augmentedImage.name == "AR") {
                rabbitDetected = true
                Toast.makeText(this, "${augmentedImage.name} tag detected", Toast.LENGTH_LONG).show()

                anchorNode.worldScale = Vector3(0.25f, 0.25f, 0.25f)
                arFragment.arSceneView.scene.addChild(anchorNode)

                val modelNode = TransformableNode(
                    arFragment.transformationSystem
                )

                modelNode.setRenderable(viewModel.getArModel())
                anchorNode.addChild(modelNode)
                viewModel.getArSound()?.start()
            }
        }
    }

    private fun applyARConfiguration() {
        val config = Config(arFragment.arSceneView.session)
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED)
        config.setFocusMode(Config.FocusMode.AUTO)

        config.setAugmentedImageDatabase(viewModel.getAugmentedImageDatabase())
        // Set the updated AR session configuration
        arFragment.setSessionConfig(config, true)
        arFragment.setOnAugmentedImageUpdateListener { augmentedImage: AugmentedImage ->
            onAugmentedImageTrackingUpdate(augmentedImage)
        }
    }

    private fun setViewModelFactory() {
        factory = ViewModelFactory.getInstance(binding.root.context)
    }


    private fun generateFilename(): String {
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .toString() + "/PinMe/" + "PinMePicture_" + date + ".jpg"
    }

    private fun takePhoto() {
        val filename = generateFilename()
        val view: ArSceneView = arFragment.arSceneView

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val handlerThread = HandlerThread("PixelCopier")
        handlerThread.start()

        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename)
                } catch (e: IOException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                    return@request
                }
                val snackbar = Snackbar.make(
                    findViewById(android.R.id.content), "Photo saved", Snackbar.LENGTH_LONG
                )
                snackbar.setAction("Open in Photos") { v: View? ->
                    val photoFile = File(filename)
                    val photoURI = FileProvider.getUriForFile(
                        this, this.packageName.toString() + ".ar.codelab.name.provider", photoFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW, photoURI)
                    intent.setDataAndType(photoURI, "image/*")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }
                snackbar.show()
            } else {
                val toast = Toast.makeText(this, "Failed to copyPixels: $copyResult", Toast.LENGTH_LONG)
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
            arrayOf(filename),
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

