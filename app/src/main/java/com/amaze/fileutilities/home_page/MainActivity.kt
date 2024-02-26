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

package com.amaze.fileutilities.home_page

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.RecognizerIntent
import android.view.MotionEvent
import android.view.View
import android.widget.AutoCompleteTextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.amaze.fileutilities.BuildConfig
import com.amaze.fileutilities.R
import com.amaze.fileutilities.WifiP2PActivity
import com.amaze.fileutilities.databinding.ActivityMainActionbarBinding
import com.amaze.fileutilities.databinding.ActivityMainActionbarItemSelectedBinding
import com.amaze.fileutilities.databinding.ActivityMainActionbarSearchBinding
import com.amaze.fileutilities.databinding.ActivityMainBinding
import com.amaze.fileutilities.home_page.database.Trial
import com.amaze.fileutilities.home_page.ui.AggregatedMediaFileInfoObserver
import com.amaze.fileutilities.home_page.ui.files.FilesFragment
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.SearchListFragment
import com.amaze.fileutilities.home_page.ui.files.TrialValidationApi
import com.amaze.fileutilities.home_page.ui.options.Billing
import com.amaze.fileutilities.home_page.ui.settings.PreferenceActivity
import com.amaze.fileutilities.home_page.ui.transfer.TransferFragment
import com.amaze.fileutilities.utilis.ItemsActionBarFragment
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.UpdateChecker
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences
import com.amaze.fileutilities.utilis.hideFade
import com.amaze.fileutilities.utilis.hideTranslateY
import com.amaze.fileutilities.utilis.showFade
import com.amaze.fileutilities.utilis.showToastInCenter
import com.amaze.fileutilities.utilis.showToastOnBottom
import com.amaze.fileutilities.utilis.showTranslateY
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stephentuso.welcome.WelcomeHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Date

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
    // refers to com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED
    val RESULT_IN_APP_UPDATE_FAILED = 1

    companion object {
        private const val VOICE_REQUEST_CODE = 1000
        const val KEY_INTENT_AUDIO_PLAYER = "audio_player_intent"
        private const val DAYS_FOR_IMMEDIATE_UPDATE = Trial.TRIAL_DEFAULT_DAYS
        const val UPDATE_REQUEST_CODE = 123234
    }

    @RequiresApi(Build.VERSION_CODES.M)
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
        supportActionBar?.setBackgroundDrawable(
            ColorDrawable(
                resources
                    .getColor(R.color.navy_blue)
            )
        )
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
            viewModel.usedImagesSummaryTransformations().observe(
                this
            ) {
                mediaInfoStorageSummaryPair ->
                viewModel.initAnalysisMigrations.observe(this) {
                    if (it) {
                        mediaInfoStorageSummaryPair?.second.let { list ->
                            list?.run {
                                val mediaFileInfoList = ArrayList(this)
                                if (!BuildConfig.IS_VERSION_FDROID) {
                                    viewModel.analyseImageFeatures(
                                        mediaFileInfoList,
                                        pathPreferences
                                    )
                                    viewModel.analyseMemeImages(
                                        mediaFileInfoList,
                                        pathPreferences
                                    )
                                }
                                viewModel.analyseBlurImages(
                                    mediaFileInfoList,
                                    pathPreferences
                                )
                                viewModel.analyseLowLightImages(
                                    mediaFileInfoList,
                                    pathPreferences
                                )
                                viewModel.analyseSimilarImages(
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
            UpdateChecker.checkForAppUpdates(this)
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
            UpdateChecker.shouldRateApp(this)
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
        UpdateChecker.unregisterListener()
        getFilesModel().resetTrashBinConfig()
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

    @Deprecated("Deprecated in Java")
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
            if (fragment?.childFragmentManager?.fragments?.isNotEmpty() == true &&
                fragment.childFragmentManager
                    .fragments[fragment.childFragmentManager.fragments.size - 1] is FilesFragment
            ) {
                exit()
            } else {
                super.onBackPressed()
            }
        }
        if (isOptionsVisible) {
            isOptionsVisible = !isOptionsVisible
            invalidateOptionsTabs()
        }
    }

    private fun exit() {
        if (getFilesModel().backPressedToExitOnce) {
            finish()
        } else {
            getFilesModel().backPressedToExitOnce = true
            showToastInCenter(getString(R.string.press_again))
            object : CountDownTimer(2000, 2000) {
                override fun onTick(millisUntilFinished: Long) {
                    // do nothing
                }

                override fun onFinish() {
                    getFilesModel().backPressedToExitOnce = false
                }
            }.start()
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
                        TrialValidationApi.TrialResponse.TRIAL_EXPIRED,
                        TrialValidationApi.TrialResponse.TRIAL_UNOFFICIAL -> {
                            showAboutActivity(true, false, false)
                        }
                        TrialValidationApi.TrialResponse.TRIAL_INACTIVE -> {
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
                Utils.marqueeAfterDelay(2000, selectedItemActionBarBinding.title)
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
