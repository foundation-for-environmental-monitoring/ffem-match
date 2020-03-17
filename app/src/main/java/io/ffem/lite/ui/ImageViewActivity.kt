package io.ffem.lite.ui

import android.net.Uri
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import androidx.core.content.ContextCompat
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.TEST_INFO_KEY
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.isDiagnosticMode
import kotlinx.android.synthetic.main.activity_image_view.*
import java.io.File

class ImageViewActivity : BaseActivity() {

    private lateinit var localPath: String
    private lateinit var serverPath: String
    private var localMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)

        val testInfo = intent.getParcelableExtra<TestInfo>(TEST_INFO_KEY)

        val path = getExternalFilesDir(DIRECTORY_PICTURES).toString() +
                File.separator + "captures" + File.separator

        val fileName = testInfo!!.name!!.replace(" ", "")
        localPath = path + testInfo.fileName + "_" + fileName + "_swatch" + ".jpg"
        serverPath = path + testInfo.fileName + "_" + fileName + ".jpg"

        textResultTitle.text = App.getVersionName()

        if (isDiagnosticMode()) {
            app_bar.setBackgroundColor(ContextCompat.getColor(this, R.color.diagnostic))
        }

        showLocalImage()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showLocalImage() {
        val file = File(localPath)
        imageModeButton.text = getString(R.string.show_server_image)
        if (file.exists()) {
            image.setImageURI(Uri.fromFile(file))
            image.refreshDrawableState()
        } else {
            imageModeButton.visibility = GONE
            showServerImage()
            return
        }
        localMode = true
    }

    private fun showServerImage() {
        val file = File(serverPath)
        if (file.exists()) {
            image.setImageURI(Uri.fromFile(file))
        }
        imageModeButton.text = getString(R.string.show_local_image)
        localMode = false
    }

    fun onHomeClick(@Suppress("UNUSED_PARAMETER") view: View) {
        finish()
    }

    fun onImageTypeClick(@Suppress("UNUSED_PARAMETER") view: View) {
        when {
            localMode -> showServerImage()
            else -> showLocalImage()
        }
    }
}
