package com.sergio.frasesestoicas

import android.content.Context
import org.json.JSONArray
import java.util.Random

class Quote(val text: String, val author: String, val cat: String)

object Quotes {
    private var all: List<Quote>? = null

    fun list(ctx: Context): List<Quote> = all ?: run {
        val a = JSONArray(ctx.assets.open("quotes.json").bufferedReader().readText())
        List(a.length()) { val r = a.getJSONArray(it); Quote(r.getString(0), r.getString(1), r.getString(2)) }.also { all = it }
    }

    fun active(ctx: Context, s: Prefs = Prefs(ctx)): List<Quote> =
        list(ctx).filter { (it.cat == "e" && s.stoic) || (it.cat == "m" && s.motiv) || (it.cat == "g" && s.war) }.ifEmpty { list(ctx) }

    // Baraja: orden aleatorio por semilla, sin repetir hasta agotar todas las frases activas.
    fun next(ctx: Context): Quote {
        val s = Prefs(ctx)
        val l = active(ctx, s)
        val mask = (if (s.stoic) 1 else 0) + (if (s.motiv) 2 else 0) + (if (s.war) 4 else 0)
        if (s.seed == 0L || s.idx >= l.size || s.mask != mask) { s.seed = System.nanoTime(); s.idx = 0; s.mask = mask }
        val order = l.indices.shuffled(Random(s.seed))
        return l[order[s.idx++]]
    }
}
