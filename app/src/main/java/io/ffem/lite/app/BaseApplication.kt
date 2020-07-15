@file:Suppress("Annotator", "Annotator", "Annotator")

package io.ffem.lite.app

import android.annotation.SuppressLint
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import io.ffem.lite.di.DaggerAppComponent

@SuppressLint("Registered")
open class BaseApplication : DaggerApplication(), CameraXConfig.Provider {

    /** @returns Camera2 default configuration */
    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponent.factory().create(this as App?)
    }
}

