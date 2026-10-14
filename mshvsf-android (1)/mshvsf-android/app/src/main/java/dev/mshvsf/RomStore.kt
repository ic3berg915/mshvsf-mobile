package dev.mshvsf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.zip.ZipFile

/** Importa o romset de arcade (CPS-2) para o armazenamento interno do app. */
object RomStore {
    const val ROM_NAME = "mshvsf"

    fun romFile(c: Context): File? =
        Paths.roms(c).listFiles { f -> f.extension.lowercase() in setOf("zip", "7z") }?.firstOrNull()

    /** Nome do romset (sem extensão) – o FBNeo exige que bata com o nome no banco dele. */
    fun romName(c: Context): String = romFile(c)?.nameWithoutExtension ?: ROM_NAME

    fun importFrom(c: Context, uri: Uri): Result<File> = runCatching {
        val dir = Paths.roms(c)
        val tmp = File(dir, "import.tmp")
        c.contentResolver.openInputStream(uri)!!.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }

        val entries = ZipFile(tmp).use { z -> z.entries().asSequence().map { it.name.lowercase() }.toSet() }
        require(entries.isNotEmpty()) { "Arquivo não é um .zip válido" }

        var display = c.contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
        } ?: "rom.zip"
        // Reconhece o set do Marvel Super Heroes vs. Street Fighter pelos nomes internos (mvs.01, mvs.05a...)
        if (entries.any { it.startsWith("mvs.") }) display = "$ROM_NAME.zip"

        dir.listFiles()?.filter { it.isFile && it.name != tmp.name }?.forEach { it.delete() }
        val dest = File(dir, display)
        check(tmp.renameTo(dest)) { "Falha ao salvar a ROM" }
        dest
    }
}
