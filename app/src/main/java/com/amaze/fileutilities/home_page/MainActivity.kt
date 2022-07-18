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
import android.content.Intent
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
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.amaze.fileutilities.R
import com.amaze.fileutilities.WifiP2PActivity
import com.amaze.fileutilities.databinding.ActivityMainActionbarBinding
import com.amaze.fileutilities.databinding.ActivityMainActionbarItemSelectedBinding
import com.amaze.fileutilities.databinding.ActivityMainActionbarSearchBinding
import com.amaze.fileutilities.databinding.ActivityMainBinding
import com.amaze.fileutilities.home_page.database.Trial
import com.amaze.fileutilities.home_page.ui.AggregatedMediaFileInfoObserver
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.SearchListFragment
import com.amaze.fileutilities.home_page.ui.files.TrialValidationApi
import com.amaze.fileutilities.home_page.ui.options.Billing
import com.amaze.fileutilities.home_page.ui.settings.PreferenceActivity
import com.amaze.fileutilities.home_page.ui.transfer.TransferFragment
import com.amaze.fileutilities.utilis.ItemsActionBarFragment
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences
import com.amaze.fileutilities.utilis.hideFade
import com.amaze.fileutilities.utilis.hideTranslateY
import com.amaze.fileutilities.utilis.showFade
import com.amaze.fileutilities.utilis.showToastInCenter
import com.amaze.fileutilities.utilis.showToastOnBottom
import com.amaze.fileutilities.utilis.showTranslateY
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory
import com.stephentuso.welcome.WelcomeHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Random

