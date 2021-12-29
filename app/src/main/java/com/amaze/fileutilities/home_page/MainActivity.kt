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

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.AudioPlayerDialogActivityViewModel
import com.amaze.fileutilities.databinding.ActivityMainActionbarBinding
import com.amaze.fileutilities.databinding.ActivityMainActionbarSearchBinding
import com.amaze.fileutilities.databinding.ActivityMainBinding
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.SearchListFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.EditText
import com.amaze.fileutilities.utilis.showToastOnTop


class MainActivity : PermissionActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var actionBarBinding: ActivityMainActionbarBinding
    private lateinit var searchActionBarBinding: ActivityMainActionbarSearchBinding
//    var showSearchFragment = false
    private lateinit var viewModel: FilesViewModel
    private var hasImageResults = false
    private var hasVideoResults = false
    private var hasAudioResults = false
    private var hasDocResults = false

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

        viewModel.usedImagesSummaryTransformations.observe(this, {
            hasImageResults = it!=null
        })
        viewModel.usedVideosSummaryTransformations.observe(this, {
            hasVideoResults = it!=null
        })
        viewModel.usedAudiosSummaryTransformations.observe(this, {
            hasAudioResults = it!=null
        })
        viewModel.usedDocsSummaryTransformations.observe(this, {
            hasDocResults = it!=null
        })

        actionBarBinding.searchActionBar.setOnClickListener {
            if (hasImageResults && hasAudioResults && hasVideoResults && hasDocResults) {
                showSearchFragment()
            } else {
                this.showToastOnTop(resources.getString(R.string.please_wait))
            }
        }
    }

    fun setCustomTitle(title: String) {
        if (::actionBarBinding.isInitialized) {
            actionBarBinding.title.text = title
        }
    }

    fun invalidateSearchBar(showSearch: Boolean): AutoCompleteTextView? {
        if (showSearch) {
            supportActionBar?.customView = searchActionBarBinding.root
            searchActionBarBinding.backActionBar.setOnClickListener {
                onBackPressed()
            }
            return searchActionBarBinding.actionBarEditText
        } else {
            supportActionBar?.customView = actionBarBinding.root
            return null
        }
    }

    private fun showSearchFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.nav_host_fragment_activity_main, SearchListFragment())
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
