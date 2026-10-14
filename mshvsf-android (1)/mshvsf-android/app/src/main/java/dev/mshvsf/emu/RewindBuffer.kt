package dev.mshvsf.emu

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Rewind por snapshots de save state.
 *
 * Guardamos só o estado mais recente inteiro + uma pilha de deltas XOR comprimidos
 * (delta_n = S_n xor S_{n-1}). Como quase tudo na RAM do CPS-2 não muda entre
 * snapshots a 10 Hz, cada delta comprime para poucos KB. Voltar no tempo é
 * S_{n-1} = S_n xor delta_n.
 */
class RewindBuffer(private val maxSteps: Int) {
    private var current: ByteArray? = null
    private val deltas = ArrayDeque<ByteArray>()

    @Synchronized
    fun push(state: ByteArray) {
        val prev = current
        if (prev != null && prev.size == state.size) {
            deltas.addLast(deflate(xor(state, prev)))
            while (deltas.size > maxSteps) deltas.removeFirst()
        } else {
            deltas.clear()
        }
        current = state
    }

    /** Devolve o snapshot anterior, ou null se o histórico acabou. */
    @Synchronized
    fun pop(): ByteArray? {
        val cur = current ?: return null
        if (deltas.isEmpty()) return null
        val delta = inflate(deltas.removeLast(), cur.size)
        val prev = xor(cur, delta)
        current = prev
        return prev
    }

    @Synchronized
    fun clear() {
        current = null
        deltas.clear()
    }

    @get:Synchronized
    val steps: Int get() = deltas.size

    private fun xor(a: ByteArray, b: ByteArray): ByteArray {
        val out = ByteArray(a.size)
        for (i in a.indices) out[i] = (a[i].toInt() xor b[i].toInt()).toByte()
        return out
    }

    private fun deflate(data: ByteArray): ByteArray {
        val d = Deflater(Deflater.BEST_SPEED)
        d.setInput(data); d.finish()
        val out = ByteArrayOutputStream(data.size / 16 + 64)
        val buf = ByteArray(16 * 1024)
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        d.end()
        return out.toByteArray()
    }

    private fun inflate(data: ByteArray, size: Int): ByteArray {
        val inf = Inflater()
        inf.setInput(data)
        val out = ByteArray(size)
        var off = 0
        while (off < size && !inf.finished()) {
            val n = inf.inflate(out, off, size - off)
            if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break
            off += n
        }
        inf.end()
        return out
    }
}
