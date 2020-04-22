package io.ffem.lite.update

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import io.ffem.lite.BuildConfig
import io.ffem.lite.common.TestHelper
import io.ffem.lite.common.TestHelper.mDevice
import io.ffem.lite.ui.ResultListActivity
import io.ffem.lite.update.di.TestInjector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

// https://github.com/malvinstn/FakeAppUpdateManagerSample
@RunWith(AndroidJUnit4::class)
class UpdateTest {

    private lateinit var fakeAppUpdateManager: FakeAppUpdateManager

    companion object {
        @JvmStatic
        @BeforeClass
        fun initialize() {
            if (!TestHelper.isDeviceInitialized()) {
                mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            }
        }
    }

    @Before
    fun setUp() {
        val component = TestInjector.inject()
        fakeAppUpdateManager = component.fakeAppUpdateManager()
    }

    @Test
    fun mainActivity_Update_Completes() {
        fakeAppUpdateManager.partiallyAllowedUpdateType = AppUpdateType.IMMEDIATE
        fakeAppUpdateManager.setUpdateAvailable(BuildConfig.VERSION_CODE + 1)

        ActivityScenario.launch(ResultListActivity::class.java)

        SystemClock.sleep(3000)

        assertTrue(fakeAppUpdateManager.isImmediateFlowVisible)

        fakeAppUpdateManager.userAcceptsUpdate()

        fakeAppUpdateManager.downloadStarts()

        fakeAppUpdateManager.downloadCompletes()

        assertTrue(fakeAppUpdateManager.isInstallSplashScreenVisible)
    }

    @Test
    fun mainActivity_Update_DownloadFails() {
        fakeAppUpdateManager.partiallyAllowedUpdateType = AppUpdateType.IMMEDIATE
        fakeAppUpdateManager.setUpdateAvailable(BuildConfig.VERSION_CODE + 1)

        ActivityScenario.launch(ResultListActivity::class.java)

        SystemClock.sleep(3000)

        assertTrue(fakeAppUpdateManager.isImmediateFlowVisible)

        fakeAppUpdateManager.userAcceptsUpdate()

        fakeAppUpdateManager.downloadStarts()

        fakeAppUpdateManager.downloadFails()

        SystemClock.sleep(3000)

        assertFalse(fakeAppUpdateManager.isInstallSplashScreenVisible)
    }
}