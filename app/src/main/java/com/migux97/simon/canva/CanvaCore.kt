package com.migux97.simon.canva

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.migux97.simon.data.PhotoAdjust
import com.migux97.simon.data.newId
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// ======================================================================
//  Editor tipo Canva: lo común a widgets y páginas. Cada editor guarda su
//  documento a su manera; acá se lo ve como una lista de capas.
// ======================================================================

/** Una capa del lienzo. Posición y tamaño en fracciones del lienzo (0..1), giro en grados. */
data class CLayer(
    val id: String,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val rotation: Float = 0f,
    val alpha: Float = 1f,
    val locked: Boolean = false,
    val hidden: Boolean = false,
    val groupId: String? = null,
    val label: String = "",
    val canDuplicate: Boolean = true,
    val flipX: Boolean = false,
    val flipY: Boolean = false,
    /** Solo fotos y dibujos: recorte y ajustes. Null si no se puede recortar. */
    val photo: PhotoAdjust? = null
)

/** Rectángulo en fracciones del lienzo. */
data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
    val cx get() = (left + right) / 2f
    val cy get() = (top + bottom) / 2f
}

/** Línea guía que aparece al arrastrar: vertical (en x) u horizontal (en y). */
data class Guide(val vertical: Boolean, val pos: Float)

/** Caja que ocupa la capa ya girada. Se calcula en píxeles porque el lienzo no es cuadrado. */
fun CLayer.bounds(wPx: Float, hPx: Float): Bounds {
    if (rotation % 360f == 0f) return Bounds(x, y, x + w, y + h)
    val a = Math.toRadians(rotation.toDouble())
    val hw = w * wPx / 2f
    val hh = h * hPx / 2f
    val ex = (abs(hw * cos(a)) + abs(hh * sin(a))).toFloat()
    val ey = (abs(hw * sin(a)) + abs(hh * cos(a))).toFloat()
    val cx = (x + w / 2f) * wPx
    val cy = (y + h / 2f) * hPx
    return Bounds((cx - ex) / wPx, (cy - ey) / hPx, (cx + ex) / wPx, (cy + ey) / hPx)
}

fun union(list: List<Bounds>): Bounds? = if (list.isEmpty()) null else Bounds(
    list.minOf { it.left }, list.minOf { it.top }, list.maxOf { it.right }, list.maxOf { it.bottom }
)

/**
 * Imán al mover: busca bordes y centros (del lienzo y de las otras capas)
 * cerca de los de [box]. Devuelve cuánto correrlo y qué guías mostrar.
 */
fun snapMove(box: Bounds, others: List<Bounds>, wPx: Float, hPx: Float, thresholdPx: Float): Triple<Float, Float, List<Guide>> {
    val xs = listOf(0f, 0.5f, 1f) + others.flatMap { listOf(it.left, it.cx, it.right) }
    val ys = listOf(0f, 0.5f, 1f) + others.flatMap { listOf(it.top, it.cy, it.bottom) }
    fun best(targets: List<Float>, edges: List<Float>, sizePx: Float): Pair<Float, Float?> {
        var bestD = Float.MAX_VALUE
        var guide: Float? = null
        for (e in edges) for (t in targets) {
            val d = t - e
            if (abs(d) * sizePx <= thresholdPx && abs(d) < abs(bestD)) { bestD = d; guide = t }
        }
        return if (guide == null) 0f to null else bestD to guide
    }
    val (dx, gx) = best(xs, listOf(box.left, box.cx, box.right), wPx)
    val (dy, gy) = best(ys, listOf(box.top, box.cy, box.bottom), hPx)
    return Triple(dx, dy, listOfNotNull(gx?.let { Guide(true, it) }, gy?.let { Guide(false, it) }))
}

