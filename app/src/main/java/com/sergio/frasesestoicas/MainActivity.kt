package com.sergio.frasesestoicas

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.TextPaint
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewOutlineProvider
import android.widget.*
import java.io.File

private const val BG = 0xFF16171B.toInt()
private const val SHEET = 0xFF1F2127.toInt()
private const val CHIP = 0xFF2C2F37.toInt()
private const val GOLD = 0xFFC9A96A.toInt()
private const val MUTED = 0xFF8A8D96.toInt()

/** Vista previa en vivo (mismo renderer que el fondo). Se puede arrastrar la tarjeta para colocarla. */
class PreviewView(ctx: Context, private val s: Prefs) : View(ctx) {
    private val r = QuoteRenderer(ctx)
    private var q: Quote = Quotes.active(ctx).random()
    private var start = 0L
    private val clock = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER }
    var onPos: () -> Unit = {}

    fun replay() { q = Quotes.active(context).random(); start = System.currentTimeMillis(); invalidate() }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN || e.action == MotionEvent.ACTION_MOVE) {
            s.posY = (e.y / height * 100).toInt().coerceIn(10, 90); onPos(); invalidate()
        }
        return true
    }

    override fun onDraw(c: Canvas) {
        val len = q.text.length
        val p = if (s.anim) (System.currentTimeMillis() - start) / s.speedMs.toFloat() else len + 8f
        r.draw(c, width, height, s, q, p, true)
        clock.textSize = width * .2f; c.drawText("09:41", width / 2f, height * .16f, clock)
        if (p < len + 8f) postInvalidateOnAnimation()
    }
}

