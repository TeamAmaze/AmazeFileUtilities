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
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.ActivityMainActionbarBinding
import com.amaze.fileutilities.databinding.ActivityMainActionbarSearchBinding
import com.amaze.fileutilities.databinding.ActivityMainBinding
import com.amaze.fileutilities.home_page.ui.files.SearchListFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : PermissionActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var actionBarBinding: ActivityMainActionbarBinding
    private lateinit var searchActionBarBinding: ActivityMainActionbarSearchBinding
    var showSearchFragment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
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

        actionBarBinding.searchActionBar.setOnClickListener {
            if (showSearchFragment) {
                showSearchFragment()
            } else {
                Toast.makeText(this, R.string.please_wait, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun setCustomTitle(title: String) {
        actionBarBinding.title.text = title
    }

    fun invalidateSearchBar(showSearch: Boolean) {
        if (showSearch) {
            supportActionBar?.customView = searchActionBarBinding.root
        } else {
            supportActionBar?.customView = actionBarBinding.root
        }
    }

    fun showSearchFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.nav_host_fragment_activity_main, SearchListFragment())
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