/** Cómo guarda cada editor sus capas. Todo es inmutable: cada cambio devuelve un documento nuevo. */
interface CanvaSource<S> {
    /** De atrás hacia adelante. */
    fun layers(doc: S): List<CLayer>
    /** Copia posición, tamaño, giro, transparencia, bloqueo, oculto y grupo de las capas dadas. */
    fun applyLayers(doc: S, changed: List<CLayer>): S
    /** Nuevo orden, de atrás hacia adelante. */
    fun reorder(doc: S, idsBackToFront: List<String>): S
    fun remove(doc: S, ids: Set<String>): S
    /** Devuelve el documento con las copias y los ids nuevos (en el mismo orden). */
    fun duplicate(doc: S, ids: List<String>): Pair<S, List<String>>
}

enum class AlignMode { LEFT, CENTER_H, RIGHT, TOP, CENTER_V, BOTTOM }

/**
 * Estado del editor: el documento, la selección, el historial (deshacer y
 * rehacer) y qué panel está abierto. [onCommit] guarda cada cambio terminado.
 */
class CanvaState<S>(initial: S, val source: CanvaSource<S>, private val onCommit: (S) -> Unit) {
    var doc by mutableStateOf(initial)
        private set
    var selection by mutableStateOf<Set<String>>(emptySet())
        private set
    /** Tocar suma o quita de la selección en vez de reemplazarla. */
    var multiSelect by mutableStateOf(false)
    var guides by mutableStateOf<List<Guide>>(emptyList())
    var panel by mutableStateOf<String?>(null)
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    private val past = ArrayDeque<S>()
    private val future = ArrayDeque<S>()
    private var lastPush = 0L
    private var gestureStart: S? = null
    val inGesture get() = gestureStart != null
    /** Igual que [inGesture] pero observable (para esconder el menú mientras se arrastra). */
    var inGestureState by mutableStateOf(false)
        private set

    val layers: List<CLayer> get() = source.layers(doc)
    val selected: List<CLayer> get() = layers.filter { it.id in selection }

    private fun flags() {
        canUndo = past.isNotEmpty()
        canRedo = future.isNotEmpty()
    }

    private fun push(s: S) {
        past.addLast(s)
        while (past.size > 50) past.removeFirst()
        future.clear()
        flags()
    }

    private fun pruneSelection() {
        val ids = layers.map { it.id }.toSet()
        if (!ids.containsAll(selection)) selection = selection.filter { it in ids }.toSet()
    }

    /**
     * Aplica un cambio y lo guarda. Con [coalesce] los cambios seguidos (un
     * deslizador, escribir) se juntan en un solo paso de «Deshacer».
     */
    fun commit(next: S, coalesce: Boolean = false) {
        if (next == doc) return
        val now = SystemClock.uptimeMillis()
        if (!coalesce || now - lastPush > 800L) push(doc)
        lastPush = now
        doc = next
        onCommit(next)
        pruneSelection()
    }

    fun update(coalesce: Boolean = false, transform: (S) -> S) = commit(transform(doc), coalesce)
    fun updateLayers(changed: List<CLayer>, coalesce: Boolean = false) = commit(source.applyLayers(doc, changed), coalesce)

    // ---- Gestos: mientras se arrastra no se guarda; al soltar queda un solo paso.
    fun beginGesture() { if (gestureStart == null) { gestureStart = doc; inGestureState = true } }
    fun gesture(next: S) { doc = next }
    fun endGesture() {
        val start = gestureStart ?: return
        gestureStart = null
        inGestureState = false
        guides = emptyList()
        if (start != doc) {
            push(start)
            lastPush = 0L
            onCommit(doc)
        }
    }

    fun undo() {
        val prev = past.removeLastOrNull() ?: return
        future.addLast(doc)
        doc = prev
        onCommit(prev)
        pruneSelection()
        flags()
    }

    fun redo() {
        val next = future.removeLastOrNull() ?: return
        past.addLast(doc)
        doc = next
        onCommit(next)
        pruneSelection()
        flags()
    }

    /** Cambios que llegan de afuera (otra pantalla, la base de datos): no entran al historial. */
    fun syncExternal(next: S) {
        if (gestureStart != null || next == doc) return
        doc = next
        pruneSelection()
    }

    /** Anota el estado actual antes de un cambio hecho por fuera del editor, para poder deshacerlo. */
    fun checkpoint() { push(doc); lastPush = 0L }

