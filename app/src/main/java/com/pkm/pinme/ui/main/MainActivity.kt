package com.pkm.pinme.ui.main

import android.content.Intent
import android.media.CamcorderProfile
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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

class MainActivity : AppCompatActivity(), FragmentOnAttachListener, OnSessionConfigurationListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var arFragment: ArFragment
    private var rabbitDetected = false

    // AR
    private var markerUrl: String? = null
    private var arUrl: String? = null
    private var soundUrl: String? = null

    // ViewModel
    private lateinit var factory: ViewModelFactory
    private val viewModel: MainViewModel by viewModels { factory }

    var recordingWidth: Int = 0
    var recordingHeight: Int = 0

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

        viewModel.configureAr(arUrl!!, soundUrl!!, markerUrl!!, this)

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

    private fun setupUI() {
        viewModel.isLoading.observe(this) {
            if (it){
                binding.loadingCard.visibility = View.VISIBLE
                binding.fabRecordBtn.visibility = View.GONE
                binding.fabShotBtn.visibility = View.GONE
            } else {
                binding.loadingCard.visibility = View.GONE
                binding.fabRecordBtn.visibility = View.VISIBLE
                binding.fabShotBtn.visibility = View.VISIBLE
            }
        }

        binding.fabRecordBtn.setOnClickListener {
            viewModel.arSound?.start()
            viewModel.videoRecorder.setSceneView(arFragment.arSceneView)
            val orientation = this.getResources().configuration.orientation;
            viewModel.videoRecorder.setVideoQuality(CamcorderProfile.QUALITY_720P, orientation)
            viewModel.videoRecorder.setVideoSize(recordingWidth, recordingHeight)
            viewModel.videoRecorder.setFrameRate(60)

            val isRecording = viewModel.videoRecorder.onToggleRecord()
            if (isRecording){
                Toast.makeText(this,"Recording",Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this,"Stop Recording",Toast.LENGTH_LONG).show()
            }
        }

        binding.fabShotBtn.setOnClickListener {
            viewModel.takePhoto(this, arFragment)
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

                anchorNode.worldScale = Vector3(0.25f, 0.25f, 0.25f)
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
}

