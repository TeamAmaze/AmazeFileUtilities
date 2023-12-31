/*
 * Copyright (C) 2021-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Utilities.
 *
 * Amaze File Utilities is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.fileutilities.image_viewer

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amaze.fileutilities.R
import com.amaze.fileutilities.utilis.ImgUtils
import com.amaze.fileutilities.utilis.log
import com.amaze.fileutilities.utilis.px
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ImageFragmentViewModel : ViewModel() {

    private var histogramBitmap: MutableLiveData<Bitmap?>? = null

    fun loadHistogram(filePath: String, width: Double, resources: Resources): LiveData<Bitmap?> {
        if (histogramBitmap == null) {
            histogramBitmap = MutableLiveData()
            histogramBitmap?.value = null
            processHistogram(filePath, width, resources)
        }
        return histogramBitmap!!
    }

    private fun processHistogram(filePath: String, width: Double, resources: Resources) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val histogram = ImgUtils.getHistogram(
                    filePath,
                    width,
                    200.px.toDouble()
                )
                if (histogram != null) {
                    histogramBitmap?.postValue(histogram)
                } else {
                    histogramBitmap?.postValue(
                        BitmapFactory.decodeResource(
                            resources,
                            R.drawable.ic_twotone_error_24
                        )
                    )
                }
            } catch (e: Exception) {
                log.warn("failed to get image histogram at $filePath")
                histogramBitmap?.postValue(
                    BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_twotone_error_24
                    )
                )
            }
        }
    }
}
