package com.migux97.simon.tools

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.migux97.simon.data.SoundRule
import com.migux97.simon.data.StudioStore
import com.migux97.simon.data.VibePattern

/** Sonidos y vibraciones propios según la app o el contacto que avisa. */
object SoundPlayer {
    private var player: MediaPlayer? = null
    private var lastPlayAt = 0L

    /** La primera regla que coincide gana; las más específicas (con texto) se revisan primero. */
    fun match(pkg: String, title: String, text: String): SoundRule? {
        val s = StudioStore.soundSettings
        if (!s.enabled) return null
        return s.rules
            .filter { it.enabled }
            .sortedWith(compareByDescending<SoundRule> { it.contains.isNotBlank() }.thenByDescending { it.packageName.isNotBlank() })
            .firstOrNull { r ->
                (r.packageName.isBlank() || r.packageName == pkg) &&
                    (r.contains.isBlank() || title.contains(r.contains, true) || text.contains(r.contains, true))
            }
    }

    fun onNotification(context: Context, pkg: String, title: String, text: String) {
        val rule = match(pkg, title, text) ?: return
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val mode = am.ringerMode
        val silent = StudioStore.soundSettings.respectSilent && mode == AudioManager.RINGER_MODE_SILENT
        if (silent) return
        if (mode == AudioManager.RINGER_MODE_NORMAL || !StudioStore.soundSettings.respectSilent) {
            rule.soundUri?.let { play(context, it, rule.volume) }
        }
        vibrate(context, rule.vibration)
    }

    fun play(context: Context, uri: String, volume: Float) {
        val now = System.currentTimeMillis()
        if (now - lastPlayAt < 800) return
        lastPlayAt = now
        stop()
        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(context.applicationContext, Uri.parse(uri))
            val v = volume.coerceIn(0f, 1f)
            mp.setVolume(v, v)
            mp.setOnCompletionListener { it.release(); if (player === it) player = null }
            mp.setOnErrorListener { m, _, _ -> m.release(); if (player === m) player = null; true }
            mp.setOnPreparedListener { it.start() }
            mp.prepareAsync()
            player = mp
        } catch (_: Exception) {
        }
    }

    fun stop() {
        try { player?.release() } catch (_: Exception) {}
        player = null
    }

    fun vibrate(context: Context, pattern: VibePattern) {
        val wave = pattern.waveform ?: return
        val vib: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vib == null || !vib.hasVibrator()) return
        try {
            vib.vibrate(VibrationEffect.createWaveform(wave, -1))
        } catch (_: Exception) {
        }
    }
}
