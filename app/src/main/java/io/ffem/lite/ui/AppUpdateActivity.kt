package io.ffem.lite.ui

import android.os.Bundle
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.TaskExecutors

private const val IMMEDIATE_UPDATE_REQUEST_CODE = 1000

abstract class AppUpdateActivity : BaseActivity() {

    private lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appUpdateManager = AppUpdateManagerFactory.create(this)

//        if (isAppUpdateCheckRequired()) {
        try {
            checkInAppUpdate()
        } catch (_: Exception) {
        }
//        }
    }

    private fun checkInAppUpdate() {
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener(TaskExecutors.MAIN_THREAD) { appUpdateInfo ->
                when (appUpdateInfo.updateAvailability()) {
                    UpdateAvailability.UPDATE_AVAILABLE -> when {
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> startImmediateUpdate(
                            appUpdateInfo
                        )
                        else -> {
                            // No update is allowed
                        }
                    }
                    else -> {
                        // No op
                    }
                }
            }
    }

    private fun startImmediateUpdate(appUpdateInfo: AppUpdateInfo?) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo!!,
            AppUpdateType.IMMEDIATE,
            this,
            IMMEDIATE_UPDATE_REQUEST_CODE
        )

        // saveLastAppUpdateCheck()
    }
}