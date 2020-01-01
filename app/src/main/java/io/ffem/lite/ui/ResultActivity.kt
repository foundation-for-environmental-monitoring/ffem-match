package io.ffem.lite.ui

import android.net.Uri
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.TEST_ID_KEY
import io.ffem.lite.app.App.Companion.TEST_NAME_KEY
import kotlinx.android.synthetic.main.activity_result.*
import java.io.File

class ResultActivity : BaseActivity() {

    private lateinit var localPath: String
    private lateinit var serverPath: String
    private var localMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val id = intent.getStringExtra(TEST_ID_KEY)!!
        val name = intent.getStringExtra(TEST_NAME_KEY)!!

        val path = getExternalFilesDir(DIRECTORY_PICTURES).toString() +
                File.separator + "captures" + File.separator

        val fileName = name.replace(" ", "")
        localPath = path + id + "_" + fileName + "_swatch" + ".jpg"
        serverPath = path + id + "_" + fileName + ".jpg"

        textResultTitle.text = App.getVersionName()

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
