package dev.mshvsf.input

import android.view.KeyEvent

/**
 * Botões do gabinete CPS-2 (6 botões) e o keycode Android que o LibretroDroid
 * traduz para o RetroPad. O FBNeo usa para jogos de luta de 6 botões:
 *   RetroPad Y,X,L  = LP,MP,HP     RetroPad B,A,R = LK,MK,HK
 * e o LibretroDroid traduz posicionalmente: BUTTON_X->Y, BUTTON_Y->X, BUTTON_A->B, BUTTON_B->A.
 * Se algum botão sair trocado no seu aparelho, este é o ÚNICO lugar para ajustar.
 */
enum class Btn(val keyCode: Int, val label: String) {
    UP(KeyEvent.KEYCODE_DPAD_UP, "↑"),
    DOWN(KeyEvent.KEYCODE_DPAD_DOWN, "↓"),
    LEFT(KeyEvent.KEYCODE_DPAD_LEFT, "←"),
    RIGHT(KeyEvent.KEYCODE_DPAD_RIGHT, "→"),
    LP(KeyEvent.KEYCODE_BUTTON_X, "LP"),
    MP(KeyEvent.KEYCODE_BUTTON_Y, "MP"),
    HP(KeyEvent.KEYCODE_BUTTON_L1, "HP"),
    LK(KeyEvent.KEYCODE_BUTTON_A, "LK"),
    MK(KeyEvent.KEYCODE_BUTTON_B, "MK"),
    HK(KeyEvent.KEYCODE_BUTTON_R1, "HK"),
    COIN(KeyEvent.KEYCODE_BUTTON_SELECT, "COIN"),
    START(KeyEvent.KEYCODE_BUTTON_START, "START");

    companion object {
        /** Controle físico (layout Xbox) e teclado -> botão do jogo. */
        val PAD_KEYS: Map<Int, Btn> = mapOf(
            KeyEvent.KEYCODE_DPAD_UP to UP, KeyEvent.KEYCODE_DPAD_DOWN to DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT to LEFT, KeyEvent.KEYCODE_DPAD_RIGHT to RIGHT,
            KeyEvent.KEYCODE_BUTTON_X to LP, KeyEvent.KEYCODE_BUTTON_Y to MP, KeyEvent.KEYCODE_BUTTON_R1 to HP,
            KeyEvent.KEYCODE_BUTTON_A to LK, KeyEvent.KEYCODE_BUTTON_B to MK, KeyEvent.KEYCODE_BUTTON_R2 to HK,
            KeyEvent.KEYCODE_BUTTON_L1 to HK,
            KeyEvent.KEYCODE_BUTTON_SELECT to COIN, KeyEvent.KEYCODE_BUTTON_START to START,
            // teclado / encoder de fliperama
            KeyEvent.KEYCODE_U to LP, KeyEvent.KEYCODE_I to MP, KeyEvent.KEYCODE_O to HP,
            KeyEvent.KEYCODE_J to LK, KeyEvent.KEYCODE_K to MK, KeyEvent.KEYCODE_L to HK,
            KeyEvent.KEYCODE_5 to COIN, KeyEvent.KEYCODE_ENTER to START,
            KeyEvent.KEYCODE_W to UP, KeyEvent.KEYCODE_S to DOWN, KeyEvent.KEYCODE_A to LEFT, KeyEvent.KEYCODE_D to RIGHT,
        )
    }
}
