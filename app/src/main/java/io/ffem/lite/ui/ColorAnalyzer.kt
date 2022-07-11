package io.ffem.lite.ui

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import io.ffem.lite.R
import io.ffem.lite.common.Constants
import io.ffem.lite.common.Constants.SAMPLE_CROP_LENGTH_DEFAULT
import io.ffem.lite.helper.SwatchHelper.analyzeColor
import io.ffem.lite.model.ResultInfo
import io.ffem.lite.model.TestInfo
import io.ffem.lite.util.ColorUtil
import io.ffem.lite.util.ImageUtil
import io.ffem.lite.util.ImageUtil.toBitmap
import io.ffem.lite.util.SoundUtil

class ColorAnalyzer(
    private val testInfo: TestInfo, private val listener: ColorListener,
    private val context: Context
) : ImageAnalysis.Analyzer {

    private val results = ArrayList<ResultInfo>()

//    private fun ByteBuffer.toByteArray(): ByteArray {
//        rewind()    // Rewind the buffer to zero
//        val data = ByteArray(remaining())
//        get(data)   // Copy the buffer into a byte array
//        return data // Return the byte array
//    }

    override fun analyze(image: ImageProxy) {
        if (done) {
            return
        }
        if (!capturePhoto) {
            image.close()
            return
        }
        frameCount++
        if (frameCount < 20) {
            if (frameCount == 1) {
                SoundUtil.playShortResource(context, R.raw.beep)
            }
            image.close()
            return
        }
        frameCount = 0
//        val buffer = image.planes[0].buffer
//        val data = buffer.toByteArray()
//        val pixels = data.map { it.toInt() }
//        listener(pixels.average().roundToInt())

        val croppedBitmap = ImageUtil.getCroppedBitmap(
            image.toBitmap(),
            SAMPLE_CROP_LENGTH_DEFAULT
        )

        //Extract the color from the photo which will be used for comparison
        val photoColor = ColorUtil.getColorFromBitmap(croppedBitmap, SAMPLE_CROP_LENGTH_DEFAULT)
        val subTest = testInfo.subTest()
        val resultInfo = analyzeColor(testInfo, photoColor.color, subTest.colors, context)
        resultInfo.sampleBitmap = croppedBitmap
        results.add(resultInfo)
        if (results.size > Constants.SAMPLING_COUNT_DEFAULT) {
            done = true
            listener(results, photoColor.color)
        }
        image.close()
    }

    fun takePhoto() {
        capturePhoto = true
    }

    fun reset() {
        done = false
        capturePhoto = false
        frameCount = 0
    }

    companion object {
        private var frameCount = 0
        private var capturePhoto: Boolean = false
        private var done: Boolean = false
    }
}