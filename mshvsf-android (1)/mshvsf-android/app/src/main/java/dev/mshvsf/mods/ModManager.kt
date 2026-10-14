package dev.mshvsf.mods

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.Variable
import dev.mshvsf.Paths
import dev.mshvsf.RomStore
import dev.mshvsf.Settings
import java.io.File

/**
 * Mods suportados pelo núcleo FBNeo (ver README do núcleo, "System Directory Layout"):
 *  - Patches IPS   -> SYSTEM/fbneo/ips/<romset>/*.dat|*.ips      (opção do núcleo "IPS Patch")
 *  - Cheats nativos-> SYSTEM/fbneo/cheats/                         (opção "Cheat")
 *  - Romsets modificados (hacks) -> SYSTEM/fbneo/patched/          (opção "Allow patched romsets")
 *  - hiscore.dat   -> SYSTEM/fbneo/                                (opção "Hiscores")
 */
enum class ModKind(val title: String, val hint: String) {
    IPS("Patches IPS (.dat / .ips)", "Hacks de balanceamento, sprites, paletas e traduções"),
    CHEATS("Cheats (arquivos do pacote FBNeo-cheats)", "Vida infinita, medidor cheio, etc."),
    PATCHED("Romsets modificados (.zip)", "Mesmo nome e tamanhos de arquivo do original"),
    HISCORE("hiscore.dat", "Placar salvo entre sessões");

    fun dir(c: Context): File = when (this) {
        IPS -> Paths.ips(c, RomStore.romName(c))
        CHEATS -> Paths.cheats(c)
        PATCHED -> Paths.patched(c)
        HISCORE -> Paths.fbneo(c)
    }
}

object ModManager {

    fun list(c: Context, kind: ModKind): List<File> =
        (kind.dir(c).listFiles()?.toList() ?: emptyList())
            .filter { it.isFile && (kind != ModKind.HISCORE || it.name == "hiscore.dat") }
            .sortedBy { it.name.lowercase() }

    fun importFrom(c: Context, kind: ModKind, uris: List<Uri>): Int {
        var n = 0
        for (uri in uris) runCatching {
            var name = c.contentResolver.query(uri, null, null, null, null)?.use {
                if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
            } ?: continue
            if (kind == ModKind.HISCORE) name = "hiscore.dat"
            if (kind == ModKind.PATCHED) name = "${RomStore.romName(c)}.zip"   // o FBNeo procura pelo nome do romset
            // nunca confiar no nome vindo do provider: remove qualquer caminho
            name = File(name).name
            val dest = File(kind.dir(c), name)
            c.contentResolver.openInputStream(uri)!!.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
            n++
        }
        return n
    }

    fun delete(file: File) = file.delete()

    /**
     * Liga/desliga as opções do núcleo conforme as escolhas do usuário. As chaves exatas variam entre
     * versões do FBNeo, então descobrimos pelo nome em runtime. Retorna true se algo mudou
     * (IPS/romset exigem reset do jogo para valer).
     */
    fun applyCoreOptions(view: GLRetroView, s: Settings): Boolean {
        val vars = view.getVariables()
        val updates = ArrayList<Variable>()
        fun want(match: Regex, on: Boolean) {
            for (v in vars) {
                val key = v.key ?: continue
                if (!match.containsMatchIn(key.lowercase())) continue
                val target = if (on) "enabled" else "disabled"
                if (v.value != target) updates += Variable(key = key, value = target)
            }
        }
        want(Regex("(^|[-_])ips([-_]|$)"), s.modIps)
        want(Regex("cheat"), s.modCheats)
        want(Regex("patched"), s.modPatched)
        want(Regex("hiscore"), s.modHiscore)
        if (updates.isNotEmpty()) view.updateVariables(*updates.toTypedArray())
        return updates.isNotEmpty()
    }
}