    // ---- Selección (los grupos se eligen enteros)

    fun groupOf(id: String): Set<String> {
        val all = layers
        val l = all.firstOrNull { it.id == id } ?: return emptySet()
        val g = l.groupId ?: return setOf(id)
        return all.filter { it.groupId == g }.map { it.id }.toSet()
    }

    fun tap(id: String) {
        val ids = groupOf(id)
        selection = when {
            multiSelect -> if (selection.containsAll(ids)) selection - ids else selection + ids
            else -> ids
        }
    }

    fun toggle(id: String) {
        val ids = groupOf(id)
        selection = if (selection.containsAll(ids)) selection - ids else selection + ids
    }

    fun selectOnly(id: String) { selection = groupOf(id) }
    fun select(ids: Collection<String>) { selection = ids.toSet() }
    fun clearSelection() { selection = emptySet(); multiSelect = false }

    // ---- Acciones sobre la selección

    fun deleteSelected() {
        val ids = selection
        if (ids.isEmpty()) return
        commit(source.remove(doc, ids))
        selection = emptySet()
    }

    fun duplicateSelected() {
        val ids = layers.filter { it.id in selection && it.canDuplicate }.map { it.id }
        if (ids.isEmpty()) return
        val (next, newIds) = source.duplicate(doc, ids)
        // Las copias de un grupo forman un grupo nuevo.
        val groups = layers.filter { it.id in ids }.mapNotNull { it.groupId }.distinct().associateWith { newId() }
        val copies = source.layers(next).filter { it.id in newIds }.map { c -> c.copy(groupId = c.groupId?.let { groups[it] }) }
        commit(source.applyLayers(next, copies))
        selection = newIds.toSet()
    }

    fun setLocked(locked: Boolean) = updateLayers(selected.map { it.copy(locked = locked) })

    fun toggleHidden(id: String) {
        val l = layers.firstOrNull { it.id == id } ?: return
        updateLayers(listOf(l.copy(hidden = !l.hidden)))
        if (!l.hidden) selection = selection - id
    }

    fun toggleLockOf(id: String) {
        val l = layers.firstOrNull { it.id == id } ?: return
        updateLayers(listOf(l.copy(locked = !l.locked)))
    }

    val canGroup get() = selection.size >= 2 && selected.map { it.groupId }.distinct().let { it.size > 1 || it.firstOrNull() == null }
    val canUngroup get() = selected.any { it.groupId != null }

    fun group() {
        val g = newId()
        updateLayers(selected.map { it.copy(groupId = g) })
    }

    fun ungroup() = updateLayers(selected.map { it.copy(groupId = null) })

    fun setAlpha(alpha: Float) = updateLayers(selected.map { it.copy(alpha = alpha) }, coalesce = true)

    fun resetRotation() = updateLayers(selected.filterNot { it.locked }.map { it.copy(rotation = 0f) })

    /** Espeja lo elegido. Con varias capas, también se espejan sus posiciones dentro del conjunto. */
    fun flip(horizontal: Boolean, wPx: Float, hPx: Float) {
        val sel = selected.filterNot { it.locked }
        if (sel.isEmpty()) return
        val box = union(sel.map { it.bounds(wPx, hPx) }) ?: return
        updateLayers(sel.map { l ->
            if (horizontal) l.copy(flipX = !l.flipX, rotation = -l.rotation, x = if (sel.size > 1) box.left + box.right - l.x - l.w else l.x)
            else l.copy(flipY = !l.flipY, rotation = -l.rotation, y = if (sel.size > 1) box.top + box.bottom - l.y - l.h else l.y)
        })
    }

    /** Cambia el recorte o los ajustes de una foto. Si cambia la forma del recorte, la capa toma esa forma. */
    fun setPhoto(id: String, adjust: PhotoAdjust, newAspect: Float? = null, wPx: Float = 1f, hPx: Float = 1f, coalesce: Boolean = false) {
        val l = layers.firstOrNull { it.id == id } ?: return
        var next = l.copy(photo = adjust)
        if (newAspect != null && newAspect > 0f) {
            // Se mantiene el ancho y el centro; el alto sale de la forma del recorte.
            val cy = l.y + l.h / 2f
            val h = (l.w * wPx / newAspect) / hPx
            next = next.copy(h = h, y = cy - h / 2f)
        }
        updateLayers(listOf(next), coalesce)
    }

