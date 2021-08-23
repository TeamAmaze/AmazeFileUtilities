package com.amaze.fileutilities

import android.app.Application
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory

class AmazeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)
    }
}