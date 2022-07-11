package io.ffem.lite.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.ffem.lite.R

object AlertUtil {

    fun showAlert(
        context: Context, @StringRes title: Int, message: String,
        @StringRes okButtonText: Int,
        positiveListener: DialogInterface.OnClickListener?,
        negativeListener: DialogInterface.OnClickListener?,
        cancelListener: DialogInterface.OnCancelListener?
    ): AlertDialog {
        return showAlert(
            context, context.getString(title), message, okButtonText, R.string.cancel,
            true, positiveListener = positiveListener, negativeListener = negativeListener,
            cancelListener = cancelListener
        )
    }

    private fun showAlert(
        context: Context, title: String, message: String,
        @StringRes okButtonText: Int, @StringRes cancelButtonText: Int,
        cancelable: Boolean, positiveListener: DialogInterface.OnClickListener?,
        negativeListener: DialogInterface.OnClickListener?,
        cancelListener: DialogInterface.OnCancelListener?
    ): AlertDialog {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(title)
            .setMessage(message)
            .setCancelable(cancelable)
        if (positiveListener != null) {
            builder.setPositiveButton(okButtonText, positiveListener)
        } else if (negativeListener == null) {
            builder.setNegativeButton(okButtonText) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
        }
        if (negativeListener != null) {
            builder.setNegativeButton(cancelButtonText, negativeListener)
        }
        builder.setOnCancelListener(cancelListener)
        val alertDialog = builder.create()
        alertDialog.show()
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(
            ThemeUtils(context).colorOnSurface
        )
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(
            ThemeUtils(context).colorOnSurface
        )
        return alertDialog
    }

    @JvmStatic
    @SuppressLint("InflateParams")
    fun showError(
        context: Context, @StringRes title: Int,
        message: String, bitmap: Bitmap?,
        @StringRes okButtonText: Int,
        positiveListener: DialogInterface.OnClickListener?,
        negativeListener: DialogInterface.OnClickListener?,
        cancelListener: DialogInterface.OnCancelListener?
    ): AlertDialog {
        if (bitmap == null) {
            return showAlert(
                context, context.getString(title), message, okButtonText,
                R.string.cancel, cancelable = false, isDestructive = false,
                positiveListener = positiveListener, negativeListener = negativeListener,
                cancelListener = cancelListener
            )
        }
        val alertDialog: AlertDialog
        val alertView: View
        val builder = MaterialAlertDialogBuilder(context)
        val inflater = LayoutInflater.from(context)
        alertView = inflater.inflate(R.layout.dialog_error, null, false)
        builder.setView(alertView)
        builder.setTitle(R.string.error)
        builder.setMessage(message)
//        val image = alertView.findViewById<ImageView>(R.id.imageSample)
//        image.setImageBitmap(bitmap)
        if (positiveListener != null) {
            builder.setPositiveButton(okButtonText, positiveListener)
        }
        if (negativeListener == null) {
            builder.setNegativeButton(R.string.cancel, null)
        } else {
            var buttonText = R.string.cancel
            if (positiveListener == null) {
                buttonText = okButtonText
            }
            builder.setNegativeButton(buttonText, negativeListener)
        }
        builder.setCancelable(false)
        alertDialog = builder.create()
        if (context is Activity) {
            alertDialog.setOwnerActivity(context)
        }
        alertDialog.show()
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(
            ThemeUtils(context).colorOnSurface
        )
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(
            ThemeUtils(context).colorOnSurface
        )
        return alertDialog
    }

    private fun showAlert(
        context: Context, title: String, message: String,
        @StringRes okButtonText: Int, @StringRes cancelButtonText: Int,
        cancelable: Boolean, isDestructive: Boolean,
        positiveListener: DialogInterface.OnClickListener?,
        negativeListener: DialogInterface.OnClickListener?,
        cancelListener: DialogInterface.OnCancelListener?
    ): AlertDialog {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(title)
            .setMessage(message)
            .setCancelable(cancelable)
        if (positiveListener != null) {
            builder.setPositiveButton(okButtonText, positiveListener)
        } else if (negativeListener == null) {
            builder.setNegativeButton(okButtonText) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
        }
        if (negativeListener != null) {
            builder.setNegativeButton(cancelButtonText, negativeListener)
        }
        builder.setOnCancelListener(cancelListener)
        val alertDialog = builder.create()
        alertDialog.show()
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(
            ThemeUtils(context).colorOnSurface
        )
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(
            ThemeUtils(context).colorOnSurface
        )
        return alertDialog
    }

    fun askQuestion(
        context: Context, @StringRes title: Int, @StringRes message: Int,
        @StringRes okButtonText: Int, @StringRes cancelButtonText: Int,
        isDestructive: Boolean,
        positiveListener: DialogInterface.OnClickListener?,
        cancelListener: DialogInterface.OnClickListener?
    ) {
        showAlert(
            context, context.getString(title), context.getString(message), okButtonText,
            cancelButtonText, true, isDestructive, positiveListener,
            cancelListener
                ?: DialogInterface.OnClickListener { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() },
            null
        )
    }


}