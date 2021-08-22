package com.amaze.fileutilities.image_viewer

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import androidx.annotation.FloatRange
import com.amaze.filemanager.utils.Utils
import com.amaze.fileutilities.Utils
import java.lang.Math.pow
import kotlin.math.pow

object ContrastingTextView {
    /**
     * Correctly sets text color based on a given background color so that the
     * user can see the text correctly
     */
    @JvmStatic
    fun setIntelligentTextColor(context: Context, textView: TextView, backgroundColor: Int) {
        val red = Color.red(backgroundColor) / 255.0
        val green = Color.green(backgroundColor) / 255.0
        val blue = Color.blue(backgroundColor) / 255.0

        val linearRed = computeLinearValueForChannel(red)
        val linearGreen = computeLinearValueForChannel(green)
        val linearBlue = computeLinearValueForChannel(blue)

        val luminance = (0.2126 * linearRed + 0.7152 * linearGreen + 0.0722 * linearBlue)

        val perceivedLuminance = computePerceivedLuminance(luminance)

        val color = if (perceivedLuminance > 50) android.R.color.black else android.R.color.white

        textView.setTextColor(Utils.getColor(context, color))
    }

    @FloatRange(from = 0.0, to = 1.0)
    private fun computeLinearValueForChannel(value: Double): Double {
        return if (value <= 0.04045) {
            value / 12.92
        } else {
            ((value + 0.055) / 1.055).pow(2.4)
        }
    }

    @FloatRange(from = 0.0, to = 100.0)
    private fun computePerceivedLuminance(luminance: Double): Double {
        return if (luminance <= 216.0 / 24389.0) {
            // The CIE standard states 0.008856 but 216/24389 is the intent for 0.008856451679036
            luminance * (24389.0 / 27.0)
            // The CIE standard states 903.3, but 24389/27 is the intent, making 903.296296296296296
        } else {
            luminance.pow(1.0 / 3.0) * 116 - 16
        }
    }
}