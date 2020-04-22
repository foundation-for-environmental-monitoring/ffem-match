package io.ffem.lite.di

import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import io.ffem.lite.app.App
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        ActivityModule::class,
        AppModule::class
    ]
)
interface AppComponent : AndroidInjector<App> {
    @Component.Factory
    abstract class Factory : AndroidInjector.Factory<App>
}
