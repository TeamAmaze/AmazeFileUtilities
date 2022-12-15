/*
 * Copyright (C) 2021-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
