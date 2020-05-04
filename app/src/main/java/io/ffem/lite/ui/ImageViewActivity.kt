package io.ffem.lite.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.navigation.findNavController
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.model.TestInfo

class ImageViewActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)
        val navController = findNavController(R.id.fragment_container)

        val testInfo = intent.getParcelableExtra<TestInfo>(App.TEST_INFO_KEY)!!

        val bundle = Bundle()
        bundle.putParcelable(App.TEST_INFO_KEY, testInfo)
        navController.setGraph(navController.graph, bundle)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
