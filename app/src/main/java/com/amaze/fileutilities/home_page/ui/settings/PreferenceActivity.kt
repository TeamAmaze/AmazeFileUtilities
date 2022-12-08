/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
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

package com.amaze.fileutilities.home_page.ui.settings

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.fragment.app.Fragment
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.ActivityPreferencesBinding
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.home_page.database.AppDatabase
import com.amaze.fileutilities.home_page.database.Trial
import com.amaze.fileutilities.home_page.ui.files.TrialValidationApi
import com.amaze.fileutilities.home_page.ui.options.AboutFragment
import com.amaze.fileutilities.home_page.ui.options.Billing
import com.amaze.fileutilities.utilis.Utils
import java.util.Stack

class PreferenceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPreferencesBinding
    private var titleStack: Stack<String> = Stack()

    companion object {
        const val KEY_IS_SETTINGS = "key_settings"
        const val KEY_IS_TRIAL_EXPIRED = "key_is_trial_expired"
        const val KEY_IS_TRIAL_INACTIVE = "key_is_trial_inactive"
        const val KEY_NOT_CONNECTED = "key_not_connected"
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
                if (extras.getBoolean(KEY_IS_TRIAL_EXPIRED)) {
                    Utils.buildTrialExpiredDialog(this) {
                        // subscribe
                        Billing.getInstance(this)?.initiatePurchaseFlow()
                    }.create().show()
                } else if (extras.getBoolean(KEY_IS_TRIAL_INACTIVE)) {
                    Utils.buildTrialExclusiveInactiveDialog(this) {
                        // subscribe
                        Billing.getInstance(this)?.initiatePurchaseFlow()
                    }.create().show()
                } else if (extras.getBoolean(KEY_NOT_CONNECTED)) {
                    Utils.buildNotConnectedTrialValidationDialog(this).create().show()
                }
                inflatePreferenceFragment(AboutFragment(), R.string.about)
                titleStack.push(resources.getString(R.string.about))
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setBackgroundDrawable(
            ColorDrawable(
                resources
                    .getColor(R.color.navy_blue)
            )
        )
    }

    override fun onBackPressed() {
        val fragment = getFragmentAtFrame()
        if (fragment is PreferenceFragment || fragment is AboutFragment) {
            // check if license expired...
            val dao = AppDatabase.getInstance(applicationContext).trialValidatorDao()
            var isActive = false
            var isSubscribed = false
            dao.getAll().forEach {
                if (it.trialStatus == TrialValidationApi.TrialResponse.TRIAL_ACTIVE ||
                    it.trialStatus == TrialValidationApi.TrialResponse.TRIAL_EXCLUSIVE
                ) {
                    isActive = true
                }
                if (it.subscriptionStatus != Trial.SUBSCRIPTION_STATUS_DEFAULT) {
                    isSubscribed = true
                }
            }
            if (!isActive && !isSubscribed) {
                finishAffinity()
            } else {
                val intent = Intent(this, MainActivity::class.java)
                NavUtils.navigateUpTo(this, intent)
            }
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
