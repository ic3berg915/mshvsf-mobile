package dev.mshvsf.input

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Macros de toque (um botão = sequência). As direções "frente" dependem do lado
 * em que o personagem está; o botão ⇄ na tela alterna [facingRight].
 * Edite [ALL] para criar as suas.
 */
class MacroRunner(private val router: InputRouter, private val scope: CoroutineScope) {
    var facingRight = true
    private var job: Job? = null

    class Step(val buttons: Set<Btn>, val ms: Long = 50)
    class Def(val label: String, val build: (fwd: Btn, back: Btn) -> List<Step>)

    fun run(def: Def) {
        job?.cancel()
        val fwd = if (facingRight) Btn.RIGHT else Btn.LEFT
        val back = if (facingRight) Btn.LEFT else Btn.RIGHT
        val steps = def.build(fwd, back)
        job = scope.launch {
            try {
                for (s in steps) {
                    router.setSource("macro", s.buttons)
                    delay(s.ms)
                }
            } finally {
                router.setSource("macro", emptySet())
            }
        }
    }

    fun cancel() { job?.cancel() }

    companion object {
        val ALL = listOf(
            Def("3P") { _, _ -> listOf(Step(setOf(Btn.LP, Btn.MP, Btn.HP), 80)) },
            Def("3K") { _, _ -> listOf(Step(setOf(Btn.LK, Btn.MK, Btn.HK), 80)) },
            // Meia-lua frente + soco forte (hadouken-style)
            Def("QCF+HP") { f, _ -> listOf(
                Step(setOf(Btn.DOWN)), Step(setOf(Btn.DOWN, f)), Step(setOf(f, Btn.HP), 70)) },
            // Dragon punch: frente, baixo, baixo-frente + soco forte
            Def("DP+HP") { f, _ -> listOf(
                Step(setOf(f)), Step(setOf(Btn.DOWN)), Step(setOf(Btn.DOWN, f, Btn.HP), 70)) },
            // Hyper combo: duas meias-luas + soco forte
            Def("HYPER") { f, _ -> listOf(
                Step(setOf(Btn.DOWN)), Step(setOf(Btn.DOWN, f)), Step(setOf(f)),
                Step(setOf(Btn.DOWN)), Step(setOf(Btn.DOWN, f)), Step(setOf(f, Btn.HP), 70)) },
        )
    }
}
