package dev.mshvsf.input

import android.os.Handler
import android.os.Looper

/**
 * Junta várias fontes de entrada (touch, controle, macros) e envia ao núcleo apenas
 * as transições down/up. Chamar sempre da thread principal.
 */
class InputRouter(
    private val autoCoin: () -> Boolean,
    private val sink: (down: Boolean, keyCode: Int) -> Unit,
) {
    private val sources = HashMap<String, Set<Btn>>()
    private var applied: Set<Btn> = emptySet()
    private val handler = Handler(Looper.getMainLooper())

    fun setSource(id: String, buttons: Set<Btn>) {
        if (buttons.isEmpty()) sources.remove(id) else sources[id] = buttons
        apply()
    }

    fun releaseAll() {
        sources.clear()
        apply()
    }

    private fun apply() {
        val want = sources.values.flatten().toMutableSet()
        // SOCD: esquerda+direita se anulam.
        if (Btn.LEFT in want && Btn.RIGHT in want) { want.remove(Btn.LEFT); want.remove(Btn.RIGHT) }

        for (b in applied - want) sink(false, b.keyCode)
        for (b in want - applied) {
            if (b == Btn.START && autoCoin()) insertCoinThenStart() else sink(true, b.keyCode)
        }
        applied = want
    }

    /** CPS-2 precisa de crédito antes do Start: toca COIN e só então aperta START. */
    private fun insertCoinThenStart() {
        sink(true, Btn.COIN.keyCode)
        handler.postDelayed({ sink(false, Btn.COIN.keyCode) }, 70)
        handler.postDelayed({ if (Btn.START in applied) sink(true, Btn.START.keyCode) }, 150)
    }
}
