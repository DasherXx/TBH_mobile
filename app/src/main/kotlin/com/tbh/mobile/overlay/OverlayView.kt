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
import com.tbh.mobile.battle.BattleState
import kotlin.math.abs

class OverlayView(
    context: Context,
    private val windowManager: WindowManager,
    private val params: WindowManager.LayoutParams
) : View(context) {

    var state: BattleState = BattleState.initial()
        set(value) { field = value; invalidate() }

    // --- Paints ---
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8152040")  // głęboki granat, ~91% krycia
    }
    private val barBgPaint = Paint().apply { color = Color.parseColor("#55FFFFFF") }
    private val barFgPaint = Paint()
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val deadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#60000000")
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")    // złoty
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCFFFFFF")
        textAlign = Paint.Align.CENTER
    }
    private val bgRect = RectF()

    // --- Drag state ---
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var dragging = false

    init { setWillNotDraw(false) }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val d = resources.displayMetrics.density

        // Tło
        bgRect.set(0f, 0f, w, h)
        canvas.drawRoundRect(bgRect, 14f, 14f, bgPaint)

        // Tytuł/wave
        titlePaint.textSize = 11f * d
        canvas.drawText("⚔  Wave ${state.wave}", w * 0.50f, h * 0.22f, titlePaint)

        val heroR   = h * 0.18f
        val monsterR = h * 0.25f
        val heroY   = h * 0.66f
        val monsterY = h * 0.60f
        val heroXs  = floatArrayOf(w * 0.14f, w * 0.29f, w * 0.44f)
        val monsterX = w * 0.79f

        namePaint.textSize = 9f * d

        // Separator "VS"
        titlePaint.textSize = 10f * d
        canvas.drawText("VS", w * 0.615f, monsterY + 5f * d, titlePaint)

        // Bohaterowie
        state.heroes.forEachIndexed { i, hero ->
            val cx = heroXs[i]
            val barW = heroR * 2.5f
            val barH = 5f * d
            val barX = cx - barW / 2f
            val barY = heroY - heroR - barH - 5f * d
            drawHpBar(canvas, barX, barY, barW, barH, hero.hp, hero.maxHp)
            if (hero.hp > 0) {
                circlePaint.color = hero.color
                canvas.drawCircle(cx, heroY, heroR, circlePaint)
            } else {
                canvas.drawCircle(cx, heroY, heroR, deadPaint)
                namePaint.color = Color.parseColor("#AA888888")
            }
            canvas.drawText(hero.name.take(7), cx, heroY + heroR + 10f * d, namePaint)
            namePaint.color = Color.parseColor("#CCFFFFFF")  // reset
        }

        // Potwór
        val monster = state.monster
        val mBarW = monsterR * 2.4f
        val mBarH = 5f * d
        drawHpBar(canvas, monsterX - mBarW / 2f, monsterY - monsterR - mBarH - 5f * d, mBarW, mBarH, monster.hp, monster.maxHp)
        circlePaint.color = monster.color
        canvas.drawCircle(monsterX, monsterY, monsterR, circlePaint)
        canvas.drawText(monster.name, monsterX, monsterY + monsterR + 10f * d, namePaint)
    }

    private fun drawHpBar(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, hp: Int, maxHp: Int) {
        canvas.drawRect(x, y, x + w, y + h, barBgPaint)
        val ratio = if (maxHp > 0) hp.toFloat() / maxHp else 0f
        barFgPaint.color = when {
            ratio > 0.6f -> Color.parseColor("#66BB6A")  // zielony
            ratio > 0.3f -> Color.parseColor("#FFA726")  // pomarańczowy
            else         -> Color.parseColor("#EF5350")  // czerwony
        }
        canvas.drawRect(x, y, x + w * ratio, y + h, barFgPaint)
    }

    // --- Przeciąganie ---
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
}
