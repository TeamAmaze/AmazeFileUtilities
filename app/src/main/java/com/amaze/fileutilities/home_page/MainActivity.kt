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

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.MotionEvent
import android.view.View
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.ActionBar
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.ActivityMainActionbarBinding
import com.amaze.fileutilities.databinding.ActivityMainActionbarSearchBinding
import com.amaze.fileutilities.databinding.ActivityMainBinding
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.SearchListFragment
import com.amaze.fileutilities.utilis.hideFade
import com.amaze.fileutilities.utilis.hideTranslateY
import com.amaze.fileutilities.utilis.showFade
import com.amaze.fileutilities.utilis.showTranslateY
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : PermissionActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var actionBarBinding: ActivityMainActionbarBinding
    private lateinit var searchActionBarBinding: ActivityMainActionbarSearchBinding
//    var showSearchFragment = false
    private lateinit var viewModel: FilesViewModel
    private var isOptionsVisible = false

    companion object {
        private const val VOICE_REQUEST_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this).get(FilesViewModel::class.java)
        actionBarBinding = ActivityMainActionbarBinding.inflate(layoutInflater)
        searchActionBarBinding = ActivityMainActionbarSearchBinding.inflate(layoutInflater)
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
        binding.optionsOverlay.setOnClickListener {
            isOptionsVisible = !isOptionsVisible
            invalidateOptionsTabs()
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
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun setCustomTitle(title: String) {
        if (::actionBarBinding.isInitialized) {
            actionBarBinding.title.text = title
        }
    }

    fun invalidateSearchBar(showSearch: Boolean): AutoCompleteTextView? {
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

    fun invalidateBottomBar(doShow: Boolean) {
        if (doShow) {
//            binding.navView.visibility = View.VISIBLE
            binding.navView.showTranslateY(500)
        } else {
//            binding.navView.visibility = View.GONE
            binding.navView.hideTranslateY(500)
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
        startActivityForResult(intent, VOICE_REQUEST_CODE)
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
}
