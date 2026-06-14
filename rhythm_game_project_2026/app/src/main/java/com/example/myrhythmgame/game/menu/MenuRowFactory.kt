package com.example.myrhythmgame.game.menu

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.myrhythmgame.R

fun createMenuRow(
    context: Context,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
): View {
    val density = context.resources.displayMetrics.density
    return TextView(context).apply {
        this.text = text
        textSize = 17f
        setTextColor(Color.WHITE)
        setTypeface(typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
        setBackgroundColor(context.getColor(if (selected) R.color.ui_selected else R.color.ui_row))
        setPadding((16 * density).toInt(), 0, (16 * density).toInt(), 0)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (52 * density).toInt(),
        ).apply {
            bottomMargin = (4 * density).toInt()
        }
        gravity = android.view.Gravity.CENTER_VERTICAL
        setOnClickListener { onClick() }
    }
}
