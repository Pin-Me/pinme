package com.pkm.pinme.ui.main

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.SensorManager
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.Pair
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.RequestManager
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Camera
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.RecordingConfig
import com.google.ar.core.RecordingStatus
import com.google.ar.core.Session
import com.google.ar.core.Track
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.ImageInsufficientQualityException
import com.google.ar.core.exceptions.RecordingFailedException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.pkm.common.helper.DisplayRotationHelper
import com.pkm.common.helper.FullScreenHelper
import com.pkm.common.helper.SnackbarHelper
import com.pkm.common.helper.TrackingStateHelper
import com.pkm.common.rendering.BackgroundRenderer
import com.pkm.pinme.R
import com.pkm.pinme.databinding.ActivityMainBinding
import com.pkm.pinme.rendering.AugmentedImageRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class MainActivity : AppCompatActivity(), GLSurfaceView.Renderer {
    private lateinit var binding: ActivityMainBinding

    private var surfaceView: GLSurfaceView? = null
    private var fitToScanView: ImageView? = null
    private var installRequested = false
    private var session: Session? = null
    private val messageSnackbarHelper = SnackbarHelper()
    private lateinit var displayRotationHelper: DisplayRotationHelper
    private lateinit var config: Config
    private val trackingStateHelper: TrackingStateHelper = TrackingStateHelper(this)
    private val backgroundRenderer = BackgroundRenderer()
    private val augmentedImageRenderer = AugmentedImageRenderer()
    private var shouldConfigureSession = false
    private val augmentedImageMap: MutableMap<Int, Pair<AugmentedImage, Anchor>> = HashMap()

    private val MP4_DATASET_FILENAME_TEMPLATE = "arcore-dataset-%s.mp4"
    private val MP4_DATASET_TIMESTAMP_FORMAT = "yyyy-MM-dd-HH-mm-ss"

    private val ANCHOR_TRACK_ID = UUID.fromString("a65e59fc-2e13-4607-b514-35302121c138")
    private val ANCHOR_TRACK_MIME_TYPE = "application/hello-recording-playback-anchor"

    private var lastRecordingDatasetPath: String? = null

    private var playbackDatasetPath: String? = null

    private var mWidth = 0
    private var mHeight = 0
    private var capturePicture = false

    private lateinit var url: String

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        val intent = intent
        url = intent.getStringExtra("url").toString()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        surfaceView = findViewById(R.id.surface_view)
        displayRotationHelper = DisplayRotationHelper(this)
        surfaceView?.apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(this@MainActivity)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            setWillNotDraw(false)
        }
        fitToScanView = findViewById(R.id.image_view_fit_to_scan)
        installRequested = false
    }

    override fun onDestroy() {
        session?.close()
        session = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (session == null) {
            try {
                when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        installRequested = true
                        return
                    }

                    ArCoreApk.InstallStatus.INSTALLED -> {
                    }
                }
                session = Session(this)
            } catch (e: UnavailableArcoreNotInstalledException) {
                messageSnackbarHelper.showError(this, "Please install ARCore")
                return
            } catch (e: UnavailableApkTooOldException) {
                messageSnackbarHelper.showError(this, "Please update ARCore")
                return
            } catch (e: UnavailableSdkTooOldException) {
                messageSnackbarHelper.showError(this, "Please update this app")
                return
            } catch (e: Exception) {
                messageSnackbarHelper.showError(this, "This device does not support AR")
                return
            }
            shouldConfigureSession = true
        }
        if (shouldConfigureSession) {
            CoroutineScope(Main).launch {
                configureSession()
            }
            shouldConfigureSession = false
        }
        surfaceView?.onResume()
        displayRotationHelper.onResume()
    }

    override fun onPause() {
        super.onPause()
        session?.apply {
            displayRotationHelper.onPause()
            surfaceView?.onPause()
            pause()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        try {
            backgroundRenderer.createOnGlThread(this)
            augmentedImageRenderer.createOnGlThread(this)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read an asset file", e)
        }


    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
        GLES20.glViewport(0, 0, width, height)
        mWidth = width;
        mHeight = height
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (session == null) {
            return
        }
        displayRotationHelper.updateSessionIfNeeded(session)
        try {
                session!!.setCameraTextureName(backgroundRenderer.textureId)
                val frame = session!!.update()
                val camera = frame.camera
                trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)
                backgroundRenderer.draw(frame)

                val projmtx = FloatArray(16)
                camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f)

                val viewmtx = FloatArray(16)
                camera.getViewMatrix(viewmtx, 0)

                val colorCorrectionRgba = FloatArray(4)
                frame.lightEstimate.getColorCorrection(colorCorrectionRgba, 0)
                drawAugmentedImages(frame, projmtx, viewmtx, colorCorrectionRgba)

        } catch (t: Throwable) {
            Log.e(TAG, "Exception on the OpenGL thread", t)
        }
    }

    private suspend fun configureSession() {
        config = Config(session)
        config.setFocusMode(Config.FocusMode.AUTO)
        setupAugmentedImageDatabaseFromUrl(config)
        session?.resume()
        session?.pause()
        session?.resume()
        session?.configure(config)
    }

    private fun drawAugmentedImages(
        frame: Frame,
        projmtx: FloatArray,
        viewmtx: FloatArray,
        colorCorrectionRgba: FloatArray
    ) {
        val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
        for (augmentedImage in updatedAugmentedImages) {
            when (augmentedImage.trackingState) {
                TrackingState.PAUSED -> {
                    val text = String.format("Detected Image %d", augmentedImage.index)
                    messageSnackbarHelper.showMessage(this, text)
                }

                TrackingState.TRACKING -> {
                    runOnUiThread {
                        fitToScanView?.visibility = View.GONE
                    }
                    if (!augmentedImageMap.containsKey(augmentedImage.index)) {
                        val centerPoseAnchor =
                            augmentedImage.createAnchor(augmentedImage.centerPose)
                        augmentedImageMap[augmentedImage.index] =
                            Pair.create(augmentedImage, centerPoseAnchor)
                    }
                }

                TrackingState.STOPPED -> augmentedImageMap.remove(augmentedImage.index)
                else -> {
                }
            }
        }
        for (pair in augmentedImageMap.values) {
            val augmentedImage = pair.first
            val centerAnchor = augmentedImageMap[augmentedImage.index]?.second
            when (augmentedImage.trackingState) {
                TrackingState.TRACKING ->
                    augmentedImageRenderer.draw(
                        viewmtx,
                        projmtx,
                        augmentedImage,
                        centerAnchor!!,
                        colorCorrectionRgba
                    )

                else -> {
                }
            }
        }
    }

    private suspend fun setupAugmentedImageDatabaseFromUrl(config: Config): Boolean {
        try {
            val augmentedImageBitmap = loadAugmentedImageUrlBitmap()
            val augmentedImageDatabase = AugmentedImageDatabase(session).apply {
                addImage("image_name", augmentedImageBitmap)
            }
            config.augmentedImageDatabase = augmentedImageDatabase
            fitToScanView?.visibility = View.VISIBLE
            surfaceView?.visibility = View.VISIBLE
            binding.pbLoading.visibility = View.GONE

        } catch (e: NullPointerException) {
//            messageSnackbarHelper.showError(
//                this@MainActivity,
//                "Minimal kasi foto yg bener"
//            )
        } catch (e: ImageInsufficientQualityException) {
//            messageSnackbarHelper.showError(
//                this@MainActivity,
//                "Fotonya jelek (too few features)"
//            )
        } catch (e: Exception) {
            messageSnackbarHelper.showError(
                this@MainActivity,
                e.message
            )
        }

        return true
    }

    private suspend fun loadAugmentedImageUrlBitmap(): Bitmap? {
        return try {
            withContext(Dispatchers.IO) {
                val url = URL(url)
                BitmapFactory.decodeStream(url.openConnection().getInputStream())
            }
        } catch (e: IOException) {
            println(e)
            null
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
