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

import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.mediarouter.app.MediaRouteButton
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.fileutilities.cast.cloud.CloudStreamer
import com.amaze.fileutilities.cast.cloud.CloudStreamerService
import com.amaze.fileutilities.cast.cloud.CloudStreamerServiceConnection
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.MediaFileAdapter
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.showToastOnBottom
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference

abstract class CastActivity :
    PermissionsActivity(),
    SessionManagerListener<CastSession> {

    private var log: Logger = LoggerFactory.getLogger(CastActivity::class.java)

    private var mCastContext: CastContext? = null
    private var mCastSession: CastSession? = null
    private var mSessionManager: SessionManager? = null
    var cloudStreamerService: CloudStreamerService? = null
    private var cloudStreamer: CloudStreamer? = null

    private lateinit var streamerServiceConnection: ServiceConnection

    abstract fun getFilesModel(): FilesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            mCastContext = CastContext.getSharedInstance(applicationContext)
            mSessionManager = mCastContext!!.sessionManager
            mCastSession = mSessionManager?.currentCastSession
        } catch (e: Exception) {
            log.warn("failed to init cast context", e)
            getFilesModel().castSetupSuccess = false
            getFilesModel().isCasting = false
        }
        /*val mediaRouter = MediaRouter.getInstance(this)
        mediaRouter.setMediaSession(mSessionManager?.currentCastSession)*/
        streamerServiceConnection =
            CloudStreamerServiceConnection(WeakReference(this))
    }

    override fun onResume() {
        super.onResume()
        try {
            mSessionManager = CastContext.getSharedInstance(applicationContext).sessionManager
            mCastSession = mSessionManager?.currentCastSession
            mSessionManager?.addSessionManagerListener(this, CastSession::class.java)
        } catch (e: Exception) {
            log.warn("failed to init cast context", e)
            getFilesModel().castSetupSuccess = false
            getFilesModel().isCasting = false
        }
        val intent = Intent(this, CloudStreamerService::class.java)
        this.bindService(intent, streamerServiceConnection, 0)
    }

    override fun onPause() {
        super.onPause()
        mSessionManager?.removeSessionManagerListener(this, CastSession::class.java)
        mCastSession = null
        this.unbindService(streamerServiceConnection)
    }

    override fun onDestroy() {
        super.onDestroy()
        clearCastResources()
    }

    override fun onSessionEnded(p0: CastSession, p1: Int) {
        showToastOnBottom(resources.getString(R.string.cast_ended))
        getFilesModel().isCasting = false
        getFilesModel().wifiIpAddress = null
        clearCastResources()
    }

    override fun onSessionEnding(p0: CastSession) {
        showToastOnBottom(resources.getString(R.string.cast_ending))
    }

    override fun onSessionResumeFailed(p0: CastSession, p1: Int) {
//        showToastOnBottom(resources.getString(R.string.failed_resume_cast))
        getFilesModel().isCasting = false
        getFilesModel().wifiIpAddress = null
    }

    override fun onSessionResumed(p0: CastSession, p1: Boolean) {
        showToastOnBottom(resources.getString(R.string.cast_resumed))
        getFilesModel().isCasting = true
        getFilesModel().wifiIpAddress = Utils.wifiIpAddress(this)
        if (cloudStreamerService == null) {
            CloudStreamerService.runService(this)
        }
    }

    override fun onSessionResuming(p0: CastSession, p1: String) {
//        showToastOnBottom(resources.getString(R.string.resuming_cast))
    }

    override fun onSessionStartFailed(p0: CastSession, p1: Int) {
        showToastOnBottom(resources.getString(R.string.failed_to_start_cast))
        getFilesModel().isCasting = false
        getFilesModel().wifiIpAddress = null
    }

    override fun onSessionStarted(p0: CastSession, p1: String) {
        showToastOnBottom(resources.getString(R.string.ready_to_cast))
        try {
            mCastContext = CastContext.getSharedInstance(applicationContext)
            getFilesModel().isCasting = true
            getFilesModel().wifiIpAddress = Utils.wifiIpAddress(this)
            if (cloudStreamerService == null) {
                CloudStreamerService.runService(this)
            }
        } catch (e: Exception) {
            log.warn("failed to start cast session", e)
            getFilesModel().castSetupSuccess = false
            getFilesModel().isCasting = false
            showToastOnBottom(resources.getString(R.string.failed_to_start_cast))
        }
    }

    override fun onSessionStarting(p0: CastSession) {
        showToastOnBottom(resources.getString(R.string.establishing_cast))
    }

    override fun onSessionSuspended(p0: CastSession, p1: Int) {
        showToastOnBottom(resources.getString(R.string.cast_suspended))
        getFilesModel().isCasting = false
        clearCastResources()
    }

    fun refactorCastButton(mediaRouteButton: MediaRouteButton) {
        val wifiIpAddress = Utils.wifiIpAddress(this)
        if (getFilesModel().isCasting) {
            mediaRouteButton.setRemoteIndicatorDrawable(
                resources
                    .getDrawable(R.drawable.ic_baseline_cast_connected_32)
            )
            mediaRouteButton.visibility = View.VISIBLE
            CastButtonFactory.setUpMediaRouteButton(
                applicationContext,
                mediaRouteButton
            )
        } else {
            mediaRouteButton.setRemoteIndicatorDrawable(
                resources
                    .getDrawable(R.drawable.ic_baseline_cast_32)
            )

            if (wifiIpAddress != null) {
                mediaRouteButton.visibility = View.VISIBLE

                if (!getFilesModel().castSetupSuccess) {
                    mediaRouteButton.setOnClickListener {
                        showToastOnBottom(getString(R.string.cast_framework_unavailable))
                    }
                } else {
                    CastButtonFactory.setUpMediaRouteButton(
                        applicationContext,
                        mediaRouteButton
                    )
                }
            } else {
                mediaRouteButton.visibility = View.GONE
            }
        }
    }

    /**
     * If we're casting right now, ask user whether to cast media file or open through
     * inbuilt player otherwise directly open the file
     */
    fun showCastFileDialog(
        mediaFileInfo: MediaFileInfo,
        mediaType: Int,
        inbuiltCallback: () -> Unit
    ) {
        initCastContext()
        if (!getFilesModel().isCasting) {
            inbuiltCallback.invoke()
        } else {
            MaterialDialog
            val dialog = AlertDialog.Builder(this, R.style.Custom_Dialog_Dark)
                .setTitle(R.string.open_with)
                .setPositiveButton(
                    R.string.inbuilt_player
                ) { dialog, _ ->
                    inbuiltCallback.invoke()
                    dialog.dismiss()
                }
                .setNeutralButton(
                    R.string.cancel,
                ) {
                    dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton(
                    R.string.chromecast
                ) { dialog, _ ->
                    startCastPlayback(mediaFileInfo, mediaType)
                    dialog.dismiss()
                }
                .create()
            dialog.show()
        }
    }

    private fun initCastContext() {
        try {
            mCastContext = CastContext.getSharedInstance(applicationContext)
            mSessionManager = mCastContext!!.sessionManager
            mCastSession = mSessionManager?.currentCastSession
            mSessionManager?.addSessionManagerListener(this, CastSession::class.java)
        } catch (e: Exception) {
            log.warn("failed to init cast context", e)
            showToastOnBottom(resources.getString(R.string.failed_to_start_cast))
            getFilesModel().castSetupSuccess = false
            getFilesModel().isCasting = false
        }
    }

    private fun submitStreamSrc(mediaFileInfo: MediaFileInfo) {
        cloudStreamerService?.setStreamSrc(mediaFileInfo)
    }

    private fun startCastPlayback(mediaFileInfo: MediaFileInfo, mediaType: Int) {
        initCastContext()
        if (cloudStreamerService == null) {
            CloudStreamerService.runService(this)
        }
        val remoteMediaClient = mCastSession?.remoteMediaClient ?: return
        submitStreamSrc(mediaFileInfo)
        getFilesModel().wifiIpAddress?.let {
            ipAddress ->
            mediaFileInfo.getContentUri(this)?.let {
                mediaFileUri ->
                val uri = Uri.parse(
                    "http://$ipAddress:${CloudStreamer.PORT}" +
                        "${mediaFileUri.encodedPath}"
                )
                val metadata: MediaMetadata
                var mediaInfo: MediaInfo? = null
                when (mediaType) {
                    MediaFileAdapter.MEDIA_TYPE_IMAGES -> {
                        metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_PHOTO)
                        metadata.putString(MediaMetadata.KEY_TITLE, mediaFileInfo.title)
                        metadata.putString(MediaMetadata.KEY_SUBTITLE, mediaFileInfo.path)

                        mediaInfo = MediaInfo.Builder(uri.toString())
                            .setStreamType(MediaInfo.STREAM_TYPE_NONE)
                            .setMetadata(metadata)
                            .setContentType(MimeTypes.IMAGE_JPEG)
                            .build()
                    }
                    MediaFileAdapter.MEDIA_TYPE_AUDIO -> {
                        metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
                        metadata.putString(MediaMetadata.KEY_TITLE, mediaFileInfo.title)
                        metadata.putString(MediaMetadata.KEY_SUBTITLE, mediaFileInfo.path)
//                    metadata.addImage(WebImage(Uri.fromFile()))
                        val duration = mediaFileInfo.extraInfo?.audioMetaData?.duration ?: 0

                        mediaInfo = MediaInfo.Builder(uri.toString())
                            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                            .setMetadata(metadata)
//                        .setStreamDuration(duration)
                            .setContentType(MimeTypes.BASE_TYPE_AUDIO)
                            .build()
                    }
                    MediaFileAdapter.MEDIA_TYPE_VIDEO -> {
                        metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
                        metadata.putString(MediaMetadata.KEY_TITLE, mediaFileInfo.title)
                        metadata.putString(MediaMetadata.KEY_SUBTITLE, mediaFileInfo.path)

                        val duration = mediaFileInfo.extraInfo?.audioMetaData?.duration ?: 0

                        mediaInfo = MediaInfo.Builder(uri.toString())
                            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                            .setMetadata(metadata)
                            .setContentType(MimeTypes.BASE_TYPE_VIDEO)
                            //            .setStreamDuration(1000 * duration)
                            .build()
                    }
                    MediaFileAdapter.MEDIA_TYPE_DOCS -> {
                        metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_GENERIC)
                        metadata.putString(MediaMetadata.KEY_TITLE, mediaFileInfo.title)
                        metadata.putString(MediaMetadata.KEY_SUBTITLE, mediaFileInfo.path)

                        mediaInfo = MediaInfo.Builder(uri.toString())
                            .setStreamType(MediaInfo.STREAM_TYPE_NONE)
                            .setMetadata(metadata)
                            .setContentType(MimeTypes.BASE_TYPE_APPLICATION)
                            .build()
                    }
                }

                remoteMediaClient.load(
                    MediaLoadRequestData
                        .Builder().setMediaInfo(mediaInfo).setAutoplay(true).build()
                )
            }
        }
        /*remoteMediaClient.registerCallback(object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                val intent = Intent(
                    this@MainActivity,
                    ExpandedControlsActivity::class.java
                )
                startActivity(intent)
                remoteMediaClient.unregisterCallback(this)
            }
        })*/