class MainActivity : Activity() {
    private lateinit var s: Prefs
    private lateinit var preview: PreviewView
    private lateinit var panel: FrameLayout
    private lateinit var tabsRow: LinearLayout
    private val TABS = listOf("Frases", "Letra", "Tamaño", "Posición", "Escritura")
    private var tab = "Frases"
    private var posSeek: SeekBar? = null
    private var posLbl: TextView? = null
    private fun p(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun round(color: Int, r: Int) = GradientDrawable().apply { setColor(color); cornerRadius = p(r).toFloat() }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        s = Prefs(this)
        preview = PreviewView(this, s)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG) }
        root.setOnApplyWindowInsetsListener { v, i -> v.setPadding(0, i.systemWindowInsetTop, 0, i.systemWindowInsetBottom); i }
        setContentView(root)

        // Cabecera + interruptor general
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(p(20), p(10), p(20), p(10)) }
        head.addView(TextView(this).apply {
            text = "Frases estoicas"; textSize = 22f; setTextColor(0xFFE3C98F.toInt()); typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        head.addView(sw(s.enabled) { s.enabled = it })
        root.addView(head)

        // Vista previa con la misma proporción que la pantalla
        val dm = resources.displayMetrics
        val ph = (dm.heightPixels * .40f).toInt()
        val box = FrameLayout(this)
        box.addView(preview, FrameLayout.LayoutParams(ph * dm.widthPixels / dm.heightPixels, ph, Gravity.CENTER_HORIZONTAL))
        preview.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, o: Outline) = o.setRoundRect(0, 0, v.width, v.height, p(20).toFloat())
        }
        preview.clipToOutline = true
        preview.onPos = { posSeek?.progress = s.posY - 10; posLbl?.text = "${s.posY} %" }
        box.addView(pill("Arrastra la frase"), FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.TOP or Gravity.START).apply { setMargins(p(16), p(6), 0, 0) })
        box.addView(pill("Otra frase").apply { setOnClickListener { preview.replay() } },
            FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.TOP or Gravity.END).apply { setMargins(0, p(6), p(16), 0) })
        root.addView(box, LinearLayout.LayoutParams(MATCH_PARENT, ph))

        // Hoja inferior: pestañas + panel + botón
        val r24 = p(24).toFloat()
        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(p(16), p(14), p(16), p(14))
            background = GradientDrawable().apply { setColor(SHEET); cornerRadii = floatArrayOf(r24, r24, r24, r24, 0f, 0f, 0f, 0f) }
        }
        tabsRow = LinearLayout(this)
        sheet.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(tabsRow) })
        panel = FrameLayout(this)
        sheet.addView(ScrollView(this).apply { addView(panel) }, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f).apply { topMargin = p(14) })
        sheet.addView(TextView(this).apply {
            text = "Elige tu fondo de pantalla"; textSize = 15f; setTextColor(BG); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setPadding(0, p(14), 0, p(14)); background = round(GOLD, 28)
            setOnClickListener { startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), 1) }
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = p(8) })
        root.addView(sheet, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        showTab(tab)
        preview.replay()
    }

    private fun pill(t: String) = TextView(this).apply {
        text = t; textSize = 11.5f; setTextColor(Color.WHITE); setPadding(p(10), p(5), p(10), p(5)); background = round(0xAA000000.toInt(), 14)
    }

    private fun showTab(t: String) {
        tab = t; posSeek = null; posLbl = null
        tabsRow.removeAllViews()
        TABS.forEach { name ->
            tabsRow.addView(TextView(this).apply {
                text = name; textSize = 13.5f; setPadding(p(14), p(7), p(14), p(7))
                val on = name == t
                setTextColor(if (on) BG else 0xFFBBBBBB.toInt()); background = round(if (on) GOLD else CHIP, 18)
                if (on) typeface = Typeface.DEFAULT_BOLD
                setOnClickListener { showTab(name) }
            }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { rightMargin = p(6) })
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        when (t) {
            "Frases" -> {
                // siempre queda al menos una categoría activa
                fun upd() { if (!s.stoic && !s.motiv && !s.war) s.stoic = true; showTab(tab); preview.replay() }
                opt(col, "Estoicos", s.stoic) { s.stoic = it; upd() }
                opt(col, "Motivacionales", s.motiv) { s.motiv = it; upd() }
                opt(col, "El arte de la guerra", s.war) { s.war = it; upd() }
            }
            "Letra" -> col.addView(HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                addView(LinearLayout(context).apply {
                    FONTS.forEach { (key, name, _) ->
                        addView(TextView(context).apply {
                            text = "Aa frase\n$name"; textSize = 18f; gravity = Gravity.CENTER; typeface = fontOf(context, key)
                            setTextColor(0xFF2A1A0C.toInt()); setPadding(p(14), p(14), p(14), p(14))
                            background = GradientDrawable().apply { setColor(0xFFE9D8AE.toInt()); cornerRadius = p(10).toFloat(); if (key == s.font) setStroke(p(3), GOLD) }
                            setOnClickListener { s.font = key; showTab(tab); preview.invalidate() }
                        }, LinearLayout.LayoutParams(p(130), WRAP_CONTENT).apply { rightMargin = p(8) })
                    }
                })
            })
            "Tamaño" -> slider(col, "Tamaño de letra", 0, 100, (s.size - 16) * 100 / 24, { "$it %" }) { s.size = 16 + it * 24 / 100; preview.invalidate() }
            "Posición" -> {
                slider(col, "Posición vertical", 10, 90, s.posY, { "$it %" }, true) { s.posY = it; preview.invalidate() }
                col.addView(TextView(this).apply { text = "También puedes arrastrar la tarjeta sobre la vista previa."; textSize = 12f; setTextColor(MUTED) })
            }
            else -> {
                val speeds = listOf(90, 45, 25)
                val cur = if (!s.anim) 0 else speeds.indexOf(speeds.minBy { kotlin.math.abs(it - s.speedMs) }) + 1
                listOf("Sin animación", "Lenta", "Normal", "Rápida").forEachIndexed { i, l ->
                    opt(col, l, i == cur) {
                        if (i == 0) s.anim = false else { s.anim = true; s.speedMs = speeds[i - 1] }
                        showTab(tab); preview.replay()
                    }
                }
                opt(col, "Mostrar autor", s.showAuthor) { s.showAuthor = it; showTab(tab); preview.replay() }
            }
        }
        panel.removeAllViews(); panel.addView(col)
    }

    /** Fila de opción: con marca ✓ si está activa; on=false en las que actúan como botón. */
    private fun opt(col: LinearLayout, label: String, on: Boolean, f: (Boolean) -> Unit) {
        val r = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(p(16), p(14), p(16), p(14))
            background = GradientDrawable().apply { setColor(CHIP); cornerRadius = p(14).toFloat(); if (on) setStroke(p(2), GOLD) }
            setOnClickListener { f(!on) }
        }
        r.addView(TextView(this).apply { text = label; textSize = 15f; setTextColor(0xFFEEEEEE.toInt()) }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        if (on) r.addView(TextView(this).apply { text = "✓"; textSize = 16f; setTextColor(GOLD) })
        col.addView(r, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = p(8) })
    }

    private fun slider(col: LinearLayout, title: String, min: Int, max: Int, v: Int, fmt: (Int) -> String, isPos: Boolean = false, f: (Int) -> Unit) {
        val head = LinearLayout(this)
        head.addView(TextView(this).apply { text = title; textSize = 14f; setTextColor(0xFFEEEEEE.toInt()) }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        val lbl = TextView(this).apply { text = fmt(v); textSize = 14f; setTextColor(GOLD) }
        head.addView(lbl)
        val sb = SeekBar(this).apply {
            this.max = max - min; progress = v - min
            progressTintList = ColorStateList.valueOf(GOLD); thumbTintList = ColorStateList.valueOf(GOLD)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(x: SeekBar, pr: Int, user: Boolean) { if (user) { lbl.text = fmt(pr + min); f(pr + min) } }
                override fun onStartTrackingTouch(x: SeekBar) {}
                override fun onStopTrackingTouch(x: SeekBar) {}
            })
        }
        if (isPos) { posSeek = sb; posLbl = lbl }
        col.addView(head)
        col.addView(sb, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = p(8); bottomMargin = p(10) })
    }

    private fun sw(v: Boolean, f: (Boolean) -> Unit) = Switch(this).apply {
        isChecked = v
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        thumbTintList = ColorStateList(states, intArrayOf(GOLD, 0xFFB0B3BA.toInt()))
        trackTintList = ColorStateList(states, intArrayOf(0x66C9A96A, 0x33FFFFFF))
        setOnCheckedChangeListener { _, c -> f(c) }
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        val uri = data?.data ?: return
        if (req != 1 || res != RESULT_OK) return
        val f = File(filesDir, "bg_${System.currentTimeMillis()}.jpg") // nombre nuevo: invalida la caché del renderer
        contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        filesDir.listFiles { x -> x.name.startsWith("bg_") && x != f }?.forEach { it.delete() }
        s.bgPath = f.path; preview.invalidate()
        if (!isActiveWallpaper()) {
            Toast.makeText(this, "Pulsa «Establecer» y elige «Pantalla de bloqueo»", Toast.LENGTH_LONG).show()
            startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, QuoteWallpaperService::class.java)))
        } else Toast.makeText(this, "Fondo actualizado", Toast.LENGTH_SHORT).show()
    }

    private fun isActiveWallpaper(): Boolean {
        val wm = WallpaperManager.getInstance(this)
        val info = if (Build.VERSION.SDK_INT >= 34) wm.getWallpaperInfo(WallpaperManager.FLAG_LOCK) else wm.wallpaperInfo
        return info?.packageName == packageName
    }
}
