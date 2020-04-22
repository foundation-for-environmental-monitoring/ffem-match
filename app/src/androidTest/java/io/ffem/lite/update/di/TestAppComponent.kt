package io.ffem.lite.update.di

import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import io.ffem.lite.app.App
import io.ffem.lite.di.ActivityModule
import io.ffem.lite.di.AppComponent
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        ActivityModule::class,
        TestAppModule::class
    ]
)
interface TestAppComponent : AppComponent {

    @Component.Factory
    abstract class Factory : AndroidInjector.Factory<App>

    fun fakeAppUpdateManager(): FakeAppUpdateManager
}
