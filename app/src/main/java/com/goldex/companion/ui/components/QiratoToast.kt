package com.goldex.companion.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.goldex.companion.R

/**
 * Qirato luxury branded toast utility.
 * Displays the gold Qirato raw emblem alongside feedback messages.
 */
object QiratoToast {

    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            try {
                val density = context.resources.displayMetrics.density
                val container = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
                    val padH = (16 * density).toInt()
                    val padV = (10 * density).toInt()
                    setPadding(padH, padV, padH, padV)

                    val bg = GradientDrawable().apply {
                        setColor(AndroidColor.parseColor("#121620"))
                        setStroke((1 * density).toInt(), AndroidColor.parseColor("#DFB35A"))
                        cornerRadius = 14 * density
                    }
                    background = bg

                    val icon = ImageView(context).apply {
                        setImageResource(R.drawable.ic_logo_raw)
                        val iconSize = (24 * density).toInt()
                        layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                            leftMargin = (10 * density).toInt()
                        }
                    }
                    addView(icon)

                    val text = TextView(context).apply {
                        text = message
                        setTextColor(AndroidColor.parseColor("#F1F5F9"))
                        textSize = 12.5f
                        typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    }
                    addView(text)
                }


                @Suppress("DEPRECATION")
                val toast = Toast(context).apply {
                    this.duration = duration
                    setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, (90 * density).toInt())
                    view = container
                }
                toast.show()
                return
            } catch (_: Throwable) {
                // Fallback to system default
            }
        }
        Toast.makeText(context, message, duration).show()
    }
}
