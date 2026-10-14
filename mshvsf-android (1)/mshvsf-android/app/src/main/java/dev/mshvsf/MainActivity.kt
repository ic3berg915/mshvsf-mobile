package dev.mshvsf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dev.mshvsf.mods.ModsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView

    private val pickRom = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) importRom(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(uiText("Marvel Super Heroes vs. Street Fighter", 22f, true))
        col.addView(uiText("Player pessoal (FBNeo / CPS-2) com touch, rewind e mods", 13f))
        status = uiText("", 14f)
        col.addView(status)
        col.addView(uiButton("Jogar") { play() })
        col.addView(uiButton("Importar ROM (.zip do seu romset)") {
            pickRom.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
        })
        col.addView(uiButton("Mods") { startActivity(Intent(this, ModsActivity::class.java)) })
        col.addView(uiButton("Configurações de toque e vídeo") { startActivity(Intent(this, SettingsActivity::class.java)) })
        setContentView(uiScreen(col))
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val rom = RomStore.romFile(this)
        status.text = buildString {
            append("ROM: ").append(rom?.name ?: "não importada").append('\n')
            append("Núcleo FBNeo: ").append(if (Paths.coreInstalled(this@MainActivity)) "instalado" else "AUSENTE (veja o README)")
        }
    }

    private fun play() {
        when {
            !Paths.coreInstalled(this) -> toast("Falta o núcleo libfbneo_libretro_android.so em jniLibs/arm64-v8a")
            RomStore.romFile(this) == null -> toast("Importe a ROM primeiro")
            else -> startActivity(Intent(this, GameActivity::class.java))
        }
    }

    private fun importRom(uri: Uri) {
        val app = applicationContext
        lifecycleScope.launch {
            val r = withContext(Dispatchers.IO) { RomStore.importFrom(app, uri) }
            toast(r.fold({ "ROM importada: ${it.name}" }, { "Erro: ${it.message}" }))
            refresh()
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
