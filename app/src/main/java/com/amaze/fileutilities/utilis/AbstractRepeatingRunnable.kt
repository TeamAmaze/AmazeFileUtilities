/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.utilis

import android.os.CountDownTimer
import java.lang.UnsupportedOperationException
import java.util.concurrent.TimeUnit

abstract class AbstractRepeatingRunnable(
    initialDelay: Long,
    period: Long,
    unit: TimeUnit,
    startImmediately: Boolean
) :
    Runnable {
//    private val handle: ScheduledFuture<*>
    private val countDownTimer: CountDownTimer
    val isAlive: Boolean
        get() = false

    /**
     * @param immediately sets if the cancellation occurt right now, or after the run() function
     * returns
     */
    fun cancel() {
        countDownTimer.cancel()
    }

    init {
        if (!startImmediately) {
            throw UnsupportedOperationException("RepeatingRunnables are immediately executed!")
        }
//        val threadExcecutor = Executors.newScheduledThreadPool(0)
//        handle = threadExcecutor.scheduleAtFixedRate(this, initialDelay, period, unit)
        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            var firstTime = true
            override fun onTick(millisUntilFinished: Long) {
                if (firstTime) {
                    firstTime = false
                    return
                }
                run()
            }

            override fun onFinish() {
                // do nothing
            }
        }.start()
    }
}
