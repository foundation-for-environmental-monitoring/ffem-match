@file:Suppress("KotlinDeprecation")

package io.ffem.lite.update.di

import androidx.test.platform.app.InstrumentationRegistry
import io.ffem.lite.app.App

object TestInjector {
    fun inject(): TestAppComponent {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as App
        return DaggerTestAppComponent.factory()
            .create(application)
            .also { it.inject(application) } as TestAppComponent
    }
}
