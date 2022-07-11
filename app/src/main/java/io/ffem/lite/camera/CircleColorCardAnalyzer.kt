package io.ffem.lite.camera

import android.content.Context
import androidx.camera.core.ImageProxy
import io.ffem.lite.common.Constants.SWATCH_CIRCLE_SIZE_PERCENTAGE
import io.ffem.lite.model.TestInfo

class CircleColorCardAnalyzer(testInfo: TestInfo, context: Context) :
    ColorCardAnalyzerBase(testInfo, context) {
    override fun analyze(imageProxy: ImageProxy) {
        colorCardType = 2
        swatchWidthPercentage = SWATCH_CIRCLE_SIZE_PERCENTAGE
        super.analyze(imageProxy)
    }
}
