package dev.mshvsf

import android.content.Context
import android.content.SharedPreferences
import com.swordfish.libretrodroid.ShaderConfig

/** Preferências do app. Tudo persistido em SharedPreferences. */
class Settings(context: Context) {
    private val p: SharedPreferences =
        context.applicationContext.getSharedPreferences("mshvsf", Context.MODE_PRIVATE)

    private fun bool(k: String, d: Boolean) = p.getBoolean(k, d)
    private fun putBool(k: String, v: Boolean) = p.edit().putBoolean(k, v).apply()

    var touchScale: Float
        get() = p.getFloat("touchScale", 1f)
        set(v) = p.edit().putFloat("touchScale", v).apply()
    var touchOpacity: Float
        get() = p.getFloat("touchOpacity", 0.55f)
        set(v) = p.edit().putFloat("touchOpacity", v).apply()

    var haptics: Boolean get() = bool("haptics", true); set(v) = putBool("haptics", v)
    var mirrored: Boolean get() = bool("mirrored", false); set(v) = putBool("mirrored", v)
    var floatingStick: Boolean get() = bool("floatingStick", true); set(v) = putBool("floatingStick", v)
    var autoCoin: Boolean get() = bool("autoCoin", true); set(v) = putBool("autoCoin", v)
    var showMacros: Boolean get() = bool("showMacros", true); set(v) = putBool("showMacros", v)
    var rewindEnabled: Boolean get() = bool("rewind", true); set(v) = putBool("rewind", v)
    var autoResume: Boolean get() = bool("autoResume", true); set(v) = putBool("autoResume", v)

    /** 0 = padrão, 1 = CRT, 2 = LCD */
    var shader: Int
        get() = p.getInt("shader", 0)
        set(v) = p.edit().putInt("shader", v).apply()

    // Mods (traduzidos em opções do núcleo FBNeo na hora de abrir o jogo)
    var modIps: Boolean get() = bool("modIps", false); set(v) = putBool("modIps", v)
    var modCheats: Boolean get() = bool("modCheats", false); set(v) = putBool("modCheats", v)
    var modPatched: Boolean get() = bool("modPatched", false); set(v) = putBool("modPatched", v)
    var modHiscore: Boolean get() = bool("modHiscore", false); set(v) = putBool("modHiscore", v)

    fun shaderConfig(): ShaderConfig = when (shader) {
        1 -> ShaderConfig.CRT
        2 -> ShaderConfig.LCD
        else -> ShaderConfig.Default
    }

    companion object {
        val SHADER_NAMES = arrayOf("Padrão", "CRT", "LCD")
    }
}
