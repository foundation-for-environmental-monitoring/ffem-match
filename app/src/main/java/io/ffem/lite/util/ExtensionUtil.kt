package io.ffem.lite.util

import io.ffem.lite.app.App
import java.util.*


fun String.toLocalString(): String {
    val value = this.toLowerCase(Locale.ROOT)
        .replace(")", "")
        .replace("(", "")
        .replace("- ", "")
        .replace(" ", "_")
    val resourceId = App.app.resources
        .getIdentifier(
            value, "string",
            App.app.packageName
        )
    return if (resourceId > 0) {
        App.app.getString(resourceId)
    } else {
        this
    }
}
