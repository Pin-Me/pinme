//package com.pkm.pinme.ui.main
//
//import android.graphics.BitmapFactory
//import android.os.Bundle
//import androidx.fragment.app.Fragment
//import android.view.View
//import com.pkm.pinme.R
//import io.github.sceneview.ar.ARSceneView
//import io.github.sceneview.ar.arcore.addAugmentedImage
//import io.github.sceneview.ar.arcore.getUpdatedAugmentedImages
//import io.github.sceneview.ar.node.AugmentedImageNode
//import io.github.sceneview.node.ModelNode
//import io.github.sceneview.math.Position
//
//class MainFragment : Fragment(R.layout.fragment_main) {
//
//    lateinit var sceneView: ARSceneView
//
//    val augmentedImageNodes = mutableListOf<AugmentedImageNode>()
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        sceneView = view.findViewById<ARSceneView>(R.id.sceneView).apply {
//            configureSession { session, config ->
//                config.addAugmentedImage(
//                    session, "rabbit",
//                    requireContext().assets.open("augmentedimages/marker.jpeg")
//                        .use(BitmapFactory::decodeStream)
//                )
//            }
//            onSessionUpdated = { session, frame ->
//                frame.getUpdatedAugmentedImages().forEach { augmentedImage ->
//                    if (augmentedImageNodes.none { it.imageName == augmentedImage.name }) {
//                        val augmentedImageNode = AugmentedImageNode(engine, augmentedImage).apply {
//                            when (augmentedImage.name) {
//                                "rabbit" -> addChildNode(
//                                    ModelNode(
//                                        modelInstance = modelLoader.createModelInstance(
//                                            assetFileLocation = "models/rabbit.glb"
//                                        ),
//                                        scaleToUnits = 0.1f,
//                                        centerOrigin = Position(0.0f)
//                                    )
//                                )
//                            }
//                        }
//                        addChildNode(augmentedImageNode)
//                        augmentedImageNodes += augmentedImageNode
//                    }
//                }
//            }
//        }
//    }
//
//    companion object {
//
//    }
//}