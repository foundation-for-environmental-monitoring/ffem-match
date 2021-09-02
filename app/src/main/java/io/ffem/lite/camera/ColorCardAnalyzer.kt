package io.ffem.lite.camera

import android.content.Context
import androidx.camera.core.ImageProxy
import io.ffem.lite.common.Constants.SWATCH_AREA_HEIGHT_PERCENTAGE
import io.ffem.lite.common.Constants.SWATCH_AREA_WIDTH_PERCENTAGE

class ColorCardAnalyzer(context: Context) : ColorCardAnalyzerBase(context) {
    override fun analyze(imageProxy: ImageProxy) {
        isCircleSwatch = false
        swatchWidthPercentage = SWATCH_AREA_WIDTH_PERCENTAGE
        swatchHeightPercentage = SWATCH_AREA_HEIGHT_PERCENTAGE
        super.analyze(imageProxy)
    }
}
