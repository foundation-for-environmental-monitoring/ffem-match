package io.ffem.lite.ui

import android.net.Uri
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.view.MenuItem
import android.view.View
import io.ffem.lite.R
import io.ffem.lite.app.App.Companion.TEST_ID_KEY
import io.ffem.lite.app.App.Companion.TEST_NAME_KEY
import kotlinx.android.synthetic.main.activity_result.*
import java.io.File

class ResultActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        setTitle(R.string.app_name)

        val id = intent.getStringExtra(TEST_ID_KEY)
        val name = intent.getStringExtra(TEST_NAME_KEY)

        val path = getExternalFilesDir(DIRECTORY_PICTURES).toString() +
                File.separator + "captures" + File.separator

        val file = File(path + id + "_" + name + ".jpg")
        if (file.exists()) {
            image.setImageURI(Uri.fromFile(file))
        }
    }

    fun onHomeClick(@Suppress("UNUSED_PARAMETER") view: View) {
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}