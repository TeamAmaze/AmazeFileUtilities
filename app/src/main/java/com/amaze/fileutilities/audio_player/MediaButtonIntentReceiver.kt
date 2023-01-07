/*
 * Copyright (C) 2021-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.fileutilities.audio_player

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException

class MediaButtonIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (handleIntent(context, intent) && isOrderedBroadcast) {
            abortBroadcast()
        }
    }

    companion object {
        var log: Logger = LoggerFactory.getLogger(MediaButtonIntentReceiver::class.java)
        val TAG = MediaButtonIntentReceiver::class.java.simpleName
        private const val MSG_HEADSET_DOUBLE_CLICK_TIMEOUT = 2
        private const val DOUBLE_CLICK = 400
        private var mWakeLock: WakeLock? = null
        private var mClickCounter = 0
        private var mLastClickTime: Long = 0

        @SuppressLint("HandlerLeak") // false alarm, handler is already static
        private val mHandler: Handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_HEADSET_DOUBLE_CLICK_TIMEOUT -> {
                        val command: String? = when (msg.arg1) {
                            1 -> AudioPlayerService.ACTION_PLAY_PAUSE
                            2 -> AudioPlayerService.ACTION_NEXT
                            3 -> AudioPlayerService.ACTION_PREVIOUS
                            else -> null
                        }
                        if (command != null) {
                            val context = msg.obj as Context
                            startService(context, command)
                        }
                    }
                }
                releaseWakeLockIfHandlerIdle()
            }
        }

        fun handleIntent(context: Context, intent: Intent): Boolean {
            val intentAction = intent.action
            if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
                val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    ?: return false
                val keycode = event.keyCode
                val action = event.action
                val eventTime =
                    if (event.eventTime != 0L) event.eventTime else System.currentTimeMillis()
                // Fallback to system time if event time was not available.
                var command: String? = null
                when (keycode) {
                    KeyEvent.KEYCODE_MEDIA_STOP -> command = AudioPlayerService.ACTION_CANCEL
                    KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ->
                        command =
                            AudioPlayerService.ACTION_PLAY_PAUSE
                    KeyEvent.KEYCODE_MEDIA_NEXT -> command = AudioPlayerService.ACTION_NEXT
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> command = AudioPlayerService.ACTION_PREVIOUS
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> command = AudioPlayerService.ACTION_PLAY_PAUSE
                    KeyEvent.KEYCODE_MEDIA_PLAY -> command = AudioPlayerService.ACTION_PLAY_PAUSE
                }
                if (command != null) {
                    if (action == KeyEvent.ACTION_DOWN) {
                        if (event.repeatCount == 0) {
                            // Only consider the first event in a sequence, not the repeat events,
                            // so that we don't trigger in cases where the first event went to
                            // a different app (e.g. when the user ends a phone call by
                            // long pressing the headset button)

                            // The service may or may not be running, but we need to send it
                            // a command.
                            if (keycode == KeyEvent.KEYCODE_HEADSETHOOK || keycode
                                == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                            ) {
                                if (eventTime - mLastClickTime >= DOUBLE_CLICK) {
                                    mClickCounter = 0
                                }
                                mClickCounter++
                                log.info("Got headset click, count = " + mClickCounter)
                                mHandler.removeMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT)
                                val msg = mHandler.obtainMessage(
                                    MSG_HEADSET_DOUBLE_CLICK_TIMEOUT, mClickCounter, 0,
                                    context
                                )
                                val delay =
                                    if (mClickCounter < 3) DOUBLE_CLICK.toLong() else 0.toLong()
                                if (mClickCounter >= 3) {
                                    mClickCounter = 0
                                }
                                mLastClickTime = eventTime
                                acquireWakeLockAndSendMessage(context, msg, delay)
                            } else {
                                startService(context, command)
                            }
                            return true
                        }
                    }
                }
            }
            return false
        }

        private fun startService(context: Context, command: String?) {
            val intent = Intent(context, AudioPlayerService::class.java)
            intent.action = command
            try {
                // IMPORTANT NOTE: (kind of a hack)
                // on Android O and above the following crashes when the app is not running
                // there is no good way to check whether the app is running so we catch the exception
                // we do not always want to use startForegroundService() because then one gets an ANR
                // if no notification is displayed via startForeground()
                // according to Play analytics this happens a lot, I suppose
                // for example if command = PAUSE
                context.startService(intent)
            } catch (ignored: IllegalStateException) {
                ContextCompat.startForegroundService(context, intent)
            }
        }

        private fun acquireWakeLockAndSendMessage(context: Context, msg: Message, delay: Long) {
            if (mWakeLock == null) {
                val powerManager = context.getSystemService(Service.POWER_SERVICE) as PowerManager
                mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
                mWakeLock?.setReferenceCounted(false)
            }
            log.debug("Acquiring wake lock and sending " + msg.what)
            // Make sure we don't indefinitely hold the wake lock under any circumstances
            mWakeLock!!.acquire(10000)
            mHandler.sendMessageDelayed(msg, delay)
        }

        private fun releaseWakeLockIfHandlerIdle() {
            if (mHandler.hasMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT)) {
                log.debug("Handler still has messages pending, not releasing wake lock")
                return
            }
            if (mWakeLock != null) {
                log.debug("Releasing wake lock")
                mWakeLock!!.release()
                mWakeLock = null
            }
        }
    }
}
