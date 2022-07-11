package io.ffem.lite.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Build
import android.text.*
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import io.ffem.lite.R
import io.ffem.lite.model.TestInfo
import io.ffem.lite.widget.CenteredImageSpan
import java.util.*
import java.util.regex.Pattern
import kotlin.text.Typography.ellipsis

object StringUtil {
    @JvmStatic
    fun getStringResourceByName(context: Context, theKey: String): Spanned {
        return getStringResourceByName(
            context, theKey,
            context.resources.configuration.locale.language
        )
    }

    @JvmStatic
    fun getStringResourceByName(context: Context, theKey: String, language: String): Spanned {
        val key = theKey.trim { it <= ' ' }
        val packageName = context.packageName
        val resId = context.resources.getIdentifier(key, "string", packageName)
        return if (resId == 0) {
            Spannable.Factory.getInstance().newSpannable(fromHtml(key))
        } else {
            if (language.isNotEmpty()) {
                try {
                    Spannable.Factory.getInstance().newSpannable(
                        getLocalizedResources(context, Locale(language)).getText(resId)
                    )
                } catch (e: Exception) {
                    return Spannable.Factory.getInstance().newSpannable(key)
                }
            } else {
                Spannable.Factory.getInstance().newSpannable(context.getText(resId))
            }
        }
    }

    private fun getLocalizedResources(context: Context, desiredLocale: Locale): Resources {
        var conf = context.resources.configuration
        conf = Configuration(conf)
        conf.setLocale(desiredLocale)
        val localizedContext = context.createConfigurationContext(conf)
        return localizedContext.resources
    }

    private fun fromHtml(html: String?): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
    }

    @JvmStatic
    fun toInstruction(
        context: AppCompatActivity,
        testInfo: TestInfo?,
        instructionText: String
    ): SpannableStringBuilder {
        var text = instructionText
        val builder = SpannableStringBuilder()
        var isBold = false
        if (text.startsWith("<b>") && text.endsWith("</b>")) {
            isBold = true
            text = text.replace("<b>", "").replace("</b>", "")
        }
        val spanned = getStringResourceByName(context, text)
        builder.append(spanned)
        if (isBold) {
            val boldSpan = StyleSpan(Typeface.BOLD)
            builder.setSpan(
                boldSpan,
                0,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        val m = Pattern.compile("\\(\\*(\\w+)\\*\\)").matcher(builder)
        while (m.find()) {
            val resId = context.resources.getIdentifier(
                "button_" + m.group(1),
                "drawable", context.packageName
            )
            if (resId > 0) {
                builder.setSpan(
                    CenteredImageSpan(context, resId),
                    m.start(0), m.end(0), Spannable.SPAN_EXCLUSIVE_INCLUSIVE
                )
            }
        }

        // Set reagent in the string
        replaceReagentTags(testInfo, builder)

        // Set sample quantity in the string
        val m1 = Pattern.compile("%sampleQuantity").matcher(builder)
        while (m1.find()) {
            builder.replace(m1.start(), m1.end(), testInfo!!.sampleQuantity.toString())
        }

        // Set sample quantity in the string
        val m3 = Pattern.compile("%dilutedSampleQty").matcher(builder)
        while (m3.find()) {
            builder.replace(
                m3.start(), m3.end(),
                java.lang.String.valueOf(testInfo!!.sampleQuantity / testInfo.dilution)
            )
        }

        // Set sample quantity in the string
        val m4 = Pattern.compile("%distilledQty").matcher(builder)
        while (m4.find()) {
            builder.replace(
                m4.start(), m4.end(),
                java.lang.String.valueOf(
                    testInfo!!.sampleQuantity - testInfo.sampleQuantity / testInfo.dilution
                )
            )
        }


        if (testInfo != null) {
            // Set reaction time in the string
            for (i in 1..4) {
                val m2 = Pattern.compile("%reactionTime$i").matcher(builder)
                while (m2.find()) {
                    if (testInfo.getReagent(i - 1).reactionTime != null) {
                        builder.replace(
                            m2.start(), m2.end(),
                            context.resources.getQuantityString(
                                R.plurals.minutes,
                                testInfo.getReagent(i - 1).reactionTime!!,
                                testInfo.getReagent(i - 1).reactionTime
                            )
                        )
                    }
                }
            }
        }
        insertDialogLinks(context, builder)
        return builder
    }

    private fun insertDialogLinks(context: AppCompatActivity, builder: SpannableStringBuilder) {
        if (builder.toString().contains("[a topic=")) {
            val startIndex = builder.toString().indexOf("[a topic=")
            val topic: String
            val p = Pattern.compile("\\[a topic=(.*?)]")
            val m3 = p.matcher(builder)
            if (m3.find()) {
                topic = m3.group(1)!!
                builder.replace(m3.start(), m3.end(), "")
                val endIndex = builder.toString().indexOf("[/a]")
                builder.replace(endIndex, endIndex + 4, "")
                val clickableSpan: ClickableSpan = object : ClickableSpan() {
                    override fun onClick(textView: View) {
//                        if (topic.equals("sulfide", ignoreCase = true)) {
//                            val newFragment: DialogFragment = SulfideDialogFragment()
//                            newFragment.show(context.supportFragmentManager, "sulfideDialog")
//                        }
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                    }
                }
                builder.setSpan(
                    clickableSpan,
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.setSpan(UnderlineSpan(), startIndex, endIndex, 0)
            }
        }
    }

    private fun replaceReagentTags(testInfo: TestInfo?, builder: SpannableStringBuilder) {
        for (i in 1..4) {
            val m1 = Pattern.compile("%reagent$i").matcher(builder)
            while (m1.find()) {
                var name = testInfo!!.getReagent(i - 1).name
                val code = testInfo.getReagent(i - 1).code
                if (code!!.isNotEmpty()) {
                    name = String.format("%s (%s)", name, code)
                }
                builder.replace(m1.start(), m1.end(), name)
            }
        }
    }

    fun ellipsize(input: String?, maxLen: Int): String? {
        if (input == null) return null
        return if (input.length < maxLen || maxLen < 2) input else input.substring(
            0,
            maxLen - 3
        ) + ellipsis
    }

    fun getStringByLocale(context: Context, resourceId: Int, desiredLocale: Locale?): String {
        var conf: Configuration = context.resources.configuration
        conf = Configuration(conf)
        conf.setLocale(desiredLocale)
        val localizedContext: Context = context.createConfigurationContext(conf)
        return localizedContext.resources.getString(resourceId)
    }
}