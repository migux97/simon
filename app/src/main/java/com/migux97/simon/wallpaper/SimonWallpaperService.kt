package com.migux97.simon.wallpaper

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.migux97.simon.data.Catalog
import com.migux97.simon.data.LiveConfig
import com.migux97.simon.data.Repository
import com.migux97.simon.data.SceneType
import java.io.File

/** Servicio del fondo animado. Dibuja la escena elegida o reproduce un video. */
class SimonWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        Repository.init(this)
        return SimonEngine()
    }

    private inner class SimonEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var scene: Scene? = null
        private var config: LiveConfig? = null
        private var visible = false
        private var lastFrame = 0L
        private var width = 0
        private var height = 0
        private var player: MediaPlayer? = null

        private val frame = object : Runnable {
            override fun run() = drawFrame()
        }

        private fun currentConfig(): LiveConfig {
            // Fondo animado distinto de día y de noche, si está configurado.
            val r = com.migux97.simon.data.StudioStore.rotationSettings
            val scheduled = if (com.migux97.simon.tools.WallpaperRotator.isNight(r)) r.liveNight else r.liveDay
            scheduled?.let { id -> com.migux97.simon.data.Lookup.live(id)?.let { return it } }
            return Repository.live.value ?: Catalog.liveWallpapers.first()
        }

        private fun isVideo(c: LiveConfig) = c.scene == SceneType.VIDEO && c.videoPath != null && File(c.videoPath).exists()

        /** Relee la configuración por si se cambió desde la app. */
        private fun refreshConfig() {
            val c = currentConfig()
            if (c == config) return
            config = c
            stopVideo()
            if (isVideo(c)) {
                scene = null
                startVideo(c)
            } else {
                scene = Scene.create(c).also { if (width > 0) it.resize(width, height) }
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            width = w
            height = h
            scene?.resize(w, h)
            if (visible) schedule(0)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            config = null
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (isVisible) {
                refreshConfig()
                player?.start()
                lastFrame = SystemClock.uptimeMillis()
                schedule(0)
            } else {
                handler.removeCallbacks(frame)
                player?.takeIf { it.isPlaying }?.pause()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(frame)
            stopVideo()
            config = null
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            handler.removeCallbacks(frame)
            stopVideo()
            super.onDestroy()
        }

        private fun schedule(delay: Long) {
            handler.removeCallbacks(frame)
            if (visible && scene != null) handler.postDelayed(frame, delay)
        }

        private fun drawFrame() {
            val s = scene ?: return
            val now = SystemClock.uptimeMillis()
            val dt = ((now - lastFrame) / 1000f).coerceIn(0f, 0.1f)
            lastFrame = now
            s.step(dt)
            val holder = surfaceHolder
            val canvas = try {
                holder.lockHardwareCanvas()
            } catch (e: Exception) {
                null
            } ?: try {
                holder.lockCanvas()
            } catch (e: Exception) {
                null
            }
            if (canvas != null) {
                try {
                    s.render(canvas)
                } finally {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {
                    }
                }
            }
            val spent = SystemClock.uptimeMillis() - now
            val perf = com.migux97.simon.data.StudioStore.perf
            // En ahorro de batería el fondo queda quieto (se redibuja cada 2 s para el reloj).
            val frameMs = if (perf.batterySaverPause && com.migux97.simon.ui.components.isPowerSave(this@SimonWallpaperService)) 2000L
            else 1000L / perf.maxFps.coerceIn(10, 60)
            schedule((frameMs - spent).coerceAtLeast(4))
        }

        private fun startVideo(c: LiveConfig) {
            try {
                player = MediaPlayer().apply {
                    setSurface(surfaceHolder.surface)
                    setDataSource(c.videoPath)
                    isLooping = true
                    setVolume(0f, 0f)
                    setOnPreparedListener { if (visible) it.start() }
                    prepareAsync()
                }
            } catch (e: Exception) {
                player = null
                scene = Scene.create(c.copy(scene = SceneType.GRADIENT_FLOW)).also { if (width > 0) it.resize(width, height) }
            }
        }

        private fun stopVideo() {
            player?.let {
                try {
                    it.stop()
                } catch (_: Exception) {
                }
                it.release()
            }
            player = null
        }
    }
}