//        val mediaItem = MediaQueueItem.Builder(mediaInfo).build()
//        remoteMediaClient.queueLoad(arrayOf(mediaItem), 0, 1, JSONObject())
//        remoteMediaClient.play()

        /*if (castPlayer == null) {
            castPlayer = CastPlayer(mCastContext!!)
            castPlayer?.setSessionAvailabilityListener(this)
        }
        val metadata = com.google.android.exoplayer2.MediaMetadata.Builder()
            .setTitle("Title").setSubtitle("Subtitle").build()
//        metadata.addImage(WebImage(Uri.parse("any-image-url")))

        val mediaInfo = MediaInfo.Builder(CloudStreamer.URL +
                Uri.fromFile(File(mediaFileInfo.path)).encodedPath)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(MimeTypes.VIDEO_MP4)
//            .setMetadata(metadata)
            .build()
        val mediaItem = MediaItem.Builder().setMimeType(MimeTypes.VIDEO_MP4)
            .setUri(uri).setMediaMetadata(metadata).build()
//        val mediaItem = MediaQueueItem.Builder(mediaInfo).build()
        castPlayer?.setMediaItem(mediaItem)
        castPlayer?.prepare()
        castPlayer?.playWhenReady = true
        castPlayer?.play()*/
    }

    private fun clearCastResources() {
        cloudStreamer?.stop()
        cloudStreamer = null
    }
}
