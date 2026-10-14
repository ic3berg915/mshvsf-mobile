package dev.mshvsf

import android.content.Context
import java.io.File

object Paths {
    /** Nome do .so em app/src/main/jniLibs/arm64-v8a (o buildbot entrega fbneo_libretro_android.so; renomeie com prefixo lib). */
    const val CORE_FILE = "libfbneo_libretro_android.so"

    fun roms(c: Context) = File(c.filesDir, "roms").apply { mkdirs() }
    fun system(c: Context) = File(c.filesDir, "system").apply { mkdirs() }
    fun saves(c: Context) = File(c.filesDir, "saves").apply { mkdirs() }
    fun states(c: Context) = File(c.filesDir, "states").apply { mkdirs() }

    /** Diretórios que o FBNeo lê dentro de SYSTEM/fbneo (ver README do núcleo). */
    fun fbneo(c: Context) = File(system(c), "fbneo").apply { mkdirs() }
    fun ips(c: Context, romName: String) = File(fbneo(c), "ips/$romName").apply { mkdirs() }
    fun cheats(c: Context) = File(fbneo(c), "cheats").apply { mkdirs() }
    fun patched(c: Context) = File(fbneo(c), "patched").apply { mkdirs() }

    fun coreInstalled(c: Context) = File(c.applicationInfo.nativeLibraryDir, CORE_FILE).exists()
}
