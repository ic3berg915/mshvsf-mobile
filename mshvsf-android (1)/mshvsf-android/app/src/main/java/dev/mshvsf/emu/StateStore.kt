package dev.mshvsf.emu

import android.content.Context
import dev.mshvsf.Paths
import java.io.File

/** Save states em disco: slot1..slot4 e "auto" (retomada automática). */
object StateStore {
    private fun file(c: Context, name: String) = File(Paths.states(c), "$name.state")

    fun save(c: Context, name: String, data: ByteArray) {
        val tmp = File(Paths.states(c), "$name.tmp")
        tmp.writeBytes(data)
        tmp.renameTo(file(c, name))
    }

    fun load(c: Context, name: String): ByteArray? =
        file(c, name).takeIf { it.exists() }?.readBytes()

    fun exists(c: Context, name: String) = file(c, name).exists()
}
