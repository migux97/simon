package com.migux97.simon.tools

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.FileProvider
import com.migux97.simon.data.Repository
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Copias de Simón: guarda en un .simon (zip) toda la configuración, los widgets,
 * el launcher, las fotos, los dibujos y las animaciones, para pasarlos a otro
 * teléfono o compartir un tema completo con alguien.
 */
object BackupManager {
    private val PREFS = listOf("simon", "simon_studio")
    private val FOLDERS = listOf("images", "drawings", "anims", "videos")
    private const val META = "simon_backup.json"

    data class Result(val ok: Boolean, val message: String)

    fun suggestedName(): String =
        "simon_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) + ".simon"

    private fun prefsToJson(p: SharedPreferences): JSONObject {
        val out = JSONObject()
        p.all.forEach { (k, v) ->
            val o = JSONObject()
            when (v) {
                is String -> o.put("t", "s").put("v", v)
                is Int -> o.put("t", "i").put("v", v)
                is Long -> o.put("t", "l").put("v", v)
                is Float -> o.put("t", "f").put("v", v.toDouble())
                is Boolean -> o.put("t", "b").put("v", v)
                is Set<*> -> o.put("t", "ss").put("v", org.json.JSONArray(v.map { it.toString() }))
                else -> return@forEach
            }
            out.put(k, o)
        }
        return out
    }

    private fun jsonToPrefs(json: JSONObject, p: SharedPreferences, oldRoot: String, newRoot: String) {
        val e = p.edit().clear()
        json.keys().forEach { k ->
            val o = json.optJSONObject(k) ?: return@forEach
            when (o.optString("t")) {
                "s" -> e.putString(k, o.optString("v").replace(oldRoot, newRoot))
                "i" -> e.putInt(k, o.optInt("v"))
                "l" -> e.putLong(k, o.optLong("v"))
                "f" -> e.putFloat(k, o.optDouble("v").toFloat())
                "b" -> e.putBoolean(k, o.optBoolean("v"))
                "ss" -> {
                    val arr = o.optJSONArray("v")
                    val set = HashSet<String>()
                    if (arr != null) for (i in 0 until arr.length()) set += arr.optString(i)
                    e.putStringSet(k, set)
                }
            }
        }
        e.commit()
    }

    /** Escribe la copia en [out]. Corre fuera del hilo principal. */
    fun export(context: Context, out: OutputStream, includeVideos: Boolean): Result {
        return try {
            val root = context.filesDir
            ZipOutputStream(out.buffered()).use { zip ->
                val meta = JSONObject()
                    .put("version", 1)
                    .put("root", root.absolutePath)
                    .put("created", System.currentTimeMillis())
                PREFS.forEach { name ->
                    meta.put("prefs_$name", prefsToJson(context.getSharedPreferences(name, Context.MODE_PRIVATE)))
                }
                zip.putNextEntry(ZipEntry(META))
                zip.write(meta.toString().toByteArray())
                zip.closeEntry()
                FOLDERS.filter { includeVideos || it != "videos" }.forEach { folder ->
                    val dir = File(root, folder)
                    if (dir.exists()) dir.walkTopDown().filter { it.isFile }.forEach { f ->
                        zip.putNextEntry(ZipEntry("files/" + f.relativeTo(root).path.replace(File.separatorChar, '/')))
                        f.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
            Result(true, "Copia guardada")
        } catch (e: Exception) {
            Result(false, "No se pudo guardar la copia: ${e.message ?: "error"}")
        }
    }

    fun exportTo(context: Context, uri: Uri, includeVideos: Boolean): Result {
        val out = try { context.contentResolver.openOutputStream(uri) } catch (e: Exception) { null }
            ?: return Result(false, "No se pudo abrir el archivo")
        return out.use { export(context, it, includeVideos) }
    }

    /** Arma la copia en caché y devuelve un Intent para compartirla por cualquier app. */
    fun shareIntent(context: Context, includeVideos: Boolean): Intent? {
        val dir = File(context.cacheDir, "shared").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val file = File(dir, suggestedName())
        val r = FileOutputStream(file).use { export(context, it, includeVideos) }
        if (!r.ok) return null
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/zip")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, "Mi tema de Simón")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, "Compartir mi Simón").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Restaura una copia. Reemplaza la configuración actual. Corre fuera del hilo principal. */
    fun restore(context: Context, uri: Uri): Result {
        val root = context.filesDir
        val canonicalRoot = root.canonicalPath
        var meta: JSONObject? = null
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return Result(false, "No se pudo abrir el archivo")
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    when {
                        entry.isDirectory -> Unit
                        name == META -> meta = JSONObject(zip.readBytes().decodeToString())
                        name.startsWith("files/") -> {
                            val target = File(root, name.removePrefix("files/"))
                            // Evita rutas que intenten escribir fuera de la carpeta de Simón.
                            if (!target.canonicalPath.startsWith(canonicalRoot + File.separator)) continue
                            target.parentFile?.mkdirs()
                            FileOutputStream(target).use { zip.copyTo(it) }
                        }
                    }
                    zip.closeEntry()
                }
            }
            val m = meta ?: return Result(false, "Ese archivo no es una copia de Simón")
            val oldRoot = m.optString("root", root.absolutePath)
            PREFS.forEach { name ->
                m.optJSONObject("prefs_$name")?.let {
                    jsonToPrefs(it, context.getSharedPreferences(name, Context.MODE_PRIVATE), oldRoot, root.absolutePath)
                }
            }
            Repository.reload(context)
            RoutineAlarms.scheduleAll(context)
            WallpaperRotator.schedule(context)
            com.migux97.simon.widget.WidgetUpdater.updateAll(context)
            Result(true, "Copia restaurada")
        } catch (e: Exception) {
            Result(false, "No se pudo leer la copia: ${e.message ?: "error"}")
        }
    }
}
