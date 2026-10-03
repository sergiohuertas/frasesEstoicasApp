package com.sergio.frasesestoicas

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class QuoteWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = E()

    private inner class E : Engine() {
        private val h = Handler(Looper.getMainLooper())
        private val r = QuoteRenderer(applicationContext)
        private var w = 0
        private var ht = 0
        private var q: Quote? = null
        private var progress = 0f
        private var card = false
        private var lastStart = 0L
        private val tick = Runnable { step() }

        // La frase solo sale con el teléfono bloqueado; desbloqueado (inicio) solo se ve la foto, sin frase.
        private val kg = getSystemService(KeyguardManager::class.java)
        private fun locked() = kg.isKeyguardLocked
        private val unlock = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { h.removeCallbacksAndMessages(null); card = false; draw() }
        }

        override fun onCreate(sh: SurfaceHolder) {
            super.onCreate(sh)
            registerReceiver(unlock, IntentFilter(Intent.ACTION_USER_PRESENT), Context.RECEIVER_NOT_EXPORTED)
        }
        override fun onDestroy() { unregisterReceiver(unlock); h.removeCallbacksAndMessages(null); super.onDestroy() }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { w = width; ht = height; draw() }
        override fun onSurfaceDestroyed(holder: SurfaceHolder) { h.removeCallbacksAndMessages(null) }

        override fun onVisibilityChanged(visible: Boolean) {
            h.removeCallbacksAndMessages(null)
            if (visible) start()
        }

        private fun start() {
            val s = Prefs(applicationContext)
            if (!s.enabled || !locked()) { card = false; draw(); return }
            val now = System.currentTimeMillis()
            if (!(card && now - lastStart < 2000)) { // el sistema a veces avisa varias veces seguidas
                q = Quotes.next(applicationContext); lastStart = now
                progress = if (s.anim) 0f else q!!.text.length + 8f
            }
            card = true
            if (progress < q!!.text.length + 8f) step() else draw()
        }

        private fun step() {
            val s = Prefs(applicationContext)
            progress += 33f / s.speedMs
            draw()
            if (progress < q!!.text.length + 8f) h.postDelayed(tick, 33)
        }

        private fun draw() {
            if (w == 0) return
            val holder = surfaceHolder
            val c = holder.lockCanvas() ?: return
            try { val s = Prefs(applicationContext); r.draw(c, w, ht, s, q, progress, card, s.bgPath) } finally { holder.unlockCanvasAndPost(c) }
        }
    }
}
