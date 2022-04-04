/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.fragment.app.Fragment
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.ActivityPreferencesBinding
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.home_page.ui.options.AboutFragment
import java.util.*

class PreferenceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPreferencesBinding
    private var titleStack: Stack<String> = Stack()

    companion object {
        const val KEY_IS_SETTINGS = "key_settings"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent.extras?.let {
            extras ->
            if (extras.getBoolean(KEY_IS_SETTINGS)) {
                inflatePreferenceFragment(PreferenceFragment(), R.string.settings)
                titleStack.push(resources.getString(R.string.settings))
            } else {
                inflatePreferenceFragment(AboutFragment(), R.string.about)
                titleStack.push(resources.getString(R.string.about))
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onBackPressed() {
        val fragment = getFragmentAtFrame()
        if (fragment is PreferenceFragment || fragment is AboutFragment) {
            val intent = Intent(this, MainActivity::class.java)
            NavUtils.navigateUpTo(this, intent)
        } else {
            super.onBackPressed()
            title = titleStack.pop()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Required parent fragment manager if calling from any nested fragment,
     * else support fragment manager from activity
     */
    fun inflatePreferenceFragment(fragment: Fragment, screenTitle: Int) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.prefs_fragment, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
        title?.let {
            titleStack.push(it.toString())
        }
        title = resources.getString(screenTitle)
    }

    private fun getFragmentAtFrame(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.prefs_fragment)
    }
}
