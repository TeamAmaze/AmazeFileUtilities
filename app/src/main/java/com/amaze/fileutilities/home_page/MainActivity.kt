/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.MotionEvent
import android.view.View
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.ActionBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.mediarouter.media.MediaRouter
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.amaze.fileutilities.PermissionsActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.cast.ExpandedControlsActivity
import com.amaze.fileutilities.databinding.ActivityMainActionbarBinding
import com.amaze.fileutilities.databinding.ActivityMainActionbarItemSelectedBinding
import com.amaze.fileutilities.databinding.ActivityMainActionbarSearchBinding
import com.amaze.fileutilities.databinding.ActivityMainBinding
import com.amaze.fileutilities.home_page.ui.AggregatedMediaFileInfoObserver
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.home_page.ui.files.SearchListFragment
import com.amaze.fileutilities.home_page.ui.options.AboutActivity
import com.amaze.fileutilities.home_page.ui.settings.PreferenceActivity
import com.amaze.fileutilities.utilis.*
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.*
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity :
    PermissionsActivity(),
    AggregatedMediaFileInfoObserver,
    SessionManagerListener<CastSession> {

    private lateinit var binding: ActivityMainBinding
    private lateinit var actionBarBinding: ActivityMainActionbarBinding
    private lateinit var searchActionBarBinding: ActivityMainActionbarSearchBinding
    private lateinit var selectedItemActionBarBinding: ActivityMainActionbarItemSelectedBinding
//    var showSearchFragment = false
    private lateinit var viewModel: FilesViewModel
    private var isOptionsVisible = false
    private var mCastContext: CastContext? = null
    private var mCastSession: CastSession? = null
    private lateinit var mSessionManager: SessionManager
    val manager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }

    var channel: WifiP2pManager.Channel? = null

    companion object {
        private const val VOICE_REQUEST_CODE = 1000
        const val KEY_INTENT_AUDIO_PLAYER = "audio_player_intent"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this).get(FilesViewModel::class.java)
        channel = manager?.initialize(this, mainLooper, null)
//        viewModel.copyTrainedData()
        actionBarBinding = ActivityMainActionbarBinding.inflate(layoutInflater)
        searchActionBarBinding = ActivityMainActionbarSearchBinding.inflate(layoutInflater)
        selectedItemActionBarBinding = ActivityMainActionbarItemSelectedBinding
            .inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_analyse, R.id.navigation_files, R.id.navigation_transfer
            )
        )