class MainActivity :
    WifiP2PActivity(),
    AggregatedMediaFileInfoObserver {

    var log: Logger = LoggerFactory.getLogger(MainActivity::class.java)

    private lateinit var binding: ActivityMainBinding
    private lateinit var actionBarBinding: ActivityMainActionbarBinding
    private lateinit var searchActionBarBinding: ActivityMainActionbarSearchBinding
    private lateinit var selectedItemActionBarBinding: ActivityMainActionbarItemSelectedBinding
//    var showSearchFragment = false
    private lateinit var viewModel: FilesViewModel
    private var isOptionsVisible = false
    private var welcomeScreen: WelcomeHelper? = null
    private var didShowWelcomeScreen = true
    private var appUpdateManager: AppUpdateManager? = null

    companion object {
        private const val VOICE_REQUEST_CODE = 1000
        const val KEY_INTENT_AUDIO_PLAYER = "audio_player_intent"
        private const val DAYS_FOR_IMMEDIATE_UPDATE = Trial.TRIAL_DEFAULT_DAYS
        private const val UPDATE_REQUEST_CODE = 123234
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this).get(FilesViewModel::class.java)
        setTheme(R.style.Theme_AmazeFileUtilities)
        super.onCreate(savedInstanceState)
        welcomeScreen = WelcomeHelper(this, WelcomeScreen::class.java)
        if (!welcomeScreen!!.show(savedInstanceState)) {
            didShowWelcomeScreen = false
            invokePermissionCheck()
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
//        viewModel.copyTrainedData()
//        viewModel.getAndSaveUniqueDeviceId()
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
                    actionBarBinding.title.text = resources.getString(R.string.title_utilities)
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
        binding.optionsOverlay.setOnClickListener {
            isOptionsVisible = !isOptionsVisible
            invalidateOptionsTabs()
        }
        binding.aboutText.setOnClickListener {
            showAboutActivity(false, false, false)
        }
        binding.settingsText.setOnClickListener {
            val intent = Intent(this, PreferenceActivity::class.java)
            intent.putExtra(PreferenceActivity.KEY_IS_SETTINGS, true)
            startActivity(intent)
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
                                val mediaFileInfoList = ArrayList(this)
                                viewModel.analyseImageFeatures(
                                    mediaFileInfoList,
                                    pathPreferences
                                )
                                viewModel.analyseMemeImages(
                                    mediaFileInfoList,
                                    pathPreferences
                                )
                                viewModel.analyseBlurImages(
                                    mediaFileInfoList,
                                    pathPreferences
                                )
                                viewModel.analyseLowLightImages(
                                    mediaFileInfoList,
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

        if (!didShowWelcomeScreen) {
            checkForAppUpdates()
            viewModel.getUniqueId().observe(this) {
                deviceId ->
                if (deviceId != null) {
                    viewModel.checkInternetConnection(30000).observe(this) {
                        viewModel.validateTrial(deviceId, it) {
                            trialResponse ->
                            handleValidateTrial(trialResponse)
                        }
                    }
                }
            }
            shouldRateApp()
        } else {
            // add install time in preferences
            getAppCommonSharedPreferences()
                .edit().putLong(PreferencesConstants.KEY_INSTALL_DATE, Date().time)
                .apply()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        welcomeScreen?.onSaveInstanceState(outState)
    }

    /*override fun onResume() {
        super.onResume()

        // observe for content changes
        applicationContext
            .contentResolver
            .registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false,
                imagesObserver
            )
        applicationContext
            .contentResolver
            .registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, false,
                audiosObserver
            )
        applicationContext
            .contentResolver
            .registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, false,
                videosObserver
            )
        applicationContext
            .contentResolver
            .registerContentObserver(
                MediaStore.Files.getContentUri("external"), false,
                documentsObserver
            )
    }

    override fun onPause() {
        super.onPause()

        // observe for content changes
        applicationContext
            .contentResolver
            .unregisterContentObserver(imagesObserver)
        applicationContext
            .contentResolver
            .unregisterContentObserver(audiosObserver)
        applicationContext
            .contentResolver
            .unregisterContentObserver(videosObserver)
        applicationContext
            .contentResolver
            .unregisterContentObserver(documentsObserver)
    }*/

    override fun onPause() {
        super.onPause()
        appUpdateManager?.unregisterListener(updateListener)
    }

    override fun getTransferFragment(): TransferFragment? {
        val fragment = getFragmentAtFrame()
        return if (fragment is NavHostFragment) {
            if (fragment.childFragmentManager.fragments.size == 1) {
                val childFragment = fragment.childFragmentManager.fragments[0]
                if (childFragment is TransferFragment) {
                    return childFragment
                }
            }
            null
        } else {
            null
        }
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
        } else if (requestCode == UPDATE_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    log.info("user accepted the update")
                    //  handle user's approval
                    showToastOnBottom(getString(R.string.app_updated))
                }
                RESULT_CANCELED -> {
                    //  handle user's rejection
                    log.info("user rejected the update")
                    showToastOnBottom(getString(R.string.update_cancelled))
                }
                RESULT_IN_APP_UPDATE_FAILED -> {
                    // if you want to request the update again just call checkUpdate()
                    //  handle update failure
                    log.info("failed to update the app")
                    showToastOnBottom(getString(R.string.failed_to_update_app))
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        val fragment = getFragmentAtFrame()
        if (selectedItemActionBarBinding.root.isVisible &&
            !searchActionBarBinding.root.isVisible
        ) {
            if (!actionBarBinding.root.isVisible) {
                var abstractListFragment: ItemsActionBarFragment? = null
                fragment?.childFragmentManager?.fragments?.forEach {
                    if (it is ItemsActionBarFragment) {
                        abstractListFragment = it
                    }
                }
                if (abstractListFragment != null) {
                    invalidateSelectedActionBar(
                        false, abstractListFragment!!.hideActionBarOnClick(),
                        abstractListFragment!!.handleBackPressed()
                    )
                    abstractListFragment!!.handleBackPressed().invoke()
                } else {
                    super.onBackPressed()
                }
            } else {
                var didShowTransfer = false
                var isTransferFragment = false
                fragment?.childFragmentManager?.fragments?.forEach {
                    if (it is TransferFragment) {
                        isTransferFragment = true
                        // check if transfer in progress, avoid back press if it is
                        if (it.getTransferViewModel().isTransferInProgress) {
                            didShowTransfer = true
                            it.warnTransferInProgress {
                                super.onBackPressed()
                            }
                        }
                    }
                }
                if (!didShowTransfer || !isTransferFragment) {
                    super.onBackPressed()
                }
            }
        } else if (searchActionBarBinding.root.isVisible) {
            val searchFragment = supportFragmentManager
                .findFragmentByTag(SearchListFragment.FRAGMENT_TAG)
            val transaction = supportFragmentManager.beginTransaction()
            searchFragment?.let {
                transaction.remove(searchFragment)
                transaction.commit()
            }
        } else {
            super.onBackPressed()
        }
        if (isOptionsVisible) {
            isOptionsVisible = !isOptionsVisible
            invalidateOptionsTabs()
        }
    }

    /*private val imagesObserver = UriObserver(Handler()) {
        showToastInCenter("changes in images")
    }
    private val videosObserver = UriObserver(Handler()) {
        showToastInCenter("changes in videos")
    }
    private val audiosObserver = UriObserver(Handler()) {
        showToastInCenter("changes in audios")
    }
    private val documentsObserver = UriObserver(Handler()) {
        showToastInCenter("changes in documents")
    }*/

    private fun shouldRateApp() {
        val alreadyRated = getAppCommonSharedPreferences()
            .getBoolean(PreferencesConstants.KEY_RATE_APP_AUTOMATED, false)
        if (!alreadyRated) {
            val random = Random()
            val chance = random.nextInt(10) + 1
            if (chance == 5) {
                // check if user using app for over 7 days
                val calWeek = GregorianCalendar.getInstance()
                val calWeekDefault = GregorianCalendar.getInstance()
                calWeekDefault.time = Date()
                calWeekDefault.add(Calendar.DAY_OF_YEAR, 8)
                val installDate = getAppCommonSharedPreferences()
                    .getLong(PreferencesConstants.KEY_INSTALL_DATE, calWeekDefault.time.time)
                calWeek.time = Date(installDate)
                calWeek.add(Calendar.DAY_OF_YEAR, 7)
                if (calWeek.time.before(Date())) {
                    val manager = ReviewManagerFactory.create(this)
                    val request = manager.requestReviewFlow()
                    request.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // We got the ReviewInfo object
                            val reviewInfo = task.result
                            val flow = manager.launchReviewFlow(this, reviewInfo)
                            flow.addOnCompleteListener { _ ->
                                // The flow has finished. The API does not indicate whether the user
                                // reviewed or not, or even whether the review dialog was shown. Thus, no
                                // matter the result, we continue our app flow.
                                // add install time in preferences
                                getAppCommonSharedPreferences()
                                    .edit()
                                    .putBoolean(
                                        PreferencesConstants.KEY_RATE_APP_AUTOMATED,
                                        true
                                    ).apply()
                            }
                        } else {
                            // There was some problem, log or handle the error code.
                            log.warn("failed to request review", task.exception)
                        }
                    }
                }
            }
        }
    }

    private fun handleValidateTrial(trialResponse: TrialValidationApi.TrialResponse) {
        this@MainActivity.runOnUiThread {
            if (trialResponse.subscriptionStatus
                == Trial.SUBSCRIPTION_STATUS_DEFAULT
            ) {
                log.debug("user not subscribed {}", trialResponse)
                if (trialResponse.isNotConnected) {
                    val notConnectedCount = getAppCommonSharedPreferences()
                        .getInt(PreferencesConstants.KEY_NOT_CONNECTED_TRIAL_COUNT, 0)
                    getAppCommonSharedPreferences().edit()
                        .putInt(
                            PreferencesConstants.KEY_NOT_CONNECTED_TRIAL_COUNT,
                            notConnectedCount + 1
                        ).apply()
                    if (notConnectedCount > PreferencesConstants.VAL_THRES_NOT_CONNECTED_TRIAL) {
                        showAboutActivity(false, false, true)
                    }
                    log.warn("internet not connected count $notConnectedCount")
                } else {
                    getAppCommonSharedPreferences().edit()
                        .putInt(PreferencesConstants.KEY_NOT_CONNECTED_TRIAL_COUNT, 0).apply()
                    getAppCommonSharedPreferences().edit()
                        .putInt(PreferencesConstants.KEY_NOT_CONNECTED_SUBSCRIBED_COUNT, 0).apply()
                    when (trialResponse.getTrialStatusCode()) {
                        TrialValidationApi.TrialResponse.TRIAL_ACTIVE -> {
                            // check if it's first day or last day
                            if (trialResponse.isNewSignup) {
                                Utils.buildTrialStartedDialog(
                                    this,
                                    trialResponse.trialDaysLeft
                                ).create().show()
                            } else if (trialResponse.isLastDay) {
                                Utils.buildLastTrialDayDialog(this) {
                                    // wants to subscribe
                                    Billing.getInstance(
                                        this
                                    )?.initiatePurchaseFlow()
                                }.create().show()
                            }
                        }
                        TrialValidationApi.TrialResponse.TRIAL_EXPIRED -> {
                            showAboutActivity(true, false, false)
                        }
                        TrialValidationApi.TrialResponse.TRIAL_INACTIVE,
                        TrialValidationApi.TrialResponse.TRIAL_EXCLUSIVE -> {
                            showAboutActivity(false, true, false)
                        }
                    }
                }
            } else {
                log.debug("user subscribed {}", trialResponse)
                if (trialResponse.isNotConnected) {
                    val notConnectedCount = getAppCommonSharedPreferences()
                        .getInt(PreferencesConstants.KEY_NOT_CONNECTED_SUBSCRIBED_COUNT, 0)
                    getAppCommonSharedPreferences().edit()
                        .putInt(
                            PreferencesConstants.KEY_NOT_CONNECTED_SUBSCRIBED_COUNT,
                            notConnectedCount + 1
                        ).apply()
                    if (notConnectedCount > PreferencesConstants
                        .VAL_THRES_NOT_CONNECTED_SUBSCRIBED
                    ) {
                        showAboutActivity(false, false, true)
                    }
                    log.warn("subscribed and internet not connected count $notConnectedCount")
                } else {
                    getAppCommonSharedPreferences().edit()
                        .putInt(PreferencesConstants.KEY_NOT_CONNECTED_TRIAL_COUNT, 0).apply()
                    getAppCommonSharedPreferences().edit()
                        .putInt(PreferencesConstants.KEY_NOT_CONNECTED_SUBSCRIBED_COUNT, 0).apply()
                }
            }
        }
    }

    private fun checkForAppUpdates() {

        log.info("Checking for app update")
        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager?.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask?.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                // This example applies an immediate update. To apply a flexible update
                // instead, pass in AppUpdateType.FLEXIBLE
            ) {
                /**
                 * check for app updates - flexible update dialog is showing till 7 days for any
                 * app update which has priority less than 4 after which he is shown
                 * immediate update dialog, for priority 4 and 5 user is asked to update
                 * immediately
                 */
                log.info("App update available")
                /*val immediateUpdate = (
                    appUpdateInfo.clientVersionStalenessDays()
                        ?: -1
                    ) >= DAYS_FOR_IMMEDIATE_UPDATE || appUpdateInfo.updatePriority() >= 4
                log.info("Immediate criteria $immediateUpdate")*/
                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) &&
                    appUpdateInfo.updatePriority() >= 4
                ) {
                    log.info("Immediate update conditions met, triggering immediate update")
                    appUpdateManager?.startUpdateFlowForResult(
                        // Pass the intent that is returned by 'getAppUpdateInfo()'.
                        appUpdateInfo,
                        // The current activity making the update request.
                        this,
                        // Or pass 'AppUpdateType.FLEXIBLE' to newBuilder() for
                        // flexible updates.
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                            .setAllowAssetPackDeletion(true)
                            .build(),
                        // Include a request code to later monitor this update request.
                        UPDATE_REQUEST_CODE
                    )
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) &&
                    appUpdateInfo.updatePriority() >= 2
                ) {
                    log.info("flexible update conditions met, triggering flexible update")
                    appUpdateManager?.startUpdateFlowForResult(
                        // Pass the intent that is returned by 'getAppUpdateInfo()'.
                        appUpdateInfo,
                        // The current activity making the update request.
                        this,
                        // Or pass 'AppUpdateType.FLEXIBLE' to newBuilder() for
                        // flexible updates.
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE)
                            .setAllowAssetPackDeletion(true)
                            .build(),
                        // Include a request code to later monitor this update request.
                        UPDATE_REQUEST_CODE
                    )
                }
            } else if (appUpdateInfo.updateAvailability()
                == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            ) {
                // Checks that the update is not stalled during 'onResume()'.
                // However, you should execute this check at all entry points into the app.
                log.info("resuming update that was already in progress")
                appUpdateManager?.startUpdateFlowForResult(
                    // Pass the intent that is returned by 'getAppUpdateInfo()'.
                    appUpdateInfo,
                    // The current activity making the update request.
                    this,
                    // Or pass 'AppUpdateType.FLEXIBLE' to newBuilder() for
                    // flexible updates.
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                        .setAllowAssetPackDeletion(true)
                        .build(),
                    // Include a request code to later monitor this update request.
                    UPDATE_REQUEST_CODE
                )
            }

            appUpdateManager?.registerListener(updateListener)

        /*val cal1 = GregorianCalendar.getInstance()
        cal1.time = Date()
        cal1.add(Calendar.DAY_OF_YEAR, -2)
        val fetchTime = applicationContext.getAppCommonSharedPreferences()
            .getLong(PreferencesConstants.KEY_UPDATE_APP_LAST_SHOWN_DATE, cal1.timeInMillis)

        // check for update only once a day
        val cal = GregorianCalendar.getInstance()
        cal.time = Date(fetchTime)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        if (cal.time.before(Date()) || true) {


                /*if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    // If an in-app update is already running, resume the update.
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        this,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                            .setAllowAssetPackDeletion(true)
                            .build(),
                        UPDATE_REQUEST_CODE)
                }*/

            applicationContext.getAppCommonSharedPreferences()
                .edit().putLong(
                    PreferencesConstants.KEY_UPDATE_APP_LAST_SHOWN_DATE,
                    Date().time
                ).apply()
        }*/
        }
    }

    private val updateListener: InstallStateUpdatedListener =
        InstallStateUpdatedListener { installState ->
            if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                // After the update is downloaded, show a notification
                // and request user confirmation to restart the app.
                log.info("update has been downloaded")
                showToastOnBottom(getString(R.string.updating_app))
                appUpdateManager!!.completeUpdate()
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

    fun invalidateSelectedActionBar(
        doShow: Boolean,
        hideActionBarOnClick: Boolean,
        onBackPressed: () -> Unit
    ): View? {
        if (::selectedItemActionBarBinding.isInitialized) {
            selectedItemActionBarBinding.run {
                return if (doShow) {
                    actionBarBinding.root.hideFade(200)
                    selectedItemActionBarBinding.root.showFade(300)
                    supportActionBar?.customView = root
                    backActionBar.setOnClickListener {
                        onBackPressed.invoke()
                        if (hideActionBarOnClick) {
                            invalidateSelectedActionBar(
                                false, hideActionBarOnClick,
                                onBackPressed
                            )
                        } else {
                            onBackPressed()
                        }
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

    private fun showAboutActivity(
        isTrialExpired: Boolean,
        isTrialInactive: Boolean,
        isNotConnected: Boolean
    ) {
        val intent = Intent(this, PreferenceActivity::class.java)
        intent.putExtra(PreferenceActivity.KEY_IS_SETTINGS, false)
        intent.putExtra(PreferenceActivity.KEY_IS_TRIAL_EXPIRED, isTrialExpired)
        intent.putExtra(PreferenceActivity.KEY_IS_TRIAL_INACTIVE, isTrialInactive)
        intent.putExtra(PreferenceActivity.KEY_NOT_CONNECTED, isNotConnected)
        startActivity(intent)
        isOptionsVisible = !isOptionsVisible
        invalidateOptionsTabs()
    }

    private fun showSearchFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(
            R.id.nav_host_fragment_activity_main, SearchListFragment(),
            SearchListFragment.FRAGMENT_TAG
        )
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
            log.warn("voice recognition activity not found", e)
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
}
