package com.sergio.frasesestoicas

import android.content.Context

class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    var enabled get() = p.getBoolean("enabled", true); set(v) = p.edit().putBoolean("enabled", v).apply()
    var stoic get() = p.getBoolean("stoic", true); set(v) = p.edit().putBoolean("stoic", v).apply()
    var motiv get() = p.getBoolean("motiv", false); set(v) = p.edit().putBoolean("motiv", v).apply()
    var war get() = p.getBoolean("war", false); set(v) = p.edit().putBoolean("war", v).apply()
    var mask get() = p.getInt("mask", 0); set(v) = p.edit().putInt("mask", v).apply()
    var posY get() = p.getInt("posY", 80); set(v) = p.edit().putInt("posY", v).apply()
    var size get() = p.getInt("size", 25); set(v) = p.edit().putInt("size", v).apply()
    var font get() = p.getString("font", "homemade_apple")!!; set(v) = p.edit().putString("font", v).apply()
    var anim get() = p.getBoolean("anim", true); set(v) = p.edit().putBoolean("anim", v).apply()
    var speedMs get() = p.getInt("speedMs", 45); set(v) = p.edit().putInt("speedMs", v).apply()
    var showAuthor get() = p.getBoolean("showAuthor", true); set(v) = p.edit().putBoolean("showAuthor", v).apply()
    var bgPath get() = p.getString("bgPath", null); set(v) = p.edit().putString("bgPath", v).apply()
    var seed get() = p.getLong("seed", 0L); set(v) = p.edit().putLong("seed", v).apply()
    var idx get() = p.getInt("idx", 0); set(v) = p.edit().putInt("idx", v).apply()
}
