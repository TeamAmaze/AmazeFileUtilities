/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.cast.cloud

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.amaze.fileutilities.CastActivity
import com.amaze.fileutilities.utilis.ObtainableServiceBinder
import java.lang.ref.WeakReference

class CloudStreamerServiceConnection(
    private val activityRef:
        WeakReference<CastActivity>
) : ServiceConnection {
    private var specificService: CloudStreamerService? = null
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder: ObtainableServiceBinder<out CloudStreamerService?> =
            service as ObtainableServiceBinder<out CloudStreamerService?>
        specificService = binder.service
        specificService?.let {
            service ->
            activityRef.get()?.apply {
                this.cloudStreamerService = service
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        activityRef.get()?.cloudStreamerService = null
    }
}