//        setupActionBarWithNavController(navController, appBarConfiguration)
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.customView = actionBarBinding.root
        supportActionBar?.elevation = 0f
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            if (isOptionsVisible) {
                isOptionsVisible = !isOptionsVisible
                invalidateOptionsTabs()
            }
            when (destination.id) {
                R.id.navigation_analyse ->
                    actionBarBinding.title.text = resources.getString(R.string.title_analyse)
                R.id.navigation_files ->
                    actionBarBinding.title.text = resources.getString(R.string.title_files)
                R.id.navigation_transfer ->
                    actionBarBinding.title.text = resources.getString(R.string.title_transfer)
            }
        }
        navView.setupWithNavController(navController)

        actionBarBinding.searchActionBar.setOnClickListener {
            showSearchFragment()
        }
        actionBarBinding.optionsImage.setOnClickListener {
            isOptionsVisible = !isOptionsVisible
            invalidateOptionsTabs()
        }
        mCastContext = CastContext.getSharedInstance(baseContext)
        mSessionManager = mCastContext!!.sessionManager
        val mediaRouter = MediaRouter.getInstance(this)
        mediaRouter.setMediaSession(mSessionManager.currentCastSession)
        CastButtonFactory.setUpMediaRouteButton(
            baseContext,
            binding.mediaRouteButton
        )
        binding.optionsOverlay.setOnClickListener {
            isOptionsVisible = !isOptionsVisible
            invalidateOptionsTabs()
        }
        binding.aboutText.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
            isOptionsVisible = !isOptionsVisible
            invalidateOptionsTabs()
        }
        binding.settingsText.setOnClickListener {
            startActivity(Intent(this, PreferenceActivity::class.java))
            isOptionsVisible = !isOptionsVisible
            invalidateOptionsTabs()
        }

        viewModel.initAndFetchPathPreferences().observe(this) { pathPreferences ->
            viewModel.usedImagesSummaryTransformations.observe(
                this
            ) {
                mediaInfoStorageSummaryPair ->
                viewModel.initAnalysisMigrations.observe(this) {
                    if (it) {
                        mediaInfoStorageSummaryPair?.second.let { list ->
                            list?.run {
                                viewModel.analyseImageFeatures(
                                    this,
                                    pathPreferences
                                )
                                viewModel.analyseMemeImages(
                                    this,
                                    pathPreferences
                                )
                                viewModel.analyseBlurImages(
                                    this,
                                    pathPreferences
                                )
                                viewModel.analyseLowLightImages(
                                    this,
                                    pathPreferences
                                )
                            }
                        }
                    }
                }
            }

            val prefs = this.getAppCommonSharedPreferences()
            val searchIdx = prefs.getInt(
                PreferencesConstants.KEY_SEARCH_DUPLICATES_IN,
                PreferencesConstants.DEFAULT_SEARCH_DUPLICATES_IN
            )

            if (searchIdx != PreferencesConstants.DEFAULT_SEARCH_DUPLICATES_IN) {
                viewModel.analyseInternalStorage(
                    searchIdx ==
                        PreferencesConstants.VAL_SEARCH_DUPLICATES_INTERNAL_DEEP
                )
            } else {
                observeMediaInfoLists { isLoading, aggregatedFiles ->
                    if (!isLoading && aggregatedFiles != null) {
                        viewModel.analyseMediaStoreFiles(aggregatedFiles)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mSessionManager = CastContext.getSharedInstance(applicationContext).sessionManager
        mCastSession = mSessionManager.currentCastSession
        mSessionManager.addSessionManagerListener(this, CastSession::class.java)
    }

    override fun onPause() {
        super.onPause()
        mSessionManager.removeSessionManagerListener(this, CastSession::class.java)
        mCastSession = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK) {
            // Populate the wordsList with the String values the recognition engine thought it heard
            val matches: ArrayList<String>? = data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )
            matches?.let {
                if (matches.size > 0) {
                    searchActionBarBinding.actionBarEditText.setText(matches[0])
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        val fragment = getFragmentAtFrame()
        if (fragment is NavHostFragment && fragment.childFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
            if (isOptionsVisible) {
                isOptionsVisible = !isOptionsVisible
                invalidateOptionsTabs()
            }
        }
    }

    private fun getFragmentAtFrame(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
    }

    fun setCustomTitle(title: String) {
        if (::actionBarBinding.isInitialized) {
            actionBarBinding.title.text = title
        }
    }

    fun getWifiP2PManager(): WifiP2pManager? {
        return manager
    }

    fun getWifiP2PChannel(): WifiP2pManager.Channel? {
        return channel
    }

    fun invalidateSearchBar(showSearch: Boolean): AutoCompleteTextView? {
        if (::searchActionBarBinding.isInitialized) {
            searchActionBarBinding.run {
                return if (showSearch) {
                    actionBarBinding.root.hideFade(200)
                    searchActionBarBinding.root.showFade(300)
                    supportActionBar?.customView = root
                    backActionBar.setOnClickListener {
                        onBackPressed()
                    }
                    actionBarEditText.setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            if (event.rawX >= (
                                actionBarEditText.right -
                                    actionBarEditText.compoundDrawables[2]
                                        .bounds.width()
                                )
                            ) {
                                actionBarEditText.setText("")
                                true
                            }
                        }
                        false
                    }
                    voiceActionBar.setOnClickListener {
                        startVoiceRecognitionActivity()
                    }
                    actionBarEditText
                } else {
                    actionBarEditText.setText("")
                    actionBarEditText.setAdapter(null)
                    searchActionBarBinding.root.hideFade(200)
                    actionBarBinding.root.showFade(300)
                    supportActionBar?.customView = actionBarBinding.root
                    actionBarEditText.setOnEditorActionListener(null)
                    null
                }
            }
        }
        return null
    }

    fun invalidateSelectedActionBar(doShow: Boolean): View? {
        if (::selectedItemActionBarBinding.isInitialized) {
            selectedItemActionBarBinding.run {
                return if (doShow) {
                    actionBarBinding.root.hideFade(200)
                    selectedItemActionBarBinding.root.showFade(300)
                    supportActionBar?.customView = root
                    backActionBar.setOnClickListener {
                        onBackPressed()
                    }
                    title.setText("0")
                    selectedItemActionBarBinding.root
                } else {
                    title.setText("0")
                    searchActionBarBinding.root.hideFade(200)
                    actionBarBinding.root.showFade(300)
                    supportActionBar?.customView = actionBarBinding.root
                    null
                }
            }
        }
        return null
    }

    fun invalidateBottomBar(doShow: Boolean) {
        if (::binding.isInitialized) {
            if (doShow) {
//            binding.navView.visibility = View.VISIBLE
                binding.navView.showTranslateY(500)
            } else {
//            binding.navView.visibility = View.GONE
                binding.navView.hideTranslateY(500)
            }
        }
    }

    private fun showSearchFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.nav_host_fragment_activity_main, SearchListFragment())
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun startVoiceRecognitionActivity() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Voice recognition Demo...")
        try {
            startActivityForResult(intent, VOICE_REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            this.showToastInCenter(resources.getString(R.string.unsupported_operation))
        }
    }

    private fun invalidateOptionsTabs() {
        binding.run {
            if (!isOptionsVisible) {
                optionsOverlay.hideFade(400)
                optionsRoot.hideFade(200)
                optionsRoot.visibility = View.GONE
            } else {
                optionsOverlay.showFade(500)
                optionsRoot.showFade(200)
                optionsRoot.visibility = View.VISIBLE
            }
        }
    }

    override fun getFilesModel(): FilesViewModel {
        return viewModel
    }

    override fun lifeCycleOwner(): LifecycleOwner {
        return this
    }

    private fun invalidateCastButton() {
        if (::binding.isInitialized && binding.root.isVisible) {
            binding.run {
                CastButtonFactory.setUpMediaRouteButton(applicationContext, mediaRouteButton)
            }
        }
    }

    fun startCastPlayback(mediaFileInfo: MediaFileInfo) {
        val remoteMediaClient = mCastSession?.remoteMediaClient ?: return
        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)

        movieMetadata.putString(MediaMetadata.KEY_TITLE, mediaFileInfo.title)
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, mediaFileInfo.path)
//        movieMetadata.addImage(WebImage(Uri.fromFile(mediaFileInfo.extraInfo?.audioMetaData?.)))
//        movieMetadata.addImage(WebImage(Uri.parse(mSelectedMedia.getImage(1))))

        val duration = mediaFileInfo.extraInfo?.audioMetaData?.duration ?: 0
        val mediaInfo = MediaInfo.Builder(mediaFileInfo.path)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("audio/mp3")
            .setMetadata(movieMetadata)
            .setStreamDuration(duration * 1000)
            .build()
        remoteMediaClient.registerCallback(object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                val intent = Intent(
                    this@MainActivity,
                    ExpandedControlsActivity::class.java
                )
                startActivity(intent)
                remoteMediaClient.unregisterCallback(this)
            }
        })
        remoteMediaClient.load(
            MediaLoadRequestData
                .Builder().setMediaInfo(mediaInfo).build()
        )
    }

    override fun onSessionEnded(p0: CastSession, p1: Int) {
        onApplicationDisconnected()
    }

    override fun onSessionResumed(p0: CastSession, p1: Boolean) {
        onApplicationConnected(p0)
    }

    override fun onSessionStarted(p0: CastSession, p1: String) {
        onApplicationConnected(p0)
    }

    override fun onSessionEnding(p0: CastSession) {
    }

    override fun onSessionResumeFailed(p0: CastSession, p1: Int) {
        onApplicationDisconnected()
    }

    override fun onSessionResuming(p0: CastSession, p1: String) {
        onApplicationConnected(p0)
    }

    override fun onSessionStartFailed(p0: CastSession, p1: Int) {
        onApplicationDisconnected()
    }

    override fun onSessionStarting(p0: CastSession) {
        onApplicationConnected(p0)
    }

    override fun onSessionSuspended(p0: CastSession, p1: Int) {
    }

    private fun onApplicationConnected(castSession: CastSession) {
        mCastSession = castSession
        /*if (null != mSelectedMedia) {
            if (mPlaybackState === PlaybackState.PLAYING) {
                mVideoView.pause()
                loadRemoteMedia(mSeekbar.getProgress(), true)
                return
            } else {
                mPlaybackState = PlaybackState.IDLE
                updatePlaybackLocation(PlaybackLocation.REMOTE)
            }
        }
        updatePlayButton(mPlaybackState)*/
//        supportInvalidateOptionsMenu()
    }

    private fun onApplicationDisconnected() {
        /*updatePlaybackLocation(PlaybackLocation.LOCAL)
        mPlaybackState = PlaybackState.IDLE
        mLocation = PlaybackLocation.LOCAL
        updatePlayButton(mPlaybackState)
        supportInvalidateOptionsMenu()*/
    }
}
