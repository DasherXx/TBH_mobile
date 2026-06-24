package com.tbh.mobile.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.tbh.core.GameState
import com.tbh.core.HeroClass
import kotlin.math.abs

class OverlayView(
    context: Context,
    private val windowManager: WindowManager,
    private val params: WindowManager.LayoutParams
) : View(context) {

    var state: GameState = GameState.initial()
        set(value) { field = value; invalidate() }

    // --- Paints ---
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8152040")
    }
    private val barBgPaint = Paint().apply { color = Color.parseColor("#55FFFFFF") }
    private val barFgPaint = Paint()
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val deadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#60000000")
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCFFFFFF")
        textAlign = Paint.Align.CENTER
    }
    private val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        textAlign = Paint.Align.CENTER
    }
    private val levelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val toastBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DD000000")
    }
    private val toastTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val bgRect = RectF()
    private val toastRect = RectF()

    // --- Toast queue ---
    private val toasts = mutableListOf<Pair<String, Long>>()

    fun showToast(message: String) {
        val expiry = System.currentTimeMillis() + TOAST_DURATION_MS
        toasts.add(Pair(message, expiry))
        if (toasts.size > MAX_TOASTS) toasts.removeAt(0)
        invalidate()
    }

    // --- Drag ---
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var dragging = false

    init { setWillNotDraw(false) }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val d = resources.displayMetrics.density
        val now = System.currentTimeMillis()

        bgRect.set(0f, 0f, w, h)
        canvas.drawRoundRect(bgRect, 14f, 14f, bgPaint)

        // Header — fala i strefa
        titlePaint.textSize = 10f * d
        canvas.drawText("⚔  Wave ${state.wave}  |  Zone ${state.zone}", w * 0.50f, h * 0.18f, titlePaint)

        // Złoto
        goldPaint.textSize = 9f * d
        canvas.drawText("🪙 ${state.gold}", w * 0.50f, h * 0.28f, goldPaint)

        val heroR    = h * 0.17f
        val monsterR = h * 0.24f
        val heroY    = h * 0.67f
        val monsterY = h * 0.61f
        val heroXs   = floatArrayOf(w * 0.14f, w * 0.29f, w * 0.44f)
        val monsterX = w * 0.79f

        namePaint.textSize = 9f * d

        // VS separator
        titlePaint.textSize = 10f * d
        canvas.drawText("VS", w * 0.615f, monsterY + 5f * d, titlePaint)

        // Heroes
        state.heroes.forEachIndexed { i, hero ->
            val cx = heroXs[i]
            val barW = heroR * 2.5f
            val barH = 5f * d
            drawHpBar(canvas, cx - barW / 2f, heroY - heroR - barH - 4f * d, barW, barH, hero.hp, hero.maxHp)

            if (hero.hp > 0) {
                circlePaint.color = hero.heroClass.toColor()
                canvas.drawCircle(cx, heroY, heroR, circlePaint)
                namePaint.color = Color.parseColor("#CCFFFFFF")
            } else {
                canvas.drawCircle(cx, heroY, heroR, deadPaint)
                namePaint.color = Color.parseColor("#AA888888")
            }

            // Poziom bohatera — wewnątrz kółka
            levelPaint.textSize = 7f * d
            canvas.drawText("${hero.level}", cx, heroY + 3f * d, levelPaint)

            canvas.drawText(hero.heroClass.displayName(), cx, heroY + heroR + 10f * d, namePaint)
            namePaint.color = Color.parseColor("#CCFFFFFF")
        }

        // Monster
        val monster = state.monster
        val mBarW = monsterR * 2.4f
        val mBarH = 5f * d
        drawHpBar(canvas, monsterX - mBarW / 2f, monsterY - monsterR - mBarH - 4f * d, mBarW, mBarH, monster.hp, monster.maxHp)
        circlePaint.color = monsterColorForWave(state.wave)
        canvas.drawCircle(monsterX, monsterY, monsterR, circlePaint)
        canvas.drawText(monster.name, monsterX, monsterY + monsterR + 10f * d, namePaint)

        // Toasty — na środku nakładki
        toasts.removeAll { it.second <= now }
        if (toasts.isNotEmpty()) {
            drawToasts(canvas, w, h, d)
            postInvalidateDelayed(TOAST_REFRESH_MS)
        }
    }

    private fun drawToasts(canvas: Canvas, w: Float, h: Float, d: Float) {
        toastTextPaint.textSize = 8f * d
        val toastH = 14f * d
        val pad = 8f * d
        val baseY = h * 0.50f

        toasts.takeLast(2).forEachIndexed { idx, (msg, _) ->
            val centerY = baseY + idx * (toastH + 3f * d)
            val textW = toastTextPaint.measureText(msg)
            toastRect.set(
                w / 2f - textW / 2f - pad,
                centerY - toastH / 2f,
                w / 2f + textW / 2f + pad,
                centerY + toastH / 2f
            )
            canvas.drawRoundRect(toastRect, 6f * d, 6f * d, toastBgPaint)
            canvas.drawText(msg, w / 2f, centerY + 3f * d, toastTextPaint)
        }
    }

    private fun drawHpBar(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, hp: Int, maxHp: Int) {
        canvas.drawRect(x, y, x + w, y + h, barBgPaint)
        val ratio = if (maxHp > 0) hp.toFloat() / maxHp else 0f
        barFgPaint.color = when {
            ratio > 0.6f -> Color.parseColor("#66BB6A")
            ratio > 0.3f -> Color.parseColor("#FFA726")
            else         -> Color.parseColor("#EF5350")
        }
        canvas.drawRect(x, y, x + w * ratio, y + h, barFgPaint)
    }

    // --- Touch ---
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastRawX = event.rawX; lastRawY = event.rawY; dragging = false; return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastRawX
                val dy = event.rawY - lastRawY
                if (!dragging && (abs(dx) > 8 || abs(dy) > 8)) dragging = true
                if (dragging) {
                    params.x += dx.toInt(); params.y += dy.toInt()
                    windowManager.updateViewLayout(this, params)
                    lastRawX = event.rawX; lastRawY = event.rawY
                }
                return true
            }
            MotionEvent.ACTION_UP -> return true
        }
        return false
    }

    // --- Presentation helpers (warstwa :app — nie należą do :core) ---

    private fun HeroClass.toColor() = when (this) {
        HeroClass.WARRIOR -> Color.parseColor("#4FC3F7")
        HeroClass.MAGE    -> Color.parseColor("#CE93D8")
        HeroClass.ARCHER  -> Color.parseColor("#A5D6A7")
    }

    private fun monsterColorForWave(wave: Int) = when {
        wave <= 5  -> Color.parseColor("#EF9A9A")
        wave <= 10 -> Color.parseColor("#FFCC80")
        wave <= 20 -> Color.parseColor("#80CBC4")
        wave <= 30 -> Color.parseColor("#B0BEC5")
        else       -> Color.parseColor("#FF8A65")
    }

    companion object {
        private const val TOAST_DURATION_MS = 2500L
        private const val TOAST_REFRESH_MS  = 50L
        private const val MAX_TOASTS        = 3
    }
}
