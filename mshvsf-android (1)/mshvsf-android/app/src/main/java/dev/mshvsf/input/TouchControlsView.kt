package dev.mshvsf.input

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import dev.mshvsf.Settings
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

enum class TouchCmd { MENU, REWIND_ON, REWIND_OFF, TURBO, QUICK_SAVE, QUICK_LOAD, FLIP_SIDE }

/**
 * Overlay multitouch:
 *  - direcional virtual 8 direções (flutuante: nasce onde o polegar toca e acompanha o arrasto)
 *  - 6 botões em grade 3x2; arrastar o dedo entre dois botões pressiona os dois (chord com 1 dedo)
 *  - COIN / START, macros e atalhos (menu, rewind, turbo, quick save/load)
 */
class TouchControlsView(
    context: Context,
    private val settings: Settings,
    private val router: InputRouter,
    private val macros: MacroRunner,
    private val onCmd: (TouchCmd) -> Unit,
) : View(context) {

    private enum class Role { STICK, BUTTONS, ONE_SHOT, REWIND }
    private class Pointer(var role: Role, var x: Float, var y: Float)
    private class Circle(val btn: Btn, val cx: Float, val cy: Float)
    private class Pill(val label: String, val rect: RectF, val onTap: (() -> Unit)?, val btn: Btn? = null, val hold: Boolean = false)

    private val pointers = HashMap<Int, Pointer>()
    private var stickPointer = -1

    // geometria
    private var btnR = 0f
    private var hitR = 0f
    private var stickR = 0f
    private var stickHomeX = 0f; private var stickHomeY = 0f
    private var stickOx = 0f; private var stickOy = 0f
    private var stickX = 0f; private var stickY = 0f
    private var zoneLeft = 0f; private var zoneRight = 0f; private var zoneTop = 0f
    private var circles = listOf<Circle>()
    private var pills = listOf<Pill>()
    private var rewindPill: Pill? = null

    private var held: Set<Btn> = emptySet()
    private var stickDir: Set<Btn> = emptySet()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; color = Color.WHITE }

    fun relayout() { if (width > 0) doLayout(width.toFloat(), height.toFloat()); invalidate() }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        doLayout(w.toFloat(), h.toFloat())
    }

    private fun doLayout(w: Float, h: Float) {
        val s = settings.touchScale
        val mirror = settings.mirrored
        val u = min(h / 6.2f, w / 11f) * s
        btnR = u * 0.95f
        hitR = btnR * 1.2f
        val gap = btnR * 2.15f
        val margin = u * 0.5f
        stickR = u * 2.0f

        // botões: lado direito (esquerdo se espelhado)
        val clusterRight = if (!mirror) w - margin - btnR else margin + btnR + gap * 2
        val rowBottom = h - margin - btnR
        val rowTop = rowBottom - gap * 0.98f
        val cols = listOf(clusterRight - gap * 2, clusterRight - gap, clusterRight)
        circles = listOf(
            Circle(Btn.LP, cols[0], rowTop), Circle(Btn.MP, cols[1], rowTop), Circle(Btn.HP, cols[2], rowTop),
            Circle(Btn.LK, cols[0], rowBottom), Circle(Btn.MK, cols[1], rowBottom), Circle(Btn.HK, cols[2], rowBottom),
        )

        // direcional
        stickHomeX = if (!mirror) margin + stickR else w - margin - stickR
        stickHomeY = h - margin - stickR
        stickOx = stickHomeX; stickOy = stickHomeY; stickX = stickOx; stickY = stickOy
        zoneLeft = if (!mirror) 0f else w * 0.58f
        zoneRight = if (!mirror) w * 0.42f else w
        zoneTop = h * 0.30f

        // pílulas
        val ph = u * 0.62f
        val pw = u * 1.55f
        val list = ArrayList<Pill>()
        fun pill(cx: Float, cy: Float, label: String, wMul: Float = 1f, tap: (() -> Unit)? = null, btn: Btn? = null, hold: Boolean = false): Pill {
            val r = RectF(cx - pw * wMul / 2, cy - ph / 2, cx + pw * wMul / 2, cy + ph / 2)
            return Pill(label, r, tap, btn, hold).also { list.add(it) }
        }
        val midX = w / 2
        // COIN / START ao centro embaixo
        pill(midX - pw * 0.6f, h - margin - ph / 2, "COIN", btn = Btn.COIN)
        pill(midX + pw * 0.6f, h - margin - ph / 2, "START", btn = Btn.START)
        // atalhos no topo
        val topY = margin * 0.7f + ph / 2
        val shortcuts = listOf<Triple<String, TouchCmd?, Boolean>>(
            Triple("MENU", TouchCmd.MENU, false), Triple("REW", null, true),
            Triple("FF", TouchCmd.TURBO, false), Triple("SAVE", TouchCmd.QUICK_SAVE, false),
            Triple("LOAD", TouchCmd.QUICK_LOAD, false),
        )
        val totalW = shortcuts.size * pw * 0.95f
        shortcuts.forEachIndexed { i, (label, cmd, hold) ->
            val cx = midX - totalW / 2 + pw * 0.95f * (i + 0.5f)
            val p = pill(cx, topY, label, 0.88f, tap = cmd?.let { c -> { onCmd(c) } }, hold = hold)
            if (hold) rewindPill = p
        }

        // macros: faixa acima do conjunto de botões
        if (settings.showMacros) {
            val my = rowTop - gap * 0.98f
            val defs = MacroRunner.ALL
            defs.forEachIndexed { i, d ->
                val col = i % 3; val row = i / 3
                val cx = cols[col]
                val cy = my - row * (ph * 1.2f)
                pill(cx, cy, d.label, 0.9f, tap = { macros.run(d) })
            }
            // alterna o lado (direção "frente" das macros)
            pill(cols[2], my - ((defs.size + 2) / 3) * (ph * 1.2f) , sideLabel(), 0.9f, tap = {
                macros.facingRight = !macros.facingRight
                onCmd(TouchCmd.FLIP_SIDE)
                relayout()
            })
        }
        pills = list
    }

    private fun sideLabel() = if (macros.facingRight) "P1 →" else "← P1"

    // ---------- toque ----------
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                down(e.getPointerId(i), e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> up(e.getPointerId(e.actionIndex))
            MotionEvent.ACTION_CANCEL -> { pointers.keys.toList().forEach { up(it) } }
        }
        for (i in 0 until e.pointerCount) {
            pointers[e.getPointerId(i)]?.let { it.x = e.getX(i); it.y = e.getY(i) }
        }
        recompute()
        invalidate()
        return true
    }

    private fun down(id: Int, x: Float, y: Float) {
        // 1) botões de jogo (círculos e COIN/START)
        if (circles.any { inCircle(it, x, y) } || pills.any { it.btn != null && it.rect.contains(x, y) }) {
            pointers[id] = Pointer(Role.BUTTONS, x, y); return
        }
        // 2) atalhos e macros
        pills.firstOrNull { it.btn == null && it.rect.contains(x, y) }?.let { p ->
            if (p.hold) {
                pointers[id] = Pointer(Role.REWIND, x, y); onCmd(TouchCmd.REWIND_ON)
            } else {
                pointers[id] = Pointer(Role.ONE_SHOT, x, y); haptic(); p.onTap?.invoke()
            }
            return
        }
        // 3) direcional
        if (stickPointer == -1 && x in zoneLeft..zoneRight && y >= zoneTop) {
            stickPointer = id
            pointers[id] = Pointer(Role.STICK, x, y)
            if (settings.floatingStick) { stickOx = x; stickOy = y } else { stickOx = stickHomeX; stickOy = stickHomeY }
            return
        }
        pointers[id] = Pointer(Role.BUTTONS, x, y)
    }

    private fun up(id: Int) {
        val p = pointers.remove(id) ?: return
        when (p.role) {
            Role.REWIND -> onCmd(TouchCmd.REWIND_OFF)
            Role.STICK -> { stickPointer = -1; stickX = stickHomeX; stickY = stickHomeY; stickOx = stickHomeX; stickOy = stickHomeY }
            else -> {}
        }
    }

    private fun inCircle(c: Circle, x: Float, y: Float) = hypot(x - c.cx, y - c.cy) <= hitR

    private fun recompute() {
        val want = HashSet<Btn>()
        stickDir = emptySet()
        for ((id, p) in pointers) {
            when (p.role) {
                Role.BUTTONS -> {
                    for (c in circles) if (inCircle(c, p.x, p.y)) want += c.btn
                    for (pl in pills) if (pl.btn != null && pl.rect.contains(p.x, p.y)) want += pl.btn
                }
                Role.STICK -> if (id == stickPointer) {
                    stickDir = direction(p.x, p.y); want += stickDir
                }
                else -> {}
            }
        }
        if (want != held) {
            if ((want - held).isNotEmpty()) haptic()
            held = want
            router.setSource("touch", want)
        }
    }

    private fun direction(x: Float, y: Float): Set<Btn> {
        var dx = x - stickOx; var dy = y - stickOy
        val dist = hypot(dx, dy)
        // origem "puxada" pelo dedo: não precisa recentralizar para inverter a direção
        val limit = stickR * 1.3f
        if (settings.floatingStick && dist > limit) {
            val k = (dist - limit) / dist
            stickOx += dx * k; stickOy += dy * k
            dx = x - stickOx; dy = y - stickOy
        }
        val d = hypot(dx, dy)
        val kn = if (d > stickR) stickR / d else 1f
        stickX = stickOx + dx * kn; stickY = stickOy + dy * kn
        if (d < stickR * 0.28f) return emptySet()
        val deg = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360 + 22.5) % 360
        return when ((deg / 45).toInt()) {
            0 -> setOf(Btn.RIGHT)
            1 -> setOf(Btn.RIGHT, Btn.DOWN)
            2 -> setOf(Btn.DOWN)
            3 -> setOf(Btn.LEFT, Btn.DOWN)
            4 -> setOf(Btn.LEFT)
            5 -> setOf(Btn.LEFT, Btn.UP)
            6 -> setOf(Btn.UP)
            else -> setOf(Btn.RIGHT, Btn.UP)
        }
    }

    private fun haptic() {
        if (settings.haptics) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    // ---------- desenho ----------
    override fun onDraw(c: Canvas) {
        val op = settings.touchOpacity
        fun a(f: Float) = (255 * op * f).toInt().coerceIn(0, 255)

        // direcional
        val active = stickPointer != -1
        val bx = if (active) stickOx else stickHomeX
        val by = if (active) stickOy else stickHomeY
        fill.color = Color.WHITE; fill.alpha = a(0.12f)
        c.drawCircle(bx, by, stickR, fill)
        stroke.color = Color.WHITE; stroke.alpha = a(0.5f); stroke.strokeWidth = btnR * 0.08f
        c.drawCircle(bx, by, stickR, stroke)
        fill.color = Color.WHITE; fill.alpha = a(if (active) 0.55f else 0.3f)
        c.drawCircle(if (active) stickX else stickHomeX, if (active) stickY else stickHomeY, stickR * 0.42f, fill)

        // botões
        text.textSize = btnR * 0.62f
        for (ci in circles) {
            val on = ci.btn in held
            val punch = ci.btn == Btn.LP || ci.btn == Btn.MP || ci.btn == Btn.HP
            fill.color = if (punch) Color.rgb(255, 120, 80) else Color.rgb(90, 170, 255)
            fill.alpha = a(if (on) 0.95f else 0.28f)
            c.drawCircle(ci.cx, ci.cy, btnR, fill)
            stroke.color = fill.color; stroke.alpha = a(0.8f)
            c.drawCircle(ci.cx, ci.cy, btnR, stroke)
            text.alpha = a(1f)
            c.drawText(ci.btn.label, ci.cx, ci.cy + text.textSize * 0.35f, text)
        }

        // pílulas
        text.textSize = btnR * 0.5f
        val pressedPills = pointers.values.filter { it.role == Role.REWIND }.size
        for (p in pills) {
            val on = (p.btn != null && p.btn in held) || (p.hold && pressedPills > 0)
            fill.color = Color.WHITE; fill.alpha = a(if (on) 0.7f else 0.16f)
            c.drawRoundRect(p.rect, p.rect.height() / 2, p.rect.height() / 2, fill)
            text.alpha = a(1f)
            c.drawText(p.label, p.rect.centerX(), p.rect.centerY() + text.textSize * 0.35f, text)
        }
    }
}
