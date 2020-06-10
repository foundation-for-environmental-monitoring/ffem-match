package io.ffem.lite.util

import android.content.Context
import android.content.res.Configuration
import java.util.*

fun getStringByLocale(context: Context, resourceId: Int, desiredLocale: Locale?): String {
    var conf: Configuration = context.resources.configuration
    conf = Configuration(conf)
    conf.setLocale(desiredLocale)
    val localizedContext: Context = context.createConfigurationContext(conf)
    return localizedContext.resources.getString(resourceId)
}