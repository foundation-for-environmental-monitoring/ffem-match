package io.ffem.lite.util

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.annotation.RawRes
import io.ffem.lite.BuildConfig
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.isDiagnosticMode

/**
 * Sound utils.
 */
object SoundUtil {
    /**
     * Play a short sound effect.
     *
     * @param resourceId the
     */
    fun playShortResource(context: Context, @RawRes resourceId: Int) {

        //play sound if the sound is not turned off in the preference
        try {
            if (AppPreferences.isSoundOn(context)) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val originalVolume: Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    if (BuildConfig.USE_SCREEN_PINNING.get() && !isDiagnosticMode())
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) else 5,
                    0
                )
                val mp = MediaPlayer.create(context, resourceId)
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
                mp.start()
                mp.setOnCompletionListener {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
                    mp.release()
                }
            }
        } catch (e: Exception) {
        }
    }
}