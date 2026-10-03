package com.sergio.frasesestoicas

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File

val FONTS = listOf(
    Triple("caveat", "Manuscrita moderna", 1f),
    Triple("homemade_apple", "Pluma antigua", .72f),
    Triple("dancing_script", "Cursiva fluida", .9f),
    Triple("pinyon_script", "Caligrafía clásica", 1.15f),
    Triple("zeyada", "Pluma rápida", 1f),
)
private val FONT_RES = mapOf("caveat" to R.font.caveat, "homemade_apple" to R.font.homemade_apple,
    "dancing_script" to R.font.dancing_script, "pinyon_script" to R.font.pinyon_script, "zeyada" to R.font.zeyada)

fun fontOf(ctx: Context, key: String): Typeface = ctx.resources.getFont(FONT_RES[key] ?: R.font.homemade_apple)

class QuoteRenderer(private val ctx: Context) {
    private val paper = BitmapFactory.decodeResource(ctx.resources, R.drawable.papel)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var bg: Bitmap? = null
    private var bgKey = ""

    private fun background(c: Canvas, w: Int, h: Int, path: String?) {
        val f = path?.let { File(it) }?.takeIf { it.exists() }
        if (f == null) {
            paint.shader = LinearGradient(0f, 0f, w * .6f, h.toFloat(), intArrayOf(0xFF2F4A63.toInt(), 0xFF1B2433.toInt(), 0xFF3B2A3F.toInt()), null, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint); paint.shader = null; return
        }
        val key = "${f.path}${f.lastModified()}$w$h"
        if (key != bgKey) {
            val b = BitmapFactory.decodeFile(f.path)
            bg = b?.let {
                val sc = maxOf(w / it.width.toFloat(), h / it.height.toFloat())
                val sw = (w / sc).toInt().coerceAtMost(it.width); val sh = (h / sc).toInt().coerceAtMost(it.height)
                Bitmap.createBitmap(it, (it.width - sw) / 2, (it.height - sh) / 2, sw, sh)
            }
            bgKey = key
        }
        bg?.let { c.drawBitmap(it, null, Rect(0, 0, w, h), paint) } ?: c.drawColor(Color.BLACK)
    }

    /** progress = nº de letras escritas (con decimales); pasado q.text.length se desvanece el autor. */
    fun draw(c: Canvas, w: Int, h: Int, s: Prefs, q: Quote?, progress: Float, card: Boolean, bgPath: String? = s.bgPath) {
        background(c, w, h, bgPath)
        if (!card || q == null) return
        val cardW = w * .88f
        val pad = cardW * .09f
        val k = FONTS.firstOrNull { it.first == s.font }?.third ?: 1f
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontOf(ctx, s.font); textSize = s.size * k * w / 360f; color = 0xEE2A1A0C.toInt()
        }
        val lay = StaticLayout.Builder.obtain(q.text, 0, q.text.length, tp, (cardW - 2 * pad).toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER).setLineSpacing(0f, 1.15f).build()
        val ap = TextPaint(tp).apply { textSize = tp.textSize * .78f; color = 0xE64A2F14.toInt(); textAlign = Paint.Align.RIGHT }
        val authorH = if (s.showAuthor) ap.fontSpacing * 1.3f else 0f
        val contentH = lay.height + authorH
        val cardH = maxOf(cardW * 755f / 1200f, contentH + 2 * pad)
        val left = (w - cardW) / 2
        val top = (h * s.posY / 100f - cardH / 2).coerceIn(0f, maxOf(0f, h - cardH))
        paint.color = 0x66000000; c.drawRect(left + 3, top + 6, left + cardW + 3, top + cardH + 6, paint); paint.color = Color.WHITE
        // recorte centrado del papel para no deformarlo
        val srcH = minOf(paper.height.toFloat(), paper.width * cardH / cardW)
        val srcW = minOf(paper.width.toFloat(), paper.height * cardW / cardH)
        val sx = (paper.width - srcW) / 2; val sy = (paper.height - srcH) / 2
        c.drawBitmap(paper, Rect(sx.toInt(), sy.toInt(), (sx + srcW).toInt(), (sy + srcH).toInt()), RectF(left, top, left + cardW, top + cardH), paint)

        c.save(); c.translate(left + pad, top + (cardH - contentH) / 2)
        val full = progress.toInt().coerceAtMost(q.text.length); val frac = progress - progress.toInt()
        for (i in 0 until lay.lineCount) {
            val st = lay.getLineStart(i); val en = lay.getLineEnd(i); val x = lay.getLineLeft(i); val y = lay.getLineBaseline(i).toFloat()
            val shown = minOf(en, full)
            if (shown > st) c.drawText(q.text, st, shown, x, y, tp)
            if (full in st until en && full < q.text.length) { // letra recién escrita: aparece con fundido
                val a = tp.alpha; tp.alpha = (a * frac).toInt()
                c.drawText(q.text, full, full + 1, x + tp.measureText(q.text, st, full), y, tp); tp.alpha = a
            }
        }
        if (s.showAuthor) {
            val a = ((progress - q.text.length) / 8f).coerceIn(0f, 1f)
            ap.alpha = (ap.alpha * a).toInt()
            c.drawText("— ${q.author}", lay.width.toFloat(), lay.height + ap.fontSpacing, ap)
        }
        c.restore()
    }
}
