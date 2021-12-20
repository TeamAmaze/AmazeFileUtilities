package com.amaze.fileutilities.home_page.ui.files

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amaze.fileutilities.home_page.ui.CircleColorView

class FilesViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "4GB Used"
    }
    val usedSpace: LiveData<String> = _text
}