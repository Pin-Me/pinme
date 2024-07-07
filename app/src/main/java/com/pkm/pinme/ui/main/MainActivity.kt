package com.pkm.pinme.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.CamcorderProfile
import android.media.MediaScannerConnection
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Sceneform
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.BaseArFragment.OnSessionConfigurationListener
import com.google.ar.sceneform.ux.TransformableNode
import com.pkm.pinme.R
import com.pkm.pinme.databinding.ActivityMainBinding
import com.pkm.pinme.factory.ViewModelFactory
import com.pkm.pinme.ui.scan.ScanQRActivity
import java.io.File


class MainActivity : AppCompatActivity(), FragmentOnAttachListener, OnSessionConfigurationListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var arFragment: ArFragment
    private var rabbitDetected = false

    // AR
    private var markerUrl: String? = null
    private var arUrl: String? = null
    private var soundUrl: String? = null
    private var size: Float? = null

    // ViewModel
    private lateinit var factory: ViewModelFactory
    private val viewModel: MainViewModel by viewModels { factory }

    var recordingWidth: Int = 0
    var recordingHeight: Int = 0

    private var recordingStartTime: Long = 0
    private var recordingHandler: Handler = Handler(Looper.getMainLooper())
    private lateinit var recordingRunnable: Runnable


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setViewModelFactory()
        getScreenSize()
        setupUI()

        markerUrl = intent.getStringExtra("markerUrl")
        arUrl = intent.getStringExtra("arUrl")
        soundUrl = intent.getStringExtra("soundUrl")
        size = intent.getFloatExtra("size", 0.25f)
        Log.e("SIZE FROM API", intent.getFloatExtra("size", 0.25F).toString())

        Log.e("SIZE FROM API", size.toString());

