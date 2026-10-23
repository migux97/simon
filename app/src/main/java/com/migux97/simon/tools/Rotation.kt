package com.migux97.simon.tools

import android.content.Context
import android.graphics.Bitmap
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.migux97.simon.data.ImageStore
import com.migux97.simon.data.Lookup
import com.migux97.simon.data.MediaLibrary
import com.migux97.simon.data.Repository
import com.migux97.simon.data.RotationEntry
import com.migux97.simon.data.RotationKind
import com.migux97.simon.data.RotationMode
import com.migux97.simon.data.StudioStore
import com.migux97.simon.data.WallpaperRotation
import com.migux97.simon.wallpaper.StaticWallpapers
import com.migux97.simon.wallpaper.WallpaperTarget
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** Cambia el fondo de pantalla solo: rotación por tiempo o fondo de día y de noche. */
object WallpaperRotator {

    fun isNight(r: WallpaperRotation, now: Calendar = Calendar.getInstance()): Boolean {
        val h = now.get(Calendar.HOUR_OF_DAY)
        return if (r.nightStartHour > r.dayStartHour) h >= r.nightStartHour || h < r.dayStartHour
        else h >= r.nightStartHour && h < r.dayStartHour
    }

    fun key(e: RotationEntry) = "${e.kind}:${e.ref}"

    /** Dibuja una entrada al tamaño de la pantalla. Corre fuera del hilo principal. */
    fun bitmapFor(context: Context, e: RotationEntry): Bitmap? {
        val (w, h) = StaticWallpapers.screenSize(context)
        return when (e.kind) {
            RotationKind.STATIC -> Lookup.static(e.ref)?.let { StaticWallpapers.render(it, w, h) }
            RotationKind.PHOTO -> ImageStore.load(e.ref, maxOf(w, h))
            RotationKind.DRAWING -> MediaLibrary.loadDrawing(e.ref, maxOf(w, h))
        }
    }

    private fun target(r: WallpaperRotation) = try { WallpaperTarget.valueOf(r.target) } catch (e: Exception) { WallpaperTarget.BOTH }

    private fun apply(context: Context, r: WallpaperRotation, e: RotationEntry): Boolean {
        val bmp = bitmapFor(context, e) ?: return false
        val ok = StaticWallpapers.apply(context, bmp, target(r))
        if (ok) StudioStore.updateRotation { it.copy(lastApplied = key(e)) }
        return ok
    }

    /** Pasa al siguiente fondo de la lista. */
    fun next(context: Context): Boolean {
        val r = StudioStore.rotationSettings
        if (r.entries.isEmpty()) return false
        val idx = if (r.shuffle && r.entries.size > 1) {
            var i: Int
            do { i = Random.nextInt(r.entries.size) } while (i == r.index)
            i
        } else (r.index + 1) % r.entries.size
        StudioStore.updateRotation { it.copy(index = idx) }
        return apply(context, r, r.entries[idx])
    }

    /** Revisión periódica: rota si pasó el intervalo o cambia entre día y noche. */
    fun tick(context: Context) {
        val r = StudioStore.rotationSettings
        when (r.mode) {
            RotationMode.OFF -> Unit
            RotationMode.INTERVAL -> next(context)
            RotationMode.DAY_NIGHT -> {
                val e = (if (isNight(r)) r.night else r.day) ?: return
                if (key(e) != r.lastApplied) apply(context, r, e)
            }
        }
    }

    fun schedule(context: Context) {
        val r = StudioStore.rotationSettings
        val wm = WorkManager.getInstance(context)
        if (r.mode == RotationMode.OFF) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val minutes = if (r.mode == RotationMode.DAY_NIGHT) 15L else r.intervalMin.toLong().coerceAtLeast(15)
        val req = PeriodicWorkRequestBuilder<RotationWorker>(minutes, TimeUnit.MINUTES).build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    private const val WORK = "simon_rotation"
}

class RotationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Repository.init(applicationContext)
        WallpaperRotator.tick(applicationContext)
        return Result.success()
    }
}
