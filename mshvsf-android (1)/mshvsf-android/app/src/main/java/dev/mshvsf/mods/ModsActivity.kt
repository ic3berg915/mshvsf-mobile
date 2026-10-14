package dev.mshvsf.mods

import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import dev.mshvsf.*

/** Importa, lista e remove mods; escolhe quais ficam ativos no jogo. */
class ModsActivity : AppCompatActivity() {
    private lateinit var settings: Settings
    private lateinit var body: LinearLayout
    private var pendingKind: ModKind? = null

    private val picker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        val kind = pendingKind ?: return@registerForActivityResult
        val n = ModManager.importFrom(this, kind, uris)
        Toast.makeText(this, "$n arquivo(s) importado(s)", Toast.LENGTH_SHORT).show()
        render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(this)
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        setContentView(uiScreen(body))
        render()
    }

    private fun render() {
        body.removeAllViews()
        body.addView(uiText("Mods", 24f, true))
        body.addView(uiText("Os mods ativos são aplicados ao abrir o jogo (ele reinicia uma vez).", 13f))

        for (kind in ModKind.values()) {
            body.addView(uiText(kind.title, 17f, true))
            body.addView(uiText(kind.hint, 12f))
            val enabled = when (kind) {
                ModKind.IPS -> settings.modIps; ModKind.CHEATS -> settings.modCheats
                ModKind.PATCHED -> settings.modPatched; ModKind.HISCORE -> settings.modHiscore
            }
            body.addView(uiCheck("Ativar", enabled) { v ->
                when (kind) {
                    ModKind.IPS -> settings.modIps = v; ModKind.CHEATS -> settings.modCheats = v
                    ModKind.PATCHED -> settings.modPatched = v; ModKind.HISCORE -> settings.modHiscore = v
                }
            })
            body.addView(uiButton("Importar…") { pendingKind = kind; picker.launch(arrayOf("*/*")) })
            for (f in ModManager.list(this, kind)) {
                body.addView(uiButton("${f.name}  (${f.length() / 1024} KB) — toque para remover") {
                    AlertDialog.Builder(this).setTitle("Remover ${f.name}?")
                        .setPositiveButton("Remover") { _, _ -> ModManager.delete(f); render() }
                        .setNegativeButton("Cancelar", null).show()
                })
            }
        }
    }
}
