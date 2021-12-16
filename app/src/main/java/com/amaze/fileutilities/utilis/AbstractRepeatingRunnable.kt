package com.amaze.fileutilities.utilis

import android.content.Context
import android.os.CountDownTimer
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat
import java.lang.UnsupportedOperationException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

abstract class AbstractRepeatingRunnable(
    initialDelay: Long, period: Long, unit: TimeUnit, startImmediately: Boolean
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
            override fun onTick(millisUntilFinished: Long) {
                run()
            }

            override fun onFinish() {
                // do nothing
            }

        }.start()
    }
}