    // ---- Orden (de atrás hacia adelante)

    private fun reorderSelected(move: (MutableList<String>, Set<String>) -> Unit) {
        val order = layers.map { it.id }.toMutableList()
        move(order, selection)
        if (order != layers.map { it.id }) commit(source.reorder(doc, order))
    }

    fun bringToFront() = reorderSelected { o, sel -> val moved = o.filter { it in sel }; o.removeAll(sel); o.addAll(moved) }
    fun sendToBack() = reorderSelected { o, sel -> val moved = o.filter { it in sel }; o.removeAll(sel); o.addAll(0, moved) }

    fun bringForward() = reorderSelected { o, sel ->
        for (i in o.size - 2 downTo 0) if (o[i] in sel && o[i + 1] !in sel) { val t = o[i]; o[i] = o[i + 1]; o[i + 1] = t }
    }

    fun sendBackward() = reorderSelected { o, sel ->
        for (i in 1 until o.size) if (o[i] in sel && o[i - 1] !in sel) { val t = o[i]; o[i] = o[i - 1]; o[i - 1] = t }
    }

    fun moveLayer(id: String, delta: Int) {
        val order = layers.map { it.id }.toMutableList()
        val i = order.indexOf(id)
        val j = i + delta
        if (i < 0 || j !in order.indices) return
        order.removeAt(i)
        order.add(j, id)
        commit(source.reorder(doc, order))
    }

    // ---- Alinear y distribuir

    /** Con una capa se alinea al lienzo; con varias, entre ellas. */
    fun align(mode: AlignMode, wPx: Float, hPx: Float) {
        val sel = selected.filterNot { it.locked }
        if (sel.isEmpty()) return
        val target = if (selection.size == 1) Bounds(0f, 0f, 1f, 1f) else union(selected.map { it.bounds(wPx, hPx) }) ?: return
        // Un grupo se alinea como un solo bloque.
        val blocks = sel.groupBy { it.groupId ?: it.id }.values.map { members -> members to union(members.map { it.bounds(wPx, hPx) })!! }
        val moved = blocks.flatMap { (members, b) ->
            val dx = when (mode) {
                AlignMode.LEFT -> target.left - b.left
                AlignMode.CENTER_H -> target.cx - b.cx
                AlignMode.RIGHT -> target.right - b.right
                else -> 0f
            }
            val dy = when (mode) {
                AlignMode.TOP -> target.top - b.top
                AlignMode.CENTER_V -> target.cy - b.cy
                AlignMode.BOTTOM -> target.bottom - b.bottom
                else -> 0f
            }
            members.map { it.copy(x = it.x + dx, y = it.y + dy) }
        }
        updateLayers(moved)
    }

    /** Deja el mismo espacio entre tres o más capas (o grupos). */
    fun distribute(horizontal: Boolean, wPx: Float, hPx: Float) {
        val blocks = selected.filterNot { it.locked }.groupBy { it.groupId ?: it.id }.values
            .map { members -> members to union(members.map { it.bounds(wPx, hPx) })!! }
            .sortedBy { (_, b) -> if (horizontal) b.left else b.top }
        if (blocks.size < 3) return
        val first = blocks.first().second
        val last = blocks.last().second
        val total = if (horizontal) last.right - first.left else last.bottom - first.top
        val used = blocks.sumOf { (_, b) -> (if (horizontal) b.width else b.height).toDouble() }.toFloat()
        val gap = (total - used) / (blocks.size - 1)
        var cursor = if (horizontal) first.left else first.top
        val moved = blocks.flatMap { (members, b) ->
            val d = cursor - if (horizontal) b.left else b.top
            cursor += (if (horizontal) b.width else b.height) + gap
            members.map { if (horizontal) it.copy(x = it.x + d) else it.copy(y = it.y + d) }
        }
        updateLayers(moved)
    }
}
