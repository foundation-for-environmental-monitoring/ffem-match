package io.ffem.lite.common

import android.os.Build

object TestUtil {

    val isEmulator: Boolean
        get() = ((Build.ID.contains("KOT49H") && Build.MODEL.contains("MLA-AL10"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.HOST.startsWith("SWDG2909")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic")
                && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT)

}
