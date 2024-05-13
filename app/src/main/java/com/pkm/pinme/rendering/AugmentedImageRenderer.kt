package com.pkm.pinme.rendering

import android.content.Context
import com.google.ar.core.Anchor
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Pose
import com.pkm.common.rendering.ObjectRenderer
import com.pkm.common.rendering.ObjectRenderer.BlendMode

import java.io.IOException

class AugmentedImageRenderer {
    private val image = ObjectRenderer()

    @Throws(IOException::class)
    fun createOnGlThread(context: Context) {
        image.createOnGlThread(
            context, "models/andy.obj", "models/andy_spec.png"
        )
        image.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f)
        image.setBlendMode(BlendMode.AlphaBlending)
    }

    fun draw(
        viewMatrix: FloatArray, projectionMatrix: FloatArray, augmentedImage: AugmentedImage,
        centerAnchor: Anchor, colorCorrectionRgba: FloatArray
    ) {
        val tintColor = convertHexToColor(
            TINT_COLORS_HEX[augmentedImage.index % TINT_COLORS_HEX.size]
        )

        val localBoundaryPoses = arrayOf(
            Pose.makeRotation(0.0f, 0.5f, 0.0f, 0.5f)
                .compose(Pose.makeTranslation(
                    0.0f * augmentedImage.extentX, 0.0f, 0.0f * augmentedImage.extentX
                ))
        )

        val anchorPose = centerAnchor.pose
        val worldBoundaryPoses = Array(1) { Pose.IDENTITY }
//        for (i in 0..3) {
            worldBoundaryPoses[0] = anchorPose.compose(localBoundaryPoses[0])
//        }

        val scaleFactor = 1.0f
        val modelMatrix = FloatArray(16)

        worldBoundaryPoses[0].toMatrix(modelMatrix, 0)
        image.updateModelMatrix(modelMatrix, scaleFactor)
        image.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor)
    }

    private fun convertHexToColor(colorHex: Int): FloatArray {
        val red = ((colorHex and 0xFF0000) shr 16) / 255.0f * TINT_INTENSITY
        val green = ((colorHex and 0x00FF00) shr 8) / 255.0f * TINT_INTENSITY
        val blue = (colorHex and 0x0000FF) / 255.0f * TINT_INTENSITY
        return floatArrayOf(red, green, blue, TINT_ALPHA)
    }

    companion object {
        private const val TAG = "AugmentedImageRenderer"
        private const val TINT_INTENSITY = 0.1f
        private const val TINT_ALPHA = 1.0f
        private val TINT_COLORS_HEX = intArrayOf(
            0x000000, 0xF44336, 0xE91E63, 0x9C27B0, 0x673AB7, 0x3F51B5, 0x2196F3, 0x03A9F4, 0x00BCD4,
            0x009688, 0x4CAF50, 0x8BC34A, 0xCDDC39, 0xFFEB3B, 0xFFC107, 0xFF9800
        )
    }
}
