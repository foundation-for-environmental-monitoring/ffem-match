package io.ffem.lite.qrcode

import android.graphics.Point
import android.graphics.PointF
import com.google.zxing.ResultPoint

class QRToViewPointTransformer {

    fun transform(
        qrPoints: Array<ResultPoint>, isMirrorPreview: Boolean,
        viewSize: Point, cameraPreviewSize: Point
    ): Array<PointF?> {
        val transformedPoints = arrayOfNulls<PointF>(qrPoints.size)
        for ((index, qrPoint) in qrPoints.withIndex()) {
            val transformedPoint = transform(
                qrPoint, isMirrorPreview, viewSize,
                cameraPreviewSize
            )
            transformedPoints[index] = transformedPoint
        }
        return transformedPoints
    }

    private fun transform(
        qrPoint: ResultPoint, isMirrorPreview: Boolean,
        viewSize: Point, cameraPreviewSize: Point
    ): PointF? {
        val previewX = cameraPreviewSize.x.toFloat()
        val previewY = cameraPreviewSize.y.toFloat()

        val transformedPoint: PointF?
        val scaleX: Float
        val scaleY: Float

        scaleX = viewSize.x / previewY
        scaleY = viewSize.y / previewX
        transformedPoint = PointF((previewY - qrPoint.y) * scaleX, qrPoint.x * scaleY)
        if (isMirrorPreview) {
            transformedPoint.y = viewSize.y - transformedPoint.y
        }
        return transformedPoint
    }
}
