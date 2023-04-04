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
                val histogram = ImgUtils.getHistogram(filePath,
                    width,
                    200.px.toDouble())
                if (histogram != null) {
                    histogramBitmap?.postValue(histogram)
                } else {
                    histogramBitmap?.postValue(
                        BitmapFactory.decodeResource(resources,
                        R.drawable.ic_twotone_error_24))
                }
            } catch (e: Exception) {
                log.warn("failed to get image histogram at $filePath")
                histogramBitmap?.postValue(
                    BitmapFactory.decodeResource(resources,
                    R.drawable.ic_twotone_error_24))
            }
        }
    }
}