//        markerUrl = "https://i.natgeofe.com/k/63b1a8a7-0081-493e-8b53-81d01261ab5d/red-panda-full-body_square.jpg"
//        arUrl = "https://github.com/Pin-Me/dummy-files/blob/main/red%20panda.glb?raw=true"
//        soundUrl ="https://github.com/Pin-Me/dummy-files/blob/main/guiro-sweep-156002.mp3?raw=true"
//        soundUrl ="https://github.com/Pin-Me/dummy-files/blob/main/guiro-sweep-156002.mp3?raw=true"

        recordingRunnable = object : Runnable {
            @SuppressLint("DefaultLocale")
            override fun run() {
                val elapsedSeconds = (System.currentTimeMillis() - recordingStartTime) / 1000
                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                binding.recordingText.text = String.format("%02d:%02d", minutes, seconds)
                recordingHandler.postDelayed(this, 1000)
            }
        }


        viewModel.configureAr(arUrl!!, soundUrl, markerUrl!!, this)

        viewModel.isConfigurationCompleted.observe(this) {
            if(it) {
                if (Sceneform.isSupported(this)) {
                    supportFragmentManager.addFragmentOnAttachListener(this)
                    supportFragmentManager.beginTransaction()
                        .add(R.id.arFragment, ArFragment::class.java, null)
                        .commit()
                }
            } else {
                Log.e("ArConfig", "Loading AR")
            }
        }
    }

    fun showRecentGalery(){
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PinMe")
        val allFiles = folder.listFiles { _, name -> name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".mp4", ignoreCase = true)
        } ?: return

        // Sort the files by last modified date in descending order
        val sortedFiles = allFiles.sortedByDescending { it.lastModified() }

        // Get the latest file or null if no files are found
        val latestFile = sortedFiles.firstOrNull()

        if (latestFile != null) {
            MediaScannerConnection.scanFile(
                this,
                arrayOf(latestFile.absolutePath),
                null
            ) { _, _ ->
                val bitmap = when {
                    latestFile.extension.equals("jpg", ignoreCase = true) -> {
                        // Load the image file into a Bitmap
                        BitmapFactory.decodeFile(latestFile.absolutePath)
                    }

                    latestFile.extension.equals("mp4", ignoreCase = true) -> {
                        // Load a video frame as a Bitmap
                        ThumbnailUtils.createVideoThumbnail(
                            latestFile.absolutePath,
                            MediaStore.Images.Thumbnails.MINI_KIND
                        )
                    }

                    else -> null
                }
                runOnUiThread {
                    binding.galleryImage.setImageBitmap(bitmap)
                    binding.galleryImage.visibility = View.VISIBLE
                }
            }
        } else {
            binding.galleryImage.visibility = View.GONE
        }
    }
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun setupUI() {
        viewModel.isLoading.observe(this) {
            if (it) {
                binding.loadingCard.visibility = View.VISIBLE
                binding.cameraBtn.visibility = View.GONE
                binding.scanQr.visibility = View.GONE
                binding.scanQrText.visibility = View.GONE
                binding.galleryImage.visibility = View.GONE
                binding.recordingTextLayout.visibility = View.INVISIBLE
            } else {
                binding.loadingCard.visibility = View.GONE
                binding.cameraBtn.visibility = View.VISIBLE
                binding.scanQr.visibility = View.VISIBLE
                binding.scanQrText.visibility = View.VISIBLE
                binding.recordingTextLayout.visibility = View.INVISIBLE
                showRecentGalery()
            }
        }

        val handler = Handler(Looper.getMainLooper())
        var isLongPress = false

        // Long press runnable
        val longPressRunnable = Runnable {
            isLongPress = true
            // Handle long press action
            binding.cameraBtn.isPressed = true
            binding.cameraBtn.setBackgroundResource(R.drawable.camera_button_pressed)
            viewModel.videoRecorder.setSceneView(arFragment.arSceneView)
            val orientation = this.resources.configuration.orientation
            viewModel.videoRecorder.setVideoQuality(CamcorderProfile.QUALITY_720P, orientation)
            viewModel.videoRecorder.setVideoSize(recordingWidth, recordingHeight)
            viewModel.videoRecorder.setFrameRate(60)
            viewModel.videoRecorder.onToggleRecord()
            if (soundUrl != null) {
                viewModel.arSound?.start()
            }

            // Start recording time tracking
            recordingStartTime = System.currentTimeMillis()
            binding.recordingTextLayout.visibility = View.VISIBLE
            recordingHandler.post(recordingRunnable)
        }

        // Set OnTouchListener for the button
        binding.cameraBtn.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isLongPress = false
                    handler.postDelayed(longPressRunnable, 1000) // Start long press detection after 1 second
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPressRunnable) // Remove long press detection
                    binding.cameraBtn.isPressed = false
                    if (isLongPress) {
                        viewModel.videoRecorder.onToggleRecord()
                        binding.cameraBtn.setBackgroundResource(R.drawable.camera_button)
                        binding.recordingTextLayout.visibility = View.GONE
                        recordingHandler.removeCallbacks(recordingRunnable)
                        binding.recordingText.text = "00:00"
                        showRecentGalery()
                    } else {
                        showBlackOverlay()
                        viewModel.takePhoto(this, arFragment)
                    }
                    true
                }
                else -> false
            }

        }

        binding.scanQr.setOnClickListener {
            startActivity(Intent(this, ScanQRActivity::class.java))
        }

        binding.galleryImage.setOnClickListener {
            val pinMeFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PinMe")

            if (pinMeFolder.exists() && pinMeFolder.isDirectory) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.type = "*/*"
                startActivity(intent)
            } else {
                // Handle case when "PinMe" folder does not exist or is not a directory
                Toast.makeText(this, "Folder 'PinMe' not found", Toast.LENGTH_SHORT).show()
            }
        }


    }



    override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment) {
        if (fragment.id == R.id.arFragment) {
            arFragment = fragment as ArFragment
            arFragment.setOnSessionConfigurationListener(this)
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

                anchorNode.worldScale = Vector3(size!!, size!!, size!!)
                arFragment.arSceneView.scene.addChild(anchorNode)

                val modelNode = TransformableNode(
                    arFragment.transformationSystem
                )

                modelNode.setRenderable(viewModel.arModel)
                anchorNode.addChild(modelNode)
                viewModel.arSound?.start()
            }
        }
    }

    override fun onSessionConfiguration(session: Session?, config: Config?) {
        config?.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED)
        config?.setFocusMode(Config.FocusMode.AUTO)

        config?.setAugmentedImageDatabase(viewModel.augmentedImageDatabase)
        // Set the updated AR session configuration
        arFragment.setOnAugmentedImageUpdateListener { augmentedImage: AugmentedImage ->
            onAugmentedImageTrackingUpdate(augmentedImage)
        }
        session?.resume()
        session?.pause()
        session?.resume()
    }


    private fun setViewModelFactory() {
        factory = ViewModelFactory.getInstance(binding.root.context)
    }

    private fun getScreenSize() {
        val displayMetrics = DisplayMetrics()
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        recordingWidth = displayMetrics.widthPixels
        recordingHeight = displayMetrics.heightPixels
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        val intentScan = Intent(this@MainActivity, ScanQRActivity::class.java)
        intentScan.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intentScan)
        finish()
    }


    private fun showBlackOverlay() {
        binding.blackOverlay.visibility = View.VISIBLE
    }

    fun hideBlackOverlay() {
        binding.blackOverlay.visibility = View.GONE
    }
}

