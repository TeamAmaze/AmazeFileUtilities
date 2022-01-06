/*
 * Copyright (C) 2021-2020 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package crash_report

import android.content.Context
import com.amaze.fileutilities.R
import org.acra.data.CrashReportData
import org.acra.sender.ReportSender

class AcraReportSender : ReportSender {

    override fun send(context: Context, errorContent: CrashReportData) {
        ErrorActivity.reportError(
            context, errorContent,
            ErrorActivity.ErrorInfo.make(
                ErrorActivity.ERROR_UI_ERROR,
                "Application crash", R.string.app_ui_crash
            )
        )
    }

    override fun requiresForeground(): Boolean {
        return true
    }
}
