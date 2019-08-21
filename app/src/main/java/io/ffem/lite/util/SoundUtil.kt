package io.ffem.lite.util

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.annotation.RawRes
import io.ffem.lite.app.App.Companion.RESULT_SOUND_PLAYED_KEY
import io.ffem.lite.app.App.Companion.SOUND_ON
import io.ffem.lite.preference.isSoundOn

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
        if (isSoundOn()) {
            val lastPlayed = PreferencesUtil.getLong(context, RESULT_SOUND_PLAYED_KEY)
            if (System.currentTimeMillis() - lastPlayed < 5000) {
                return
            }
            PreferencesUtil.setLong(context, RESULT_SOUND_PLAYED_KEY, System.currentTimeMillis())

            val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
            val originalVolume: Int
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            @Suppress("ConstantConditionIf")
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (SOUND_ON)
                    (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 1.5).toInt() else 1,
                0
            )
            val mp = MediaPlayer.create(context, resourceId)
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.start()
            mp.setOnCompletionListener {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
                mp.release()
            }
        }
    }
}
