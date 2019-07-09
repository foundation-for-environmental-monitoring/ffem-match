package io.ffem.lite.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import io.ffem.lite.R
import io.ffem.lite.preference.SettingsActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    fun onSettingsClick(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        val intent = Intent(baseContext, SettingsActivity::class.java)
        startActivity(intent)
    }
}
