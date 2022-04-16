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
import android.content.*
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Point
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.amaze.fileutilities.BuildConfig
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.database.PathPreferences
import com.amaze.fileutilities.home_page.ui.analyse.ReviewAnalysisAdapter
import com.amaze.fileutilities.home_page.ui.files.MediaFileAdapter
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

class Utils {

    companion object {
        var log: Logger = LoggerFactory.getLogger(Utils::class.java)

        const val URL_PRIVACY_POLICY = "https://teamamaze.xyz/privacy-policy-utilities"
        const val URL_GITHUB_ISSUES =
            "https://github.com/TeamAmaze/AmazeFileUtilities-Issue-Tracker/issues"
        const val AMAZE_FILE_MANAGER_MAIN = "com.amaze.filemanager.ui.activities.MainActivity"
        const val AMAZE_PACKAGE = "com.amaze.filemanager"

        private const val EMAIL_EMMANUEL = "emmanuelbendavid@gmail.com"
        private const val EMAIL_RAYMOND = "airwave209gt@gmail.com"
        private const val EMAIL_VISHAL = "vishalmeham2@gmail.com"
        private const val URL_TELEGRAM = "https://t.me/AmazeFileManager"

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
        fun buildEmailIntent(text: String?, supportMail: String): Intent? {
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
            emailIntent.type = "message/rfc822"
            return emailIntent
        }

        /** Open telegram in browser  */
        fun openTelegramURL(context: Context) {
            openURL(
                URL_TELEGRAM,
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
                        else -> 3
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
                        else -> 3
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
                    positiveCallback.invoke()
                    dialog.dismiss()
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

        fun buildRateNowDialog(
            context: Context,
            positiveCallback: () -> Unit,
            neutralCallback: () -> Unit
        ): AlertDialog.Builder {
            val builder = AlertDialog.Builder(context, R.style.Custom_Dialog_Dark)
            builder
                .setTitle(R.string.rate_now_title)
                .setMessage(R.string.rate_now_message)
                .setPositiveButton(
                    context.resources.getString(R.string.take_me)
                ) { dialog, _ ->
                    positiveCallback.invoke()
                    dialog.dismiss()
                }
                .setNegativeButton(
                    context.resources.getString(R.string.later)
                ) { dialog, _ ->
                    dialog.dismiss()
                }
                .setNeutralButton(
                    context.resources.getString(R.string.dont_show_again)
                ) { dialog, _ ->
                    neutralCallback.invoke()
                    dialog.dismiss()
                }
            return builder
        }
    }
}
