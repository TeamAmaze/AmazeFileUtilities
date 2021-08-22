package com.amaze.fileutilities

import android.content.Context
import android.os.Build
import androidx.annotation.ColorRes

class Utils {

    companion object {

        /**
         * Gets color
         *
         * @param color the resource id for the color
         * @return the color
         */
        fun getColor(c: Context, @ColorRes color: Int): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                c.getColor(color)
            } else {
                c.resources.getColor(color)
            }
        }
    }

}