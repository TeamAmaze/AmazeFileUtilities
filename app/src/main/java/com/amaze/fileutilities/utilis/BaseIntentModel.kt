package com.amaze.fileutilities.utilis

import android.content.Context
import android.net.Uri

interface BaseIntentModel {
    fun getUri(): Uri
    fun getName(context: Context): String
}