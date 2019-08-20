package io.ffem.lite.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import androidx.annotation.RawRes;

import static android.content.Context.AUDIO_SERVICE;
import static io.ffem.lite.app.App.SOUND_ON;
import static io.ffem.lite.preference.AppPreferencesKt.isSoundOn;

/**
 * Sound utils.
 */
public class SoundUtil {
    /**
     * Play a short sound effect.
     *
     * @param resourceId the
     */
    public static void playShortResource(Context context, @RawRes int resourceId) {

        //play sound if the sound is not turned off in the preference
        if (isSoundOn()) {
            final AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
            final int originalVolume;
            if (audioManager != null) {
                originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        SOUND_ON ? (int) (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 1.5) : 1,
                        0);
                MediaPlayer mp = MediaPlayer.create(context, resourceId);
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mp.start();
                mp.setOnCompletionListener(mediaPlayer -> {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
                    mp.release();
                });
            }
        }
    }
}
