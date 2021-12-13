package com.amaze.fileutilities.utilis

import android.app.Service
import android.os.Binder

class ObtainableServiceBinder<T : Service?>(val service: T) : Binder()