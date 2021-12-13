package com.amaze.fileutilities.utilis

import java.lang.UnsupportedOperationException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

abstract class AbstractRepeatingRunnable(
    initialDelay: Long, period: Long, unit: TimeUnit, startImmediately: Boolean
) :
    Runnable {
    private val handle: ScheduledFuture<*>
    val isAlive: Boolean
        get() = !handle.isDone

    /**
     * @param immediately sets if the cancellation occurt right now, or after the run() function
     * returns
     */
    fun cancel(immediately: Boolean) {
        handle.cancel(immediately)
    }

    init {
        if (!startImmediately) {
            throw UnsupportedOperationException("RepeatingRunnables are immediately executed!")
        }
        val threadExcecutor = Executors.newScheduledThreadPool(0)
        handle = threadExcecutor.scheduleAtFixedRate(this, initialDelay, period, unit)
    }
}