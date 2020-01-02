package io.ffem.lite.app

import android.annotation.SuppressLint
import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

@SuppressLint("Registered")
open class BaseApplication : Application(), CameraXConfig.Provider {

    /** @returns Camera2 default configuration */
    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }
}

