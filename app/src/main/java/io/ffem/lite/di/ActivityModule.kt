package io.ffem.lite.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import io.ffem.lite.ui.ResultListActivity

@Suppress("unused")
@Module
interface ActivityModule {
    @ContributesAndroidInjector
    fun contributesMainActivity(): ResultListActivity
}
