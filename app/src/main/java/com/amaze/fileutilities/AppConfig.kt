/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities

import android.content.Context
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import com.amaze.fileutilities.crash_report.AcraReportSenderFactory
import com.amaze.fileutilities.crash_report.ErrorActivity
import org.acra.ACRA
import org.acra.annotation.AcraCore
import org.acra.config.ACRAConfigurationException
import org.acra.config.CoreConfigurationBuilder
import org.acra.data.StringFormat
import org.opencv.android.OpenCVLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@AcraCore(
    buildConfigClass = BuildConfig::class,
    reportSenderFactoryClasses = [AcraReportSenderFactory::class]
)
class AppConfig : AmazeApplication() {

    var log: Logger = LoggerFactory.getLogger(AppConfig::class.java)

    override fun onCreate() {
        super.onCreate()

        // disabling file exposure method check for api n+
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        initACRA()
        if (!OpenCVLoader.initDebug())
            log.warn("Unable to load OpenCV!")
        else
            log.debug("OpenCV loaded Successfully!")
    }

    /**
     * Called in [.attachBaseContext] after calling the `super` method. Should be
     * overridden if MultiDex is enabled, since it has to be initialized before ACRA.
     */
    private fun initACRA() {
        if (ACRA.isACRASenderServiceProcess()) {
            return
        }
        try {
            val acraConfig = CoreConfigurationBuilder(this)
                .setBuildConfigClass(BuildConfig::class.java)
                .setReportFormat(StringFormat.JSON)
                .setSendReportsInDevMode(true)
                .setEnabled(true)
                .build()
            ACRA.init(this, acraConfig)
        } catch (ace: ACRAConfigurationException) {
            log.warn("cannot init acra", ace)
            ErrorActivity.reportError(
                this,
                ace,
                null,
                ErrorActivity.ErrorInfo.make(
                    ErrorActivity.ERROR_UNKNOWN,
                    "Could not initialize ACRA crash report",
                    R.string.app_ui_crash
                )
            )
        }
    }
}
