@file:Suppress("DEPRECATION")

package io.ffem.lite.ui

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import io.ffem.lite.common.Constants
import io.ffem.lite.preference.AppPreferences.getCameraFocusMode
import io.ffem.lite.preference.AppPreferences.getCameraResolution
import io.ffem.lite.preference.AppPreferences.getCameraZoom
import io.ffem.lite.preference.isDiagnosticMode
import timber.log.Timber
import java.io.IOException

class CameraPreview(context: Context?) : SurfaceView(context), SurfaceHolder.Callback {
    private val mHolder: SurfaceHolder
    lateinit var camera: Camera
        private set

    /**
     * Surface created.
     *
     * @param holder the holder
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            camera.setPreviewDisplay(holder)
            camera.startPreview()
        } catch (e: IOException) {
            Timber.d("Error setting camera preview: %s", e.message)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    /**
     * Surface changed.
     *
     * @param holder the holder
     * @param format the format
     * @param w      the width
     * @param h      the height
     */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        if (mHolder.surface == null) {
            // preview surface does not exist
            return
        }

        // stop preview before making changes
        try {
            camera.stopPreview()
        } catch (e: Exception) {
            // ignore: tried to stop a non-existent preview
        }

        // start preview with new settings
        try {
            camera.setPreviewDisplay(mHolder)
            camera.startPreview()
        } catch (e: Exception) {
            Timber.d("Error starting camera preview: %s", e.message)
        }
    }

    /**
     * Camera setup.
     *
     * @param camera the camera
     */
    fun setupCamera(camera: Camera) {
        this.camera = camera
        val parameters = camera.parameters
        val supportedWhiteBalance = camera.parameters.supportedWhiteBalance
        if (supportedWhiteBalance != null && supportedWhiteBalance.contains(
                Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT
            )
        ) {
            parameters.whiteBalance = Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT
        }
        val supportedSceneModes = camera.parameters.supportedSceneModes
        if (supportedSceneModes != null && supportedSceneModes.contains(Camera.Parameters.SCENE_MODE_AUTO)) {
            parameters.sceneMode = Camera.Parameters.SCENE_MODE_AUTO
        }
        val supportedColorEffects = camera.parameters.supportedColorEffects
        if (supportedColorEffects != null && supportedColorEffects.contains(Camera.Parameters.EFFECT_NONE)) {
            parameters.colorEffect = Camera.Parameters.EFFECT_NONE
        }
        val supportedPictureFormats = camera.parameters.supportedPictureFormats
        if (supportedPictureFormats != null && supportedPictureFormats.contains(ImageFormat.JPEG)) {
            parameters.pictureFormat = ImageFormat.JPEG
            parameters.jpegQuality = 100
        }
        val focusModes = parameters.supportedFocusModes
        val focusMode = getCameraFocusMode(focusModes)
        if (focusMode.isNotEmpty()) {
            parameters.focusMode = focusMode
        }
        if (parameters.maxNumMeteringAreas > 0) {
            val meteringAreas: MutableList<Camera.Area> = ArrayList()
            val areaRect1 = Rect(
                -METERING_AREA_SIZE, -METERING_AREA_SIZE,
                METERING_AREA_SIZE, METERING_AREA_SIZE
            )
            meteringAreas.add(Camera.Area(areaRect1, 1000))
            parameters.meteringAreas = meteringAreas
        }
        parameters.exposureCompensation = EXPOSURE_COMPENSATION
        if (parameters.isZoomSupported) {
            parameters.zoom = getCameraZoom(context)
        }
        camera.setDisplayOrientation(Constants.DEGREES_90)
        if (isDiagnosticMode()) {
            val resolution = getCameraResolution(context)
            parameters.setPictureSize(resolution.first, resolution.second)
        } else {
            parameters.setPictureSize(MIN_PICTURE_WIDTH, MIN_PICTURE_HEIGHT)
        }
        try {
            camera.parameters = parameters
        } catch (ex: Exception) {
            try {
                val supportedPictureSizes = parameters.supportedPictureSizes
                parameters.setPictureSize(
                    supportedPictureSizes[supportedPictureSizes.size - 1].width,
                    supportedPictureSizes[supportedPictureSizes.size - 1].height
                )
                for (size in supportedPictureSizes) {
                    if (size.width in (MIN_SUPPORTED_WIDTH + 1)..999) {
                        parameters.setPictureSize(size.width, size.height)
                        break
                    }
                }
                camera.parameters = parameters
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    companion object {
        private const val METERING_AREA_SIZE = 100
        private const val EXPOSURE_COMPENSATION = -2
        private const val MIN_PICTURE_WIDTH = 640
        private const val MIN_PICTURE_HEIGHT = 480
        private const val MIN_SUPPORTED_WIDTH = 400
    }

    init {
        try {
            camera = Camera.open()
        } catch (e: Exception) {
            Timber.e(e)
        }

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = holder
        mHolder.addCallback(this)
    }
}