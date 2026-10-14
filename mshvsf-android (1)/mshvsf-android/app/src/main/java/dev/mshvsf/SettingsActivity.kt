package dev.mshvsf

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.AdapterView
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val s = Settings(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(uiText("Configurações", 24f, true))

        col.addView(uiText("Controles de toque", 17f, true))
        col.addView(uiSeek("Tamanho", 0.6f, 1.6f, 0.05f, s.touchScale, { "%.0f%%".format(it * 100) }) { s.touchScale = it })
        col.addView(uiSeek("Opacidade", 0.15f, 1f, 0.05f, s.touchOpacity, { "%.0f%%".format(it * 100) }) { s.touchOpacity = it })
        col.addView(uiCheck("Vibração ao tocar", s.haptics) { s.haptics = it })
        col.addView(uiCheck("Modo canhoto (direcional à direita)", s.mirrored) { s.mirrored = it })
        col.addView(uiCheck("Direcional flutuante (nasce onde você toca)", s.floatingStick) { s.floatingStick = it })
        col.addView(uiCheck("Mostrar botões de macro (3P, 3K, QCF, DP, HYPER)", s.showMacros) { s.showMacros = it })
        col.addView(uiCheck("START insere moeda automaticamente", s.autoCoin) { s.autoCoin = it })

        col.addView(uiText("Jogo", 17f, true))
        col.addView(uiCheck("Rewind (segurar REW)", s.rewindEnabled) { s.rewindEnabled = it })
        col.addView(uiCheck("Retomar de onde parou ao abrir", s.autoResume) { s.autoResume = it })

        col.addView(uiText("Vídeo", 17f, true))
        val sp = Spinner(this).apply {
            adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, Settings.SHADER_NAMES)
            setSelection(s.shader)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { s.shader = pos }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }
        col.addView(sp)

        col.addView(uiText("Controle físico: X/Y/RB = LP/MP/HP, A/B/RT = LK/MK/HK, Select = moeda, Start, " +
            "botão Guide = menu, LT (segurar) = rewind, R3 = avanço rápido. Teclado: U I O / J K L, WASD/setas, 5 e Enter.", 12f))
        setContentView(uiScreen(col))
    }
}
