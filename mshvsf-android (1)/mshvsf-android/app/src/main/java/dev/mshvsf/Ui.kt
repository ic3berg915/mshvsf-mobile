package dev.mshvsf

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.*

fun Context.dp(v: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

fun Context.uiText(text: String, sp: Float = 15f, bold: Boolean = false, color: Int = Color.WHITE) =
    TextView(this).apply {
        this.text = text
        textSize = sp
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(6), 0, dp(6))
    }

fun Context.uiButton(text: String, onClick: () -> Unit) = Button(this).apply {
    this.text = text
    isAllCaps = false
    setOnClickListener { onClick() }
}

fun Context.uiCheck(label: String, initial: Boolean, onChange: (Boolean) -> Unit) = CheckBox(this).apply {
    text = label
    setTextColor(Color.WHITE)
    isChecked = initial
    setOnCheckedChangeListener { _, v -> onChange(v) }
}

/** SeekBar com rótulo. valor real = min + progress * step */
fun Context.uiSeek(
    label: String, min: Float, max: Float, step: Float, initial: Float,
    fmt: (Float) -> String, onChange: (Float) -> Unit,
): LinearLayout {
    val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val title = uiText("$label: ${fmt(initial)}")
    val bar = SeekBar(this).apply {
        this.max = ((max - min) / step).toInt()
        progress = ((initial - min) / step).toInt()
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) {
                val v = min + p * step
                title.text = "$label: ${fmt(v)}"
                if (user) onChange(v)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }
    col.addView(title); col.addView(bar)
    return col
}

fun Context.uiScreen(content: LinearLayout): ScrollView {
    content.setPadding(dp(20), dp(20), dp(20), dp(20))
    return ScrollView(this).apply {
        setBackgroundColor(Color.BLACK)
        addView(content, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
