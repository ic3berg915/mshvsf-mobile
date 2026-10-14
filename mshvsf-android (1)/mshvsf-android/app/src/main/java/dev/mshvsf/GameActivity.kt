package dev.mshvsf

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import dev.mshvsf.emu.RewindBuffer
import dev.mshvsf.emu.StateStore
import dev.mshvsf.input.Btn
import dev.mshvsf.input.InputRouter
import dev.mshvsf.input.MacroRunner
import dev.mshvsf.input.TouchCmd
import dev.mshvsf.input.TouchControlsView
import dev.mshvsf.mods.ModManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameActivity : AppCompatActivity() {

    private lateinit var settings: Settings
    private lateinit var retroView: GLRetroView
    private lateinit var touch: TouchControlsView
    private lateinit var router: InputRouter
    private lateinit var macros: MacroRunner

    private var rewind = RewindBuffer(maxSteps = 200)   // ~20 s a 10 snapshots/s
    @Volatile private var running = false
    @Volatile private var rewinding = false
    private var gameReady = false
    private var menuOpen = false
    private var reshowMenu = false
    private var exiting = false
    private var slot = 1
    private var speed = 1

    private val padKeys = HashSet<Btn>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(this)
        val rom = RomStore.romFile(this)
        if (rom == null || !Paths.coreInstalled(this)) {
            Toast.makeText(this, "ROM ou núcleo ausente", Toast.LENGTH_LONG).show()
            finish(); return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val data = GLRetroViewData(this).apply {
            coreFilePath = Paths.CORE_FILE
            gameFilePath = rom.absolutePath
            systemDirectory = Paths.system(this@GameActivity).absolutePath
            savesDirectory = Paths.saves(this@GameActivity).absolutePath
            shader = settings.shaderConfig()
            rumbleEventsEnabled = false
            preferLowLatencyAudio = true
        }
        retroView = GLRetroView(this, data)
        lifecycle.addObserver(retroView)

        router = InputRouter({ settings.autoCoin }) { down, key ->
            retroView.sendKeyEvent(if (down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, key, 0)
        }
        macros = MacroRunner(router, lifecycleScope)
        touch = TouchControlsView(this, settings, router, macros, ::onTouchCmd)

        val root = FrameLayout(this)
        root.addView(retroView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        root.addView(touch, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setContentView(root)

        collectCoreFlows()
        startRewindLoop()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = showMenu()
        })
    }

    // ---------------- núcleo ----------------

    private fun collectCoreFlows() {
        lifecycleScope.launch {
            retroView.getGLRetroEvents().collect { ev ->
                // evita depender do nome exato do tipo aninhado entre versões da lib
                if (!gameReady && ev::class.java.simpleName == "FrameRendered") {
                    gameReady = true
                    onGameReady()
                }
            }
        }
        lifecycleScope.launch {
            retroView.getGLRetroErrors().collect { code ->
                toast("Erro do núcleo ($code)")
                if (code == GLRetroView.ERROR_LOAD_LIBRARY || code == GLRetroView.ERROR_LOAD_GAME ||
                    code == GLRetroView.ERROR_GL_NOT_COMPATIBLE) finish()
            }
        }
    }

    private fun onGameReady() {
        running = true
        val optionsChanged = ModManager.applyCoreOptions(retroView, settings)
        if (optionsChanged) {
            retroView.reset()      // IPS / romset modificado só valem após reiniciar
            toast("Mods aplicados")
        } else if (settings.autoResume) {
            StateStore.load(this, "auto")?.let { runCatching { retroView.unserializeState(it) } }
        }
    }

    // ---------------- rewind ----------------

    private fun startRewindLoop() {
        lifecycleScope.launch(Dispatchers.Default) {
            while (isActive) {
                if (!running || menuOpen || !settings.rewindEnabled) { delay(100); continue }
                if (rewinding) {
                    val prev = rewind.pop()
                    if (prev != null) runCatching { retroView.unserializeState(prev) }
                    delay(50)    // 2 snapshots (0,1 s cada) por segundo real = 2x de velocidade ao voltar
                } else {
                    val s = runCatching { retroView.serializeState() }.getOrNull()
                    if (s != null && s.isNotEmpty()) rewind.push(s)
                    delay(100)
                }
            }
        }
    }

    // ---------------- comandos de toque / atalhos ----------------

    private fun onTouchCmd(cmd: TouchCmd) {
        when (cmd) {
            TouchCmd.MENU -> showMenu()
            TouchCmd.REWIND_ON -> rewinding = true
            TouchCmd.REWIND_OFF -> rewinding = false
            TouchCmd.TURBO -> toggleSpeed()
            TouchCmd.QUICK_SAVE -> saveSlot(live = true)
            TouchCmd.QUICK_LOAD -> loadSlot(live = true)
            TouchCmd.FLIP_SIDE -> toast(if (macros.facingRight) "Macros: P1 à esquerda, olhando →" else "Macros: P1 à direita, olhando ←")
        }
    }

    private fun toggleSpeed() {
        speed = if (speed == 1) 2 else 1
        retroView.frameSpeed = speed
        toast("Avanço rápido x$speed")
    }

    // ---------------- save states ----------------

    /** live = emulação rodando (usa a thread do núcleo); false = pausado pelo menu. */
    private fun saveSlot(live: Boolean) {
        runCatching {
            val bytes = retroView.serializeState(useEmulationThread = live)
            StateStore.save(this, "slot$slot", bytes)
            toast("Estado salvo no slot $slot")
        }.onFailure { toast("Falha ao salvar: ${it.message}") }
    }

    private fun loadSlot(live: Boolean) {
        val bytes = StateStore.load(this, "slot$slot")
        if (bytes == null) { toast("Slot $slot vazio"); return }
        runCatching {
            if (retroView.unserializeState(bytes, useEmulationThread = live)) {
                rewind.clear()
                toast("Estado do slot $slot carregado")
            } else toast("Estado incompatível")
        }.onFailure { toast("Falha ao carregar: ${it.message}") }
    }

    // ---------------- menu de pausa ----------------

    private fun showMenu() {
        if (menuOpen || exiting) return
        menuOpen = true
        router.releaseAll(); padKeys.clear(); rewinding = false
        retroView.onPause()   // GLSurfaceView: para o loop de emulação (que roda no onDrawFrame)

        val items = arrayOf(
            "Continuar",
            "Salvar estado (slot $slot)${if (StateStore.exists(this, "slot$slot")) " ●" else ""}",
            "Carregar estado (slot $slot)",
            "Trocar slot (atual: $slot de 4)",
            "Avanço rápido: x$speed",
            "Shader: ${Settings.SHADER_NAMES[settings.shader]}",
            "Reiniciar jogo",
            "Salvar e sair",
            "Sair sem salvar",
        )
        AlertDialog.Builder(this)
            .setTitle("Pausa")
            .setItems(items) { _, i ->
                when (i) {
                    1 -> saveSlot(live = false)
                    2 -> loadSlot(live = false)
                    3 -> { slot = slot % 4 + 1; reshowMenu = true }
                    4 -> { toggleSpeed(); reshowMenu = true }
                    5 -> {
                        settings.shader = (settings.shader + 1) % Settings.SHADER_NAMES.size
                        retroView.shader = settings.shaderConfig(); reshowMenu = true
                    }
                    6 -> { retroView.reset(useEmulationThread = false); rewind.clear() }
                    7 -> { autoSave(); exiting = true; finish() }
                    8 -> { exiting = true; finish() }
                }
            }
            .setOnDismissListener {
                menuOpen = false
                when {
                    exiting -> {}
                    reshowMenu -> { reshowMenu = false; showMenu() }
                    else -> retroView.onResume()
                }
            }
            .show()
    }

    private fun autoSave() {
        runCatching { StateStore.save(this, "auto", retroView.serializeState(useEmulationThread = false)) }
    }

    // ---------------- controle físico / teclado ----------------

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val down = e.action == KeyEvent.ACTION_DOWN
        when (e.keyCode) {
            KeyEvent.KEYCODE_BACK -> return super.dispatchKeyEvent(e)
            KeyEvent.KEYCODE_BUTTON_MODE -> { if (down && e.repeatCount == 0) showMenu(); return true }
            KeyEvent.KEYCODE_BUTTON_L2 -> { rewinding = down; return true }
            KeyEvent.KEYCODE_BUTTON_THUMBR -> { if (down && e.repeatCount == 0) toggleSpeed(); return true }
        }
        if (menuOpen) return super.dispatchKeyEvent(e)
        val btn = Btn.PAD_KEYS[e.keyCode] ?: return super.dispatchKeyEvent(e)
        if (e.repeatCount > 0) return true
        if (down) padKeys += btn else padKeys -= btn
        router.setSource("padkeys", padKeys.toSet())
        return true
    }

    override fun dispatchGenericMotionEvent(e: MotionEvent): Boolean {
        val isStick = e.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        if (isStick && e.action == MotionEvent.ACTION_MOVE && !menuOpen) {
            val hatX = e.getAxisValue(MotionEvent.AXIS_HAT_X)
            val hatY = e.getAxisValue(MotionEvent.AXIS_HAT_Y)
            val x = if (hatX != 0f) hatX else e.getAxisValue(MotionEvent.AXIS_X)
            val y = if (hatY != 0f) hatY else e.getAxisValue(MotionEvent.AXIS_Y)
            val dirs = HashSet<Btn>()
            if (x < -0.5f) dirs += Btn.LEFT
            if (x > 0.5f) dirs += Btn.RIGHT
            if (y < -0.5f) dirs += Btn.UP
            if (y > 0.5f) dirs += Btn.DOWN
            router.setSource("padaxis", dirs)
            return true
        }
        return super.dispatchGenericMotionEvent(e)
    }

    // ---------------- ciclo de vida ----------------

    override fun onPause() {
        running = false
        router.releaseAll(); padKeys.clear(); rewinding = false
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (gameReady && !menuOpen) running = true
    }

    override fun onDestroy() {
        running = false
        macros.cancel()
        super.onDestroy()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
