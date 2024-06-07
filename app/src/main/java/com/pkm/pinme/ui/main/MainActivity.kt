//package com.pkm.pinme.ui.main
//
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.os.Bundle
//import android.util.Log
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.lifecycleScope
//import com.google.ar.core.Config
//import com.pkm.pinme.databinding.ActivityMainBinding
//import io.github.sceneview.ar.arcore.addAugmentedImage
//import io.github.sceneview.ar.arcore.getUpdatedAugmentedImages
//import io.github.sceneview.ar.node.AugmentedImageNode
//import io.github.sceneview.math.Position
//import io.github.sceneview.node.ModelNode
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.runBlocking
//import kotlinx.coroutines.withContext
//import java.io.IOException
//import java.net.URL
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//
//    private val augmentedImageNodes = mutableListOf<AugmentedImageNode>()
//
//    private var model : ModelNode? = null
//    private var markerUrl : String? = "https://yt3.googleusercontent.com/cFv0K1fb4MjrZcnq0ntEkRU99ADunvfvFZ1APEr6nmJlRsAMwKPYVpiUb2C1UxfUjc14ajM=s900-c-k-c0x00ffffff-no-rj"
//    private var arUrl : String? = "https://github.com/Pin-Me/dummy-files/blob/main/ak47.glb?raw=true"
//    private var soundUrl : String? = "https://github.com/Pin-Me/dummy-files/blob/main/guiro-sweep-156002.mp3?raw=true"
//
//    private var markerBitmap : Bitmap? = null
//    override fun onCreate(savedInstanceState: Bundle?) {
//        runBlocking {
//            markerBitmap = loadAugmentedImageUrlBitmap(markerUrl!!)
//        }
//
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        binding.sceneView.apply {
//            configureSession { session, config ->
//                config.setFocusMode(Config.FocusMode.AUTO)
//                config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED)
//                config.addAugmentedImage(
//                    session,
//                    "rabbit",
//                    markerBitmap!!
//                )
//            }
//            onSessionUpdated = { _, frame ->
//                frame.getUpdatedAugmentedImages().forEach { augmentedImage ->
//                    if (augmentedImageNodes.none { it.imageName == augmentedImage.name }) {
//                        val augmentedImageNode = AugmentedImageNode(engine, augmentedImage).apply {
//
//                            when (augmentedImage.name) {
//                                "rabbit" -> model?.let {
//                                    addChildNode(
//                                        it
//                                    )
//                                }
//                            }
//                        }
//                        addChildNode(augmentedImageNode)
//                        augmentedImageNodes += augmentedImageNode
//                    }
//                }
//            }
//        }
//        lifecycleScope.launch {
//            model = buildModelNode()
//
//        }
//    }
//    private suspend fun buildModelNode(): ModelNode? {
//        binding.sceneView.modelLoader.loadModelInstance(
//            arUrl!!
//        )?.let { modelInstance ->
//            return ModelNode(
//                autoAnimate = true,
//
//                modelInstance = modelInstance,
//                // Scale to fit in a 0.5 meters cube
//                scaleToUnits = 0.1f,
//                // Bottom origin instead of center so the model base is on floor
//                centerOrigin = Position(y = -0.1f)
//            ).apply {
//                isEditable = true
//                isShadowReceiver = false
//                isShadowCaster = false
//            }
//        }
//        return null
//    }
//
//    private suspend fun loadAugmentedImageUrlBitmap(imgUrl: String): Bitmap? {
//        return try {
//            withContext(Dispatchers.IO) {
//                BitmapFactory.decodeStream(URL(imgUrl).openConnection().getInputStream())
//            }
//        } catch (e: IOException) {
//            println(e)
//            null
//        }
//    }
//}