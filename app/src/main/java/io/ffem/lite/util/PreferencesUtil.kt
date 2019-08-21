package io.ffem.lite.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager

/**
 * Various utility functions to get/set values from/to SharedPreferences.
 */
object PreferencesUtil {

    /**
     * Gets a preference key from strings
     *
     * @param context the context
     * @param keyId   the key id
     * @return the string key
     */
    private fun getKey(context: Context, @StringRes keyId: Int): String {
        return context.getString(keyId)
    }

    /**
     * Gets a boolean value from preferences.
     *
     * @param context      the context
     * @param keyId        the key id
     * @param defaultValue the default value
     * @return the stored boolean value
     */
    fun getBoolean(context: Context, @StringRes keyId: Int, defaultValue: Boolean): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getBoolean(getKey(context, keyId), defaultValue)
    }

    /**
     * Sets a boolean value to preferences.
     *
     * @param context the context
     * @param keyId   the key id
     * @param value   the value
     */
    fun setBoolean(context: Context, @StringRes keyId: Int, value: Boolean) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        editor.putBoolean(getKey(context, keyId), value)
        editor.apply()
    }

    /**
     * Gets an integer value from preferences.
     *
     * @param context      the context
     * @param key          the key id
     * @param defaultValue the default value
     * @return stored int value
     */
    fun getInt(context: Context, key: String, defaultValue: Int): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getInt(key, defaultValue)
    }

    /**
     * Sets an integer value to preferences.
     *
     * @param context the context
     * @param key     the key id
     * @param value   the value to set
     */
    fun setInt(context: Context, key: String, value: Int) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    /**
     * Gets a string value from preferences.
     *
     * @param context      the context
     * @param keyId        the key id
     * @param defaultValue default value
     * @return the stored string value
     */
    fun getString(context: Context, @StringRes keyId: Int, defaultValue: String): String {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getString(getKey(context, keyId), defaultValue)!!
    }

    /**
     * Sets a string value to preferences.
     *
     * @param context the context
     * @param keyId   the key id
     */
    fun setString(context: Context, @StringRes keyId: Int, value: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        editor.putString(getKey(context, keyId), value)
        editor.apply()
    }

    fun getLong(context: Context, key: String): Long {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getLong(key, -1L)
    }

    fun setLong(context: Context, key: String, value: Long) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        editor.putLong(key, value)
        editor.apply()
    }
}
