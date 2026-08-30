package com.novatube.app.player.music

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.novatube.app.NovaTubeApp
import okhttp3.OkHttpClient

/**
 * Background music playback using Media3 MediaSessionService so the system can render
 * lock-screen / notification controls and continue playback when the activity is gone.
 */
class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        val app = applicationContext as NovaTubeApp
        val ok = OkHttpClient.Builder().build()
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            setHandleAudioBecomingNoisy(true)
        }
        mediaSession = MediaSession.Builder(this, player)
            .setId("novatube_music")
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
