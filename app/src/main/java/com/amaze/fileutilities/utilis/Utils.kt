/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.utilis

import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageStatsObserver
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageStats
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.PowerManager
import android.os.Process
import android.os.RemoteException
import android.provider.MediaStore
import android.provider.Settings
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.GridLayoutManager
import com.amaze.fileutilities.BuildConfig
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.database.PathPreferences
import com.amaze.fileutilities.home_page.ui.analyse.ReviewAnalysisAdapter
import com.amaze.fileutilities.home_page.ui.files.MediaFileAdapter
import com.google.android.material.slider.Slider
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.Method
import java.math.BigInteger
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteOrder
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Collections
import java.util.concurrent.TimeUnit
import kotlin.math.ln
import kotlin.math.pow

class Utils {

    companion object {
        var log: Logger = LoggerFactory.getLogger(Utils::class.java)

        const val URL_PRIVACY_POLICY = "https://teamamaze.xyz/privacy-policy-utilities"
        const val URL_LICENSE_AGREEMENT = "https://teamamaze.xyz/license-agreement-utilities"
        const val URL_GITHUB_ISSUES =
            "https://github.com/TeamAmaze/AmazeFileUtilities-Issue-Tracker/issues"
        const val AMAZE_FILE_MANAGER_MAIN = "com.amaze.filemanager.ui.activities.MainActivity"
        const val AMAZE_PACKAGE = "com.amaze.filemanager"

        private const val EMAIL_EMMANUEL = "emmanuelbendavid@gmail.com"
        private const val EMAIL_RAYMOND = "airwave209gt@gmail.com"
        private const val EMAIL_VISHAL = "vishalmeham2@gmail.com"
        private const val URL_TELEGRAM = "https://t.me/AmazeFileManager"
        private const val URL_TRANSLATE = "https://crwd.in/amaze-file-utilities"

        const val EMAIL_NOREPLY_REPORTS = "no-reply@teamamaze.xyz"
        const val EMAIL_SUPPORT = "support@teamamaze.xyz"

        /**
         * Open url in browser
         *
         * @param url given url
         */
        fun openURL(url: String?, context: Context) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                log.warn("cannot open url activity not found", e)
            }
        }

        fun openActivity(context: Context, packageName: String, className: String) {
            try {
                val intent = Intent()
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.setClassName(packageName, className)
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
//                context.showToastOnBottom(context.resources.getString(R.string.install_amaze))
                showAmazeFileManagerDialog(context, packageName)
            }
        }

        fun Context.showProcessingDialog(
            layoutInflater: LayoutInflater,
            message: String
        ): AlertDialog.Builder {
            val dialogBuilder = AlertDialog.Builder(this, R.style.Custom_Dialog_Dark)
                .setCancelable(false)
            val dialogView: View = layoutInflater.inflate(R.layout.please_wait_dialog, null)
            val textView = dialogView.findViewById<TextView>(R.id.please_wait_text)
            textView.text = message
            dialogBuilder.setView(dialogView)
            return dialogBuilder
        }

        private fun showAmazeFileManagerDialog(context: Context, packageName: String) {
            val dialog = AlertDialog.Builder(
                context,
                R.style.Custom_Dialog_Dark
            ).setTitle(R.string.amaze_file_manager)
                .setPositiveButton(R.string.download) { dialog, _ ->
                    popupPlay(packageName, context)
                    dialog.dismiss()
                }
                .setNegativeButton(
                    R.string.cancel
                ) { dialog, _ -> dialog.dismiss() }
                .setMessage(R.string.amaze_fm_redirect)
                .create()
            dialog.show()
        }

        private fun popupPlay(packageName: String, context: Context) {
            val intent1 =
                Intent(Intent.ACTION_VIEW)
            try {
                intent1.data = Uri.parse(
                    String.format(
                        "market://details?id=%s",
                        packageName
                    )
                )
                context.startActivity(intent1)
            } catch (ifPlayStoreNotInstalled: ActivityNotFoundException) {
                intent1.data = Uri.parse(
                    String.format(
                        "https://play.google.com/store/apps/details?id=%s",
                        packageName
                    )
                )
                context.startActivity(intent1)
            }
        }

        fun copyToClipboard(context: Context, text: String?, message: String): Boolean {
            return try {
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(
                    context.getString(R.string.clipboard_path_copy), text
                )
                clipboard.setPrimaryClip(clip)
                true
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Builds a email intent for amaze feedback
         *
         * @param text email content
         * @param supportMail support mail for given intent
         * @return intent
         */
        fun buildEmailIntent(text: String?, supportMail: String, context: Context): Intent {
            val emailIntent = Intent(Intent.ACTION_SEND)
            val aEmailList = arrayOf(supportMail)
            val aEmailCCList = arrayOf(
                EMAIL_VISHAL,
                EMAIL_EMMANUEL,
                EMAIL_RAYMOND
            )
            emailIntent.putExtra(Intent.EXTRA_EMAIL, aEmailList)
            emailIntent.putExtra(Intent.EXTRA_CC, aEmailCCList)
            emailIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                "Feedback : Amaze File Utilities for " + BuildConfig.VERSION_NAME
            )
            if (!isNullOrEmpty(text)) {
                emailIntent.putExtra(Intent.EXTRA_TEXT, text)
            }
            val logFilePath = copyLogsFileToInternalStorage(context)
            if (logFilePath != null) {
                val logFile = File(logFilePath)
                if (logFile.exists()) {
                    log.info("Attaching logs file at path $logFilePath")
                    val logsFileUri = FileProvider.getUriForFile(
                        context, context.packageName,
                        logFile
                    )
                    emailIntent.putExtra(Intent.EXTRA_STREAM, logsFileUri)
                }
            }
            emailIntent.type = "message/rfc822"
            return emailIntent
        }

        /**
         * Copies logs file to internal storage and returns the written file path
         */
        fun copyLogsFileToInternalStorage(context: Context): String? {
            context.getExternalStorageDirectory()?.let {
                internalStoragePath ->
                val inputFile = File("/data/data/${context.packageName}/cache/logs.txt")
                if (!inputFile.exists()) {
                    log.warn("Log file not found at path ${inputFile.path}")
                    return null
                }
                /*FileInputStream(inputFile).use {
                    inputStream ->
                    val file = File(
                        internalStoragePath.path +
                            "/${TransferFragment.RECEIVER_BASE_PATH}/cache"
                    )
                    file.mkdirs()
                    val logFile = File(file, "logs.txt")
                    FileOutputStream(logFile).use {
                        outputStream ->
                        ByteStreams.copy(inputStream, outputStream)
                    }
                    return logFile.path
                }*/
                return inputFile.path
            }
            return null
        }

        /** Open telegram in browser  */
        fun openTelegramURL(context: Context) {
            openURL(
                URL_TELEGRAM,
                context
            )
        }

        /** Open crowdin in browser  */
        fun openTranslateURL(context: Context) {
            openURL(
                URL_TRANSLATE,
                context
            )
        }

        fun isNullOrEmpty(list: Collection<*>?): Boolean {
            return list == null || list.isEmpty()
        }

        fun isNullOrEmpty(string: String?): Boolean {
            return string == null || string.isEmpty()
        }

        fun setGridLayoutManagerSpan(
            gridLayoutManager: GridLayoutManager,
            adapter: MediaFileAdapter
        ) {
            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter.getItemViewType(position)) {
                        AbstractMediaFilesAdapter.TYPE_ITEM ->
                            1
                        else -> gridLayoutManager.spanCount
                    }
                }
            }
        }

        fun setGridLayoutManagerSpan(
            gridLayoutManager: GridLayoutManager,
            adapter: ReviewAnalysisAdapter
        ) {
            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter.getItemViewType(position)) {
                        AbstractMediaFilesAdapter.TYPE_ITEM ->
                            1
                        else -> gridLayoutManager.spanCount
                    }
                }
            }
        }

        /**
         * Force disables screen rotation. Useful when we're temporarily in activity because of external
         * intent, and don't have to really deal much with filesystem.
         */
        fun disableScreenRotation(activity: Activity) {
            val screenOrientation = activity.resources.configuration.orientation
            if (screenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }

        fun enableScreenRotation(activity: Activity) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        }

        fun setScreenRotationSensor(activity: Activity) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }

        fun getScreenHeight(windowManager: WindowManager): Int {
            val deviceDisplay = windowManager.defaultDisplay
            val size = Point()
            deviceDisplay?.getSize(size)
            return size.y
        }

        fun containsInPreferences(
            path: String,
            pathPreferences: List<PathPreferences>,
            inclusive: Boolean
        ):
            Boolean {
            pathPreferences.forEach {
                if (path.contains(it.path, true)) {
                    return !it.excludes
                }
            }
            return !inclusive
        }

        fun generateRandom(min: Int, max: Int): Int {
            return (Math.random() * (max - min + 1) + min).toInt()
        }

        fun wifiIpAddress(context: Context): String? {
            val wifiManager = context.applicationContext
                .getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager
            var ipAddress = wifiManager.connectionInfo.ipAddress

            // Convert little-endian to big-endianif needed
            if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                ipAddress = Integer.reverseBytes(ipAddress)
            }
            val ipByteArray: ByteArray = BigInteger.valueOf(ipAddress.toLong()).toByteArray()
            val ipAddressString: String? = try {
                InetAddress.getByAddress(ipByteArray).hostAddress
            } catch (ex: UnknownHostException) {
                log.error("Unable to get host address.", ex)
                null
            }
            return ipAddressString
        }

        fun openInMaps(context: Context, latitude: String, longitude: String) {
            val url = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }

        fun buildDeleteSummaryDialog(
            context: Context,
            positiveCallback: () -> Unit
        ): AlertDialog.Builder {
            val builder = AlertDialog.Builder(context, R.style.Custom_Dialog_Dark)
            builder
                .setTitle(R.string.delete_files_title)
                .setMessage(R.string.delete_files_message)
                .setPositiveButton(
                    context.resources.getString(R.string.yes)
                ) { dialog, _ ->
                    dialog.dismiss()
                    positiveCallback.invoke()
                }
                .setNegativeButton(
                    context.resources.getString(R.string.no)
                ) { dialog, _ ->
                    dialog.dismiss()
                }
            return builder
        }

        fun buildTrialStartedDialog(context: Context, trialDays: Int): AlertDialog.Builder {
            val builder = AlertDialog.Builder(context, R.style.Custom_Dialog_Dark)
            builder
                .setTitle(context.getString(R.string.trial_started_title).format(trialDays))
                .setMessage(R.string.trial_started_message)
                .setPositiveButton(
                    context.resources.getString(R.string.close)
                ) { dialog, _ ->
                    dialog.dismiss()
                }
            return builder
        }

        fun buildLastTrialDayDialog(
            context: Context,
            positiveCallback: () -> Unit
        ): AlertDialog.Builder {
            val builder = AlertDialog.Builder(context, R.style.Custom_Dialog_Dark)
            builder
                .setTitle(R.string.trial_last_day_title)
                .setMessage(R.string.trial_last_day_message)
                .setPositiveButton(
                    context.resources.getString(R.string.subscribe)
                ) { dialog, _ ->
                    positiveCallback.invoke()
                    dialog.dismiss()
                }.setNegativeButton(
                    context.resources.getString(R.string.close)
                ) { dialog, _ ->
                    dialog.dismiss()
                }
            return builder
        }

        fun buildTrialExpiredDialog(
            context: Context,
            positiveCallback: () -> Unit
        ): AlertDialog.Builder {
            val builder = AlertDialog.Builder(context, R.style.Custom_Dialog_Dark)
            builder
                .setTitle(R.string.trial_expired_title)
                .setMessage(R.string.trial_expired_message)
                .setPositiveButton(
                    context.resources.getString(R.string.subscribe)
                ) { dialog, _ ->
                    positiveCallback.invoke()
                    dialog.dismiss()
                }
                .setNegativeButton(
                    context.resources.getString(R.string.close)
                ) { dialog, _ ->
                    dialog.dismiss()
                }
            return builder
        }

        fun buildTrialExclusiveInactiveDialog(
            context: Context,
            positiveCallback: () -> Unit
        ): AlertDialog.Builder {
            val builder = AlertDialog.Builder(context, R.style.Custom_Dialog_Dark)
            builder
                .setTitle(R.string.trial_inactive_title)
                .setMessage(R.string.trial_inactive_message)
                .setPositiveButton(
                    context.resources.getString(R.string.subscribe)
                ) { dialog, _ ->
                    positiveCallback.invoke()
                    dialog.dismiss()
                }
                .setNegativeButton(
                    context.resources.getString(R.string.close)
                ) { dialog, _ ->
                    dialog.dismiss()
                }
            return builder
        }

        fun buildSubscriptionPurchasedDialog(
            context: Context
        ): AlertDialog.Builder {
            val builder = AlertDialog.Builder(context, R.style.Custom_Dialog_Dark)
            builder
                .setTitle(R.string.subscription_purchased_title)
                .setMessage(R.string.subscription_purchased_message)
                .setPositiveButton(
                    context.resources.getString(R.string.close)
                ) { dialog, _ ->
                    dialog.dismiss()
                }
            return builder
        }

        fun buildSubscriptionExpiredDialog(
            context: Context,
            positiveCallback: () -> Unit
        ): AlertDialog.Builder {
            val builder = AlertDialog.Builder(context, R.style.Custom_Dialog_Dark)
            builder
                .setTitle(R.string.subscription_expired_title)
                .setMessage(R.string.subscription_expired_message)
                .setPositiveButton(
                    context.resources.getString(R.string.subscribe)
                ) { dialog, _ ->
                    positiveCallback.invoke()
                    dialog.dismiss()
                }
                .setNegativeButton(
                    context.resources.getString(R.string.close)
                ) { dialog, _ ->
                    dialog.dismiss()
                }
            return builder
        }

        fun buildNotConnectedTrialValidationDialog(
            context: Context
        ): AlertDialog.Builder {
            val builder = AlertDialog.Builder(context, R.style.Custom_Dialog_Dark)
            builder
                .setTitle(R.string.not_connected_trial_title)
                .setMessage(R.string.not_connected_trial_message)
                .setPositiveButton(
                    context.resources.getString(R.string.close)
                ) { dialog, _ ->
                    dialog.dismiss()
                }
            return builder
        }

        fun deleteFromMediaDatabase(context: Context, file: String) {
            val where = MediaStore.MediaColumns.DATA + "=?"
            val selectionArgs = arrayOf(file)
            val contentResolver = context.contentResolver
            val filesUri = MediaStore.Files.getContentUri("external")
            // Delete the entry from the media database. This will actually delete media files.
            contentResolver.delete(filesUri, where, selectionArgs)
        }

        fun getOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder().readTimeout(2, TimeUnit.MINUTES)
                .connectTimeout(2, TimeUnit.MINUTES)
                .connectionPool(ConnectionPool(0, 5, TimeUnit.MINUTES))
                .protocols(listOf(Protocol.HTTP_1_1))
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }

        fun buildGridColumnsDialog(
            context: Context,
            checkedItemIdx: Int,
            positiveCallback: (gridSize: Int) -> Unit,
        ): AlertDialog.Builder {
            val builder = AlertDialog.Builder(context, R.style.Custom_Dialog_Dark)
            builder
                .setTitle(R.string.columns_grid_title)
                .setSingleChoiceItems(
                    R.array.columns, checkedItemIdx
                ) { dialog, which ->
                    positiveCallback.invoke(which + 2)
                    dialog?.dismiss()
                }
                .setNegativeButton(
                    context.resources.getString(R.string.close)
                ) { dialog, _ ->
                    dialog.dismiss()
                }
            return builder
        }

        fun showPlaybackPropertiesDialog(
            context: Context,
            layoutInflater: LayoutInflater,
            defaultPlayback: Float,
            defaultPitch: Float,
            applyCallback: (Float, Float) -> Unit,
            dismissCallback: () -> Unit
        ): AlertDialog.Builder {
            val dialogBuilder = AlertDialog.Builder(context, R.style.Custom_Dialog_Dark)
                .setTitle(R.string.playback_properties)
                .setNegativeButton(R.string.close) { dialog, _ ->
                    dismissCallback.invoke()
                    dialog.dismiss()
                }

            val dialogView: View = layoutInflater
                .inflate(R.layout.playback_speed_pitch_dialog, null)
            dialogBuilder.setView(dialogView)
            val playbackSlider = dialogView.findViewById(R.id.playback_speed_slider) as Slider
            val playbackValue = dialogView.findViewById<TextView>(R.id.playback_speed_value)
            playbackSlider.valueFrom = 0.25f
            playbackSlider.valueTo = 2.0f
            playbackSlider.stepSize = 0.05f
            playbackSlider.value = defaultPlayback
            playbackValue.text = playbackSlider.value.toString() + "x"
            playbackSlider.addOnChangeListener(
                Slider.OnChangeListener { _, value, fromUser ->
                    if (fromUser) {
                        playbackValue.text = value.toString() + "x"
                    }
                }
            )
            val pitchSlider = dialogView.findViewById(R.id.pitch_slider) as Slider
            val pitchValue = dialogView.findViewById<TextView>(R.id.pitch_value)
            val pitchHintImageView = dialogView.findViewById<ImageView>(R.id.pitch_hint)
            val pitchHintTextView = dialogView.findViewById<TextView>(R.id.pitch_hint_text_view)
            pitchHintImageView.setOnClickListener {
                if (pitchHintTextView.isVisible) {
                    pitchHintTextView.hideFade(300)
                } else {
                    pitchHintTextView.showFade(300)
                }
            }
            pitchSlider.valueFrom = -12f
            pitchSlider.valueTo = 12f
            pitchSlider.stepSize = 0.5f

            pitchSlider.value = defaultPitch
            pitchValue.text = roundOffDecimal(pitchSlider.value)
            pitchSlider.addOnChangeListener(
                Slider.OnChangeListener { _, value, fromUser ->
                    if (fromUser) {
                        pitchValue.text = pitchSlider.value.toString()
                    }
                }
            )
            dialogBuilder.setPositiveButton(
                R.string.apply
            ) { dialog, _ ->
                context.getAppCommonSharedPreferences().edit()
                    .putFloat(
                        PreferencesConstants.KEY_PLAYBACK_SEMITONES,
                        pitchSlider.value
                    ).apply()
                applyCallback.invoke(playbackSlider.value, fromSemitoneToPitch(pitchSlider.value))
                dialog.dismiss()
            }
            dialogBuilder.setNeutralButton(
                context.resources.getString(R.string.reset)
            ) { dialog, _ ->
                playbackSlider.value = 1.0f
//                playbackValue.text = playbackSlider.value.toString() + "x"
                context.getAppCommonSharedPreferences().edit()
                    .remove(PreferencesConstants.KEY_PLAYBACK_SEMITONES).apply()
                pitchSlider.value = 0f
//                pitchValue.text = roundOffDecimal(0f / 0.06f)
                applyCallback.invoke(playbackSlider.value, fromSemitoneToPitch(pitchSlider.value))
                dialog.dismiss()
            }
            return dialogBuilder
        }

        private fun fromSemitoneToPitch(semitone: Float): Float {
            return 2.0.pow((semitone / 12).toDouble()).toFloat()
        }

        private fun fromPitchToSemitone(pitch: Float): Float {
            return (ln(pitch.toDouble()) * 12).toFloat()
        }

        private fun roundOffDecimal(number: Float): String {
            val df = DecimalFormat("#.#")
//            df.roundingMode = RoundingMode.CEILING
            return df.format(number)
        }

        fun generatePalette(bitmap: Bitmap?): Palette? {
            return if (bitmap == null) null else Palette.from(bitmap).generate()
        }

        const val PALETTE_DARKEN_INTENSITY_HIGH = 0.2f
        const val PALETTE_DARKEN_INTENSITY_MEDIUM = 0.4f

        @ColorInt
        fun getColorDark(palette: Palette?, fallback: Int): Int {
            val toReturn = getPaletteColor(palette, fallback)
            val light = shiftBackgroundColorForLightText(toReturn, PALETTE_DARKEN_INTENSITY_MEDIUM)
            val dark = shiftBackgroundColorForLightText(toReturn, PALETTE_DARKEN_INTENSITY_HIGH)
            if (light == dark || toReturn == fallback) {
                return fallback
            }
            return dark
        }

        @ColorInt
        fun getColor(palette: Palette?, fallback1: Int, fallback2: Int): Pair<Int, Int> {
            val toReturn = getPaletteColor(palette, fallback1)
            val light = shiftBackgroundColorForLightText(toReturn, PALETTE_DARKEN_INTENSITY_MEDIUM)
            val dark = shiftBackgroundColorForLightText(toReturn, PALETTE_DARKEN_INTENSITY_HIGH)
            if (light == dark || toReturn == fallback1) {
                return Pair(fallback1, fallback2)
            }
            return Pair(light, dark)
        }

        /**
         * Animates filenames textview to marquee after a delay. Make sure to set [ ][TextView.setSelected] to false in order to stop the marquee later
         */
        @JvmStatic
        fun marqueeAfterDelay(delayInMillis: Int, marqueeView: TextView) {
            marqueeView.isSelected = false
            Handler()
                .postDelayed(
                    {
                        // marquee works only when text view has focus
                        marqueeView.isSelected = true
                    },
                    delayInMillis.toLong()
                )
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
        fun getAppsUsageStats(context: Context): List<UsageStats> {
            val usm: UsageStatsManager = (
                context.getSystemService(Context.USAGE_STATS_SERVICE)
                    as UsageStatsManager
                )
            val endTime = LocalDateTime.now()
            val sharedPrefs = context.getAppCommonSharedPreferences()
            val days = sharedPrefs.getInt(
                PreferencesConstants.KEY_UNUSED_APPS_DAYS,
                PreferencesConstants.DEFAULT_UNUSED_APPS_DAYS
            )
            val startTime = LocalDateTime.now().minusDays(days.toLong())
            return usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                endTime
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
        }

        /**
         * https://stackoverflow.com/questions/28921136/how-to-check-if-android-permission-package-usage-stats-permission-is-given
         */
        @RequiresApi(Build.VERSION_CODES.M)
        fun checkUsageStatsPermission(context: Context): Boolean {
            val appOps = context
                .getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                "android:get_usage_stats",
                Process.myUid(), context.packageName
            )
            return if (mode != AppOpsManager.MODE_ALLOWED) {
                getAppsUsageStats(context).isNotEmpty()
            } else {
                true
            }
        }

        fun uninstallPackage(pkg: String, activity: Activity): Boolean {
            try {
                val intent = Intent(Intent.ACTION_DELETE)
                intent.data = Uri.parse("package:$pkg")
                activity.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(activity, "" + e, Toast.LENGTH_SHORT).show()
                log.warn("failed to uninstall apk", e)
                return false
            }
            return true
        }

        /**
         * Check if an App is under /system or has been installed as an update to a built-in system
         * application.
         */
        fun isAppInSystemPartition(applicationInfo: ApplicationInfo): Boolean {
            return (
                (
                    applicationInfo.flags
                        and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)
                    )
                    != 0
                )
        }

        /** Check if an App is signed by system or not.  */
        fun isSignedBySystem(piApp: PackageInfo?, piSys: PackageInfo?): Boolean {
            return piApp != null && piSys != null && piApp.signatures != null &&
                piSys.signatures[0] == piApp.signatures[0]
        }

        fun openExternalApp(context: Context, packageName: String): Boolean {
            try {
                val it = context.packageManager.getLaunchIntentForPackage(packageName)
                if (null != it) context.startActivity(it)
            } catch (e: ActivityNotFoundException) {
                com.amaze.fileutilities.utilis.log.warn("app not found to open", e)
                return false
            }
            return true
        }

        fun openExternalAppInfoScreen(context: Context, packageName: String): Boolean {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                log.warn("app not found to open", e)
                return false
            }
            return true
        }

        /**
         * Finds application size
         * Ref - https://stackoverflow.com/questions/1806286/getting-installed-app-size
         * https://stackoverflow.com/questions/49667101/android-method-invoke-crashes-in-sdk-26-oreo/56616172#56616172
         */
        fun findApplicationInfoSize(
            context: Context,
            applicationInfo: ApplicationInfo
        ): Long {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val storageStatsManager =
                    context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                /*val storageManager =
                    context.getSystemService(Context.STORAGE_SERVICE) as StorageManager*/
                try {
                    val ai = context.packageManager.getApplicationInfo(
                        applicationInfo.packageName,
                        0
                    )
                    val storageStats = storageStatsManager.queryStatsForUid(
                        ai.storageUuid,
                        ai.uid
                    )
                    val cacheSize = storageStats.cacheBytes
                    val dataSize = storageStats.dataBytes
                    val apkSize = storageStats.appBytes
                    val externalSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        storageStats.externalCacheBytes
                    } else {
                        0L
                    }
                    log.info(
                        "found size for package ${applicationInfo.packageName}" +
                            " cacheSize $cacheSize , dataSize $dataSize" +
                            " , apkSize $apkSize , externalCacheSize $externalSize"
                    )
                    return cacheSize + dataSize + apkSize + externalSize
                } catch (e: Exception) {
                    log.warn("failed to extract app size for {}", applicationInfo.packageName, e)
                    return findApplicationInfoSizeFallback(applicationInfo)
                }
            } else {
                val getPackageSizeInfo: Method
                try {
                    getPackageSizeInfo = context.packageManager.javaClass
                        .getMethod(
                            "getPackageSizeInfo",
                            String::class.java,
                            Class.forName("android.content.pm.IPackageStatsObserver")
                        )
                    var size = findApplicationInfoSizeFallback(applicationInfo)
                    getPackageSizeInfo.invoke(
                        context.packageManager, applicationInfo.packageName,
                        object : IPackageStatsObserver.Stub() {
                            // error
                            @Throws(RemoteException::class)
                            override fun onGetStatsCompleted(
                                pStats: PackageStats,
                                succeeded: Boolean
                            ) {
                                log.info(
                                    "found size for package ${applicationInfo.packageName} $pStats"
                                )
                                size = pStats.codeSize + pStats.dataSize +
                                    pStats.cacheSize + pStats.externalDataSize +
                                    pStats.externalCacheSize +
                                    pStats.externalObbSize + pStats.externalMediaSize
                            }
                        }
                    )
                    return size
                } catch (e: Exception) {
                    log.warn("failed to extract app size for {}", applicationInfo.packageName, e)
                    return findApplicationInfoSizeFallback(applicationInfo)
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun applicationIsGame(info: ApplicationInfo): Boolean {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    info.category == ApplicationInfo.CATEGORY_GAME
                } else {
                    // We are suppressing deprecation since there are no other options in this API Level
                    (info.flags and ApplicationInfo.FLAG_IS_GAME) == ApplicationInfo.FLAG_IS_GAME
                }
            } catch (e: PackageManager.NameNotFoundException) {
                log.warn("Package info not found for name: " + info.packageName, e)
                false
            }
        }

        fun buildUnusedAppsDaysPrefDialog(
            context: Context,
            days: Int,
            callback: (Int) -> Unit
        ) {
            val inputEditTextField = EditText(context)
            inputEditTextField.inputType = InputType.TYPE_CLASS_NUMBER
            inputEditTextField.setText("$days")
            val dialog = AlertDialog.Builder(context, R.style.Custom_Dialog_Dark)
                .setTitle(R.string.unused_apps)
                .setMessage(R.string.unused_apps_pref_message)
                .setView(inputEditTextField)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { dialog, _ ->
                    val salt = inputEditTextField.text.toString()
                    callback.invoke(salt.toInt())
                    dialog.dismiss()
                }
                .setNegativeButton(
                    R.string.cancel
                ) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            dialog.show()
        }

        fun buildPickLyricsTypeDialog(
            context: Context,
            callback: (Int) -> Unit
        ) {
            val dialog = AlertDialog.Builder(context, R.style.Custom_Dialog_Dark)
                .setTitle(R.string.lyrics_type)
                .setItems(R.array.lyrics_type) { dialog, which ->
                    callback.invoke(which)
                    dialog?.dismiss()
                }
                .setNegativeButton(
                    context.resources.getString(R.string.close)
                ) { dialog, _ ->
                    dialog.dismiss()
                }
            dialog.show()
        }

        fun buildPasteLyricsDialog(
            context: Context,
            callback: (String) -> Unit
        ) {
            val inputEditTextField = EditText(context)
            inputEditTextField.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
            inputEditTextField.isSingleLine = false
            inputEditTextField.minimumHeight = 500
            inputEditTextField.setTextColor(Color.WHITE)
            val dialog = AlertDialog.Builder(context, R.style.Custom_Dialog_Dark)
                .setTitle(R.string.paste_lyrics)
                .setView(inputEditTextField)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { dialog, _ ->
                    callback.invoke(inputEditTextField.text.toString())
                    dialog.dismiss()
                }
                .setNegativeButton(
                    R.string.cancel
                ) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            dialog.show()
        }

        /**
         * return true if in App's Battery settings "Not optimized" and false if "Optimizing battery use"
         */
        @RequiresApi(Build.VERSION_CODES.M)
        fun isIgnoringBatteryOptimizations(context: Context): Boolean {
            val pwrm = context.applicationContext.getSystemService(POWER_SERVICE) as PowerManager
            val name = context.applicationContext.packageName
            return pwrm.isIgnoringBatteryOptimizations(name)
        }

        @RequiresApi(Build.VERSION_CODES.M)
        fun invokeNotOptimizeBatteryScreen(context: Context) {
            val intent = Intent()
            val pwrm = context.applicationContext.getSystemService(POWER_SERVICE) as PowerManager
            val name = context.applicationContext.packageName
            if (!pwrm.isIgnoringBatteryOptimizations(name)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$name")
                context.startActivity(intent)
            }
        }

        fun convertDrawableToBitmap(drawable: Drawable): Bitmap {
            return drawable.toBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, null)
        }

        private fun findApplicationInfoSizeFallback(applicationInfo: ApplicationInfo): Long {
            var cacheSize = 0L
            File(applicationInfo.sourceDir).parentFile?.let {
                cacheSize += findSize(it)
            }
            val dataSize = findSize(File(applicationInfo.dataDir))
            return cacheSize + dataSize
        }

        private fun findSize(file: File): Long {
            return if (file.isDirectory) {
                var size = 0L
                file.listFiles()?.forEach {
                    size += findSize(it)
                }
                size
            } else {
                file.length()
            }
        }

        private fun getPaletteColor(palette: Palette?, fallback: Int): Int {
            var toReturn = fallback
            if (palette != null) {
                if (palette.vibrantSwatch != null) {
                    toReturn = palette.vibrantSwatch!!.rgb
                } else if (palette.mutedSwatch != null) {
                    toReturn = palette.mutedSwatch!!.rgb
                } else if (palette.darkVibrantSwatch != null) {
                    toReturn = palette.darkVibrantSwatch!!.rgb
                } else if (palette.darkMutedSwatch != null) {
                    toReturn = palette.darkMutedSwatch!!.rgb
                } else if (palette.lightVibrantSwatch != null) {
                    toReturn = palette.lightVibrantSwatch!!.rgb
                } else if (palette.lightMutedSwatch != null) {
                    toReturn = palette.lightMutedSwatch!!.rgb
                } else if (palette.swatches.isNotEmpty()) {
                    toReturn = Collections.max(palette.swatches) { o1, o2 ->
                        o1.population - o2.population
                    }.rgb
                }
            }
            return toReturn
        }

        private fun shiftBackgroundColorForLightText(
            @ColorInt backgroundColor: Int,
            intensity: Float
        ): Int {
            var backgroundColor = backgroundColor
            while (isColorLight(backgroundColor)) {
                backgroundColor = darkenColor(backgroundColor, intensity)
            }
            return backgroundColor
        }

        private fun isColorLight(@ColorInt color: Int): Boolean {
            val darkness = 1.0 - (
                0.299 * Color.red(color).toDouble() + 0.587 * Color.green(color)
                    .toDouble() + 0.114 * Color.blue(color).toDouble()
                ) / 255.0
            return darkness < 0.7
        }

        private fun darkenColor(@ColorInt color: Int, intensity: Float): Int {
            return shiftColor(color, intensity)
        }

        private fun shiftColor(
            @ColorInt color: Int,
            @FloatRange(from = 0.0, to = 2.0) by: Float
        ): Int {
            return if (by == 1.0f) {
                color
            } else {
                val alpha = Color.alpha(color)
                val hsv = FloatArray(3)
                Color.colorToHSV(color, hsv)
                hsv[2] *= by
                (alpha shl 24) + (16777215 and Color.HSVToColor(hsv))
            }
        }
    }
}
