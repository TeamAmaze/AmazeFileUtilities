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

package com.amaze.fileutilities.home_page.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Date

@RunWith(AndroidJUnit4::class)
class AppStorageStatsDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var appStorageStatsDao: AppStorageStatsDao
    private lateinit var installedAppsDao: InstalledAppsDao
    private lateinit var appEntry: InstalledApps

    private val appName = "testApp"

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        appStorageStatsDao = db.appStorageStatsDao()
        installedAppsDao = db.installedAppsDao()
        installedAppsDao.insert(InstalledApps(appName, listOf()))
        val entry = installedAppsDao.findByPackageName(appName)
        Assert.assertNotNull(entry)
        appEntry = entry!!
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun insertTest() {
        val date = Date()
        val size = 1234L

        appStorageStatsDao.insert(appName, date, size)
        val allAppStorageStats = appStorageStatsDao.findAll()
        Assert.assertNotNull(
            "Did not contain correct entry with packageId ${appEntry.uid}, timestamp $date " +
                "and packageSize $size: $allAppStorageStats",
            allAppStorageStats.find {
                it.packageSize == size && it.timestamp == date && it.packageId == appEntry.uid
            }
        )
    }

    @Test
    @Throws(IOException::class)
    fun deleteOlderThanDateTest() {
        val minDate = Date(1000)
        val size = 1234L

        val earlierDate = Date(100)
        val laterDate = Date(5000)
        appStorageStatsDao.insert(appName, earlierDate, size)
        appStorageStatsDao.insert(appName, laterDate, size)

        appStorageStatsDao.deleteOlderThan(minDate)
        val allStorageStats = appStorageStatsDao.findAll()
        Assert.assertEquals(
            "Database should only contain one AppStorageStats entry " +
                "but was $allStorageStats",
            1,
            allStorageStats.size
        )
        Assert.assertNotNull(
            "Database did not contain expected entry with timestamp $laterDate " +
                "but was $allStorageStats",
            allStorageStats.find { it.timestamp == laterDate && appEntry.uid == it.packageId }
        )
    }
